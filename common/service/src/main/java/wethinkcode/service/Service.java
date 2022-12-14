package wethinkcode.service;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JsonMapper;
import io.javalin.plugin.bundled.CorsPluginConfig;
import picocli.CommandLine;
import wethinkcode.service.controllers.Controllers;
import wethinkcode.service.json.GSONMapper;
import wethinkcode.service.messages.*;

import javax.jms.JMSException;
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static wethinkcode.logger.Logger.formatted;
import static wethinkcode.service.Checks.*;
import static wethinkcode.service.messages.AlertService.*;
import static wethinkcode.service.properties.Properties.populateFields;

/**
 * The Service class is used to create a Javalin server instance and allow a user to create a Javalin server
 * by annotating a class with the `Service` annotation. The `execute` method sets up the Javalin server and creates
 * a new instance of the user's class with the `Service` annotation. It also initializes the properties of the user's
 * class by calling the `initProperties` method and initializes the Javalin server by calling the `initHttpServer` method.
 *
 * @param <E> The type parameter for the user's class with the `Service` annotation
 */
public class Service<E>{

    static {
        try {
            Broker.start();
            AlertService.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * The class annotated as a service
     */
    public final E instance;
    /**
     * The javalin server used to host this service.
     */
    private Javalin server;
    /**
     * Port for the service
     */
    @CommandLine.Option(
            names = {"--port", "-p"},
            description = "The name of a directory where CSV datafiles may be found. This option overrides and data-directory setting in a configuration file.",
            type = Integer.class,
            defaultValue = "0"
    )
    public Integer port=0;
    /**
     * Commands enables/disables
     */
    @CommandLine.Option(
            names = {"--commands", "-o"},
            description = "Enables or Disables Commands during runtime from sys.in",
            type = Boolean.class
    )
    Boolean commands = false;
    /**
     * Domain of the server, default is localhost
     */
    @CommandLine.Option(
            names = {"--domain", "-dom"},
            description = "The host name of the server"
    )
    String domain = "http://localhost";
    private static int SERVICE_COUNT = 0;
    private final Logger logger;
    /**
     * Used for waiting
     */
    private final Object lock = new Object();
    private boolean started = false;
    private boolean stopped = false;


    /**
     * Create a service instance.
     * Run Execute on this instance to set up and start the service
     * <br><br>
     * Must use the Service annotations
     * <br><br>
     * Use picocli's @CommandLine.Option
     * for custom fields to have them instantiated by the Properties of this service
     * <br><br>
     * Use Controllers annotation for any routes you wish to set up.
     * <br><br>
     * @param instance an instantiated object that will be run as a service.
     *
     */
    public Service(E instance){
        checkClassAnnotation(instance.getClass());
        this.instance = instance;
        logger = formatted("Annotation Handler: " + instance.getClass().getSimpleName(),  "\u001B[38;5;215m", "\u001B[38;5;221m");
        logger.info("Service " + instance.getClass().getSimpleName() + " has been created.");
    }
    /**
     * This method sets up and runs the Javalin server instance.
     * It creates a new instance of the given class with
     * the Service annotation and initializes the
     * properties of the class by calling the initProperties method.
     * It also initializes the Javalin server by calling the initHttpServer method.
     * <br/><br/>
     * @param args CLI arguments
     * @return A Service object with an instance of your class.
     */
    public Service<E> execute(String ... args){
        try {
            SERVICE_COUNT++;
            logger.info("Active Service Count: " + SERVICE_COUNT);
            Method[] methods = instance.getClass().getMethods();
            initProperties(args);
            logger.info("Properties Instantiated");
            initHttpServer(methods);
            logger.info("Javalin Server Created");

            if (handleMethods(methods, RunBefore.class)){
                logger.info("Initialization Methods Run");
            }

            activate();
            logger.info("Service Started");

            if (handleMethods(methods, RunAfter.class)){
                logger.info("Post Methods Run");
            }
            if (startListening(methods)){
                logger.info("Message Listeners Active");
            }

            if (startPublishing(instance.getClass().getFields())){
                logger.info("Message Publishers Active");
            }

            return this;
        } catch (Exception e){
            publishSevere(instance.getClass().getSimpleName(), "Failed to start", e);
            SERVICE_COUNT--;
            return null;
        }
    }

    /**
     * Stops this service
     * @throws AlreadyStoppedException if you try stop it a second time.
     */
    public void stop() throws AlreadyStoppedException{
        if (stopped){
            throw new AlreadyStoppedException("This service is designed to be stopped once");
        }
        stopped = true;
        server.stop();
        SERVICE_COUNT--;
        logger.info("Active Service Count: " + SERVICE_COUNT);
        publishWarning(instance.getClass().getSimpleName(), "Service stopped");
    }


    private boolean startPublishing(Field[] fields) {
        Stream<Field> f = Arrays
                .stream(fields)
                .filter(field -> field.isAnnotationPresent(Publish.class));

        List<Field> methodList = f.toList();

        if (methodList.isEmpty()){
            return false;
        }

        methodList.forEach(this::createPublisher);
        return true;
    }

    private void createPublisher(Field field) {
        Thread publisherThread = new Thread(() -> {

            Publish annotation = field.getAnnotation(Publish.class);
            Publisher l = new Publisher(annotation.prefix().prefix + annotation.destination(), field.getName());

            try {
                if (!annotation.overrideURL().equals("")){
                    l.overrideURL(annotation.overrideURL());
                }

                l.publish((Queue<String>) field.get(instance));
            } catch (IllegalAccessException | JMSException | InterruptedException | URISyntaxException e) {
                publishSevere(instance.getClass().getSimpleName(), "Failed to create publisher for " + field.getName(), e);
            }

        });
        publisherThread.setName("Annotation Handler: " + field.getName());
        publisherThread.start();
    }

    private boolean startListening(Method[] methods) {
        Stream<Method> m = Arrays
                .stream(methods)
                .filter(method -> method.isAnnotationPresent(Listen.class));

        List<Method> methodList = m.toList();

        if (methodList.isEmpty()){
            return false;
        }

        methodList.forEach(this::createListener);
        return true;
    }

    private void createListener(Method method) {
        Thread listenerThread = new Thread(() -> {
            Listen annotation = method.getAnnotation(Listen.class);
            Listener l = new Listener(annotation.prefix().prefix + annotation.destination(), method.getName());

            try {
                if (!annotation.overrideURL().equals("")){
                    l.overrideURL(annotation.overrideURL());
                }

                l.listen((message) -> {
                    try {
                        if (method.getAnnotation(Listen.class).withServiceAsArg()){
                            method.invoke(instance, message, this);
                        } else {
                            method.invoke(instance, message);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        publishWarning(instance.getClass().getSimpleName() + " : " +  method.getName(), "Has failed to process a message");
                    }
                });
            } catch (JMSException | URISyntaxException e) {
                publishSevere(instance.getClass().getSimpleName(), "Failed to create listener for " + method.getName(), e);
            }
        });
        listenerThread.setName("Annotation Handler: " + method.getName());
        listenerThread.start();

    }

    private void run() {
        if (started){
            throw new AlreadyStartedException("This service is designed to be run once");
        }

        started = true;
        server.start(port);

        synchronized (lock){
            lock.notify();
        }

        if (commands) {
            logger.info("Commands are active");
            Thread th = new Thread(this::startCommands);
            th.setName("Command Handler " + instance.getClass().getSimpleName());
            th.start();

        }
    }

    /**
     * Takes a service and runs it in a separate thread.
     */
    private void activate(){
        Thread t = new Thread(this::run);
        t.setName(this.instance.getClass().getSimpleName());
        t.start();

        try {
            synchronized (lock){
                lock.wait();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Allows accepting certain general commands from sys.in during runtime
     */
    private void startCommands(){
        Scanner s = new Scanner(System.in);
        String nextLine;
        while ((nextLine = s.nextLine())!=null) {
            String[] args = nextLine.split(" ");
            switch (args[0].toLowerCase()) {
                case "quit" -> {
                    stop();
                    System.exit(0);
                    return;
                }

                case "help" -> System.out.println(
                        """
                                commands available:
                                    'help' - list of commands
                                    'quit' - close the service
                                """
                );
            }
        }
    }

    private void handleCustomJavalinConfigs(Method[] methods, JavalinConfig javalinConfig) {
        logger.info("Handling Custom Javalin Configs");
        Arrays
                .stream(methods)
                .filter(method -> method.isAnnotationPresent(CustomJavalinConfig.class))
                .forEach(method -> handleCustomJavalinConfig(method, javalinConfig));
    }

    private void handleCustomJavalinConfig(Method method, JavalinConfig javalinConfig) {
        logger.info("Invoking " + method.getName());
        checkHasJavalinConfigAsArg(method);
        try {
            method.invoke(instance, javalinConfig);
        } catch (IllegalAccessException | InvocationTargetException e) {
            publishSevere(instance.getClass().getSimpleName(), "Failed to handle custom javalin config on " + method.getName(), e);
        }
    }

    private JsonMapper handleCustomJSONMapper(Method[] methods){
        List<Method> mapper = Arrays
                .stream(methods)
                .filter(method -> method.isAnnotationPresent(CustomJSONMapper.class))
                .toList();

        if (mapper.size() > 1){
            publishWarning(instance.getClass().getSimpleName(), "Has more than one custom JSON Mapper");
        }

        if (mapper.isEmpty()){
            return createJsonMapper();
        }

        Method method = mapper.get(0);
        checkForNoArgs(method);

        try {
            return (JsonMapper) method.invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            publishSevere(instance.getClass().getSimpleName(), "Failed to handle custom JsonMapper on " + method.getName(), e);
            return null;
        }
    }

    /**
     * Use GSON for serialisation instead of Jackson by default
     * because GSON allows for serialisation of objects without noargs constructors.
     *
     * @return A JsonMapper for Javalin
     */
    private JsonMapper createJsonMapper() {
        return new GSONMapper(instance.getClass().getSimpleName());
    }

    private boolean handleMethods(Method[] methods, Class<? extends Annotation> annotation) {
        Stream<Method> m = Arrays
                .stream(methods)
                .filter(method -> method.isAnnotationPresent(annotation));

        List<Method> methodList = m.toList();

        if (methodList.isEmpty()){
            return false;
        }

        methodList.forEach(method -> handleMethod(method , annotation));
        return true;
    }

    private void handleMethod(Method method, Class<? extends Annotation> annotationClass){
            boolean willUseServiceAsArg = false;
            logger.info("Attempting to invoke " + method.getName());

            if (annotationClass.equals(RunAfter.class)) {
                willUseServiceAsArg = method.getAnnotation(RunAfter.class).withServiceAsArg();
            }

            if (annotationClass.equals(RunBefore.class)) {
                willUseServiceAsArg = method.getAnnotation(RunBefore.class).withServiceAsArg();
            }

            try {
                if (willUseServiceAsArg) {
                    method.invoke(instance, this);
                } else {
                    method.invoke(instance);
                }
                logger.info("Invoked " + method.getName() + " successfully");
            } catch (IllegalAccessException | InvocationTargetException e) {
                publishSevere(instance.getClass().getSimpleName(), "Failed to run initialization method " + method.getName(), e);
            }
    }

    /**
     * Handles the CLI and properties file to configure the service
     *
     * @param args CLI arguments
     */
    private void initProperties(String... args) {
        populateFields(this, args);
    }

    /**
     * Gets the routes from the routes package in the given services package
     */
    private void addRoutes(){
        new Controllers(instance).getEndpoints().forEach(server::routes);
    }

    /**
     * Creates the javalin server.
     * By default, this just loads the JsonMapper from createJsonMapper
     */
    private void initHttpServer(Method[] methods) {
        server = Javalin.create(
            javalinConfig -> {
                handleCustomJavalinConfigs(methods, javalinConfig);
                if (instance.getClass().getAnnotation(AsService.class).AnyHost()){
                    javalinConfig.plugins.enableCors(cors -> cors.add(CorsPluginConfig::anyHost));
                }
                javalinConfig.jsonMapper(handleCustomJSONMapper(methods));
            }
        );
        this.addRoutes();
    }

    /**
     * Gets the URL of this service
     * @return a string representing the URL of this service
     */
    public String url() {
        return domain + ":" + port;
    }

    /**
     * Annotates a class as a service and marks it as able to be used as such.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface AsService {
        /**
         * Boolean that when true will allow other origins to access these routes
         * @return Allows all hosts if true
         */
        boolean AnyHost() default true;
    }

    /**
     * Marks a method to be run first in Javalin.create()
     * <br><br>
     * Requires that the method accepts a JavalinConfig object
     * as it's only parameter
     * <br><br>
     * This method can be a non-static or static member of the instance class
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface CustomJavalinConfig {}

    /**
     * Marks a method to be used to get a JSON Mapper
     * <br><br>
     * Requires that the method returns an instantiated JsonMapper object
     * and that it has no arguments
     * <br><br>
     * This method can be a non-static or static member of the instance class
     * and only ONE method can be annotated as a custom JSON mapper
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface CustomJSONMapper {}

    /**
     * Marks a method to be run before the service starts.
     * <br><br>
     * Requires that the method has no arguments, or Service typed as the instance class
     * as it's only argument
     * <br><br>
     * This method can be a non-static or static member of the instance class
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface RunBefore {
        /**
         * Marks this method as one that accepts the service as an argument
         * @return Will add Service object when invoking method if true
         */
        boolean withServiceAsArg() default false;
    }

//        Thread thread = new Thread(() -> {
    /**
     * Marks a method to be run after the service starts.
     * <br><br>
     * Requires that the method has no arguments, or Service typed as the instance class
     * as it's only argument
     * <br><br>
     * This method can be a non-static or static member of the instance class
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface RunAfter {
        /**
         * Marks this method as one that accepts the service as an argument
         * @return Will add Service object when invoking method if true
         */
        boolean withServiceAsArg() default false;
    }

    /**
     * Marks a method to be used as a message consumer for a listener
     * <br><br>
     * Requires that the method has a String argument for the message to handle, and optionally a Service typed as the instance class
     * as it's second argument
     * <br><br>
     * This method can be a non-static or static member of the instance class
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Listen {
        /**
         * The name of the queue or topic this method will listen on
         * @return the name of the queue/topic
         */
        String destination();

        /**
         * prefix marking this as listening on a queue or topic
         * @return if its a queue or topic
         */
        Prefix prefix() default Prefix.TOPIC;

        /**
         * Adding this will ignore the url created by the prefix and destination.
         * Use this to listen to an external queue or topic
         * @return a url to override the locally hosted amq url.
         */
        String overrideURL() default "";

        /**
         * Marks this method as one that accepts the service as a second argument
         * @return Will add Service object when invoking method if true
         */
        boolean withServiceAsArg() default false;

    }

    /**
     * Marks a Queue object to be used as a message producer for a publisher
     * <br><br>
     * Requires that the queue contains only String objects
     * <br><br>
     * This field can be a non-static or static member of the instance class and cannot be null
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Publish {
        /**
         * The name of the queue or topic this method will listen on
         * @return the name of the queue/topic
         */
        String destination();
        /**
         * prefix marking this as listening on a queue or topic
         * @return if its a queue or topic
         */
        Prefix prefix() default Prefix.TOPIC;

        /**
         * Adding this will ignore the url created by the prefix and destination.
         * Use this to listen to an external queue or topic
         * @return a url to override the locally hosted amq url.
         */
        String overrideURL() default "";
    }
}

/**
 * Contains All the Special Checks that throw exceptions during execution of a service
 */
class Checks {
    static void checkForNoArgs(Method method){
        if (method.getGenericParameterTypes().length != 0) {
            throw new MethodTakesNoArgumentsException(method.getName() + " must have no arguments");
        }
    }

    static void checkClassAnnotation(Class<?> clazz){
        if (!clazz.isAnnotationPresent(Service.AsService.class)){
            throw new NotAServiceException(
                    clazz.getSimpleName() + " is not an Annotated with @AsService"
            );
        }
    }

    static void checkHasJavalinConfigAsArg(Method method){
        checkHasArgs(method, JavalinConfig.class);
    }

    static void checkHasArgs(Method method, Type ... types){
        Type[] params = method.getGenericParameterTypes();
        if (params.length != types.length){
            throw new ArgumentException(
                    method.getName() + " has not got enough parameters");
        }

        for (int i = 0; i < types.length; i++){
            if (!params[i].equals(types[i])){
                throw new ArgumentException(
                        method.getName() + " must have "+ types[i].getTypeName() +" as it's parameter, not " + params[i].getTypeName());
            }
        }


    }
}

class NotAServiceException extends RuntimeException {
    public NotAServiceException(String message){
        super(message);
    }
}

class MultipleJSONMapperMethodsException extends RuntimeException {
    public MultipleJSONMapperMethodsException(String message){
        super(message);
    }
}

class MethodTakesNoArgumentsException extends RuntimeException {
    public MethodTakesNoArgumentsException(String message){
        super(message);
    }
}

class ArgumentException extends RuntimeException {
    public ArgumentException(String message){
        super(message);
    }
}

class AlreadyStartedException extends RuntimeException {
    public AlreadyStartedException(String message){
        super(message);
    }
}
class AlreadyStoppedException extends RuntimeException {
    public AlreadyStoppedException(String message){
        super(message);
    }
}



