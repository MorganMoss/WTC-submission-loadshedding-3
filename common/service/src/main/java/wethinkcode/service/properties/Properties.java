package wethinkcode.service.properties;

import picocli.CommandLine;
import wethinkcode.service.Service;
import wethinkcode.service.messages.Broker;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static wethinkcode.logger.Logger.formatted;
import static wethinkcode.service.messages.ErrorHandler.publishError;

/**
 * The Properties class represents a collection of properties that are loaded from a properties file. This class allows properties to be specified via a properties file or via the command-line interface (CLI), and it uses reflection to populate fields in a Service instance with the properties that are loaded from the properties file.
 */
public class Properties{
    /**
     * This field represents the Reader instance that is used to read the properties file. This field is initialized in the Properties constructor and is used to read the properties file and populate the properties field. The type of Reader that is used will depend on whether a custom properties file was specified via the CLI or not. If a custom properties file was specified, a FileReader will be used. If no custom properties file was specified, an InputStreamReader will be used instead.
     */
    private Reader reader;
    /**
     * This field represents a Logger instance that is used to log messages related to the Properties class. This field is initialized in the Properties constructor and is used throughout the class to log information, warnings, and errors.
     */
    private final Logger logger;
    /**
     * This field is a java.util.Properties instance that is used to store the properties that are loaded from the properties file. This field is initialized in the Properties constructor and is used throughout the class to store the properties that are loaded from the file.
     */
    private final java.util.Properties properties = new java.util.Properties();

    /**
     * This field represents the properties file that is specified via the command-line interface (CLI). This field is annotated with the CommandLine.Option annotation, which allows it to be set via the CLI. This field is a File instance that represents the properties file on the local file system. If no properties file is specified via the CLI, this field will be null.
     */
    @CommandLine.Option(
            names = {"--config", "-c"},
            description = "A file pathname referring to an (existing!) configuration file in standard Java properties-file format",
            type = File.class,
            echo = true
    )
    File config = null;

    /**
     * This method is a static method that takes
     * a Service instance and a variable number
     * of arguments as input and
     * populates the fields of the given Service instance.
     * @param service a Service instance
     * @param args variable number of arguments
     */
    public static void populateFields(Service<?> service, String ... args){
        try {
            new Properties(service, args);
        } catch (IOException e){
            publishError(e);
            System.exit(1);
        }
    }

    /**
     * This method is the constructor
     * of the Properties class.
     * It creates a new Properties instance
     * and loads its default properties
     * from the given relative position.
     * @param service instance
     * @param args variable number of arguments
     * @throws IOException if an I/O error occurs.
     */
    Properties(Service<?> service, String ... args) throws IOException {
        logger = formatted(this.getClass().getSimpleName() + " " + service.instance.getClass().getSimpleName(),
                "\u001B[38;5;129m", "\u001B[38;5;165m");

        loadProperties(service.instance.getClass());

        ArrayList<Object> objects = new ArrayList<>(List.of(this, service, service.instance));
        if (!Broker.ACTIVE){
            //I've instantiated it because this instance will be ignored anyway
            //as all cli options are going to be static.
            objects.add(new Broker());
        }

        HashMap<Object, String[]> propertiesArgsForObjects = getArgsFromProperties(objects.toArray());

        propertiesArgsForObjects.forEach((object, propertiesArgs) -> {
            String[] cliArgs = getArgsToParse(object, args);
            String[] combinedArgs = Stream.of(
                    cliArgs,
                    Arrays
                        .stream(propertiesArgs)
                        .filter(arg -> {
                            String name = Arrays.stream(arg.split("=")).map(String::strip).findFirst().get();
                            return !Arrays.stream(cliArgs).toList().contains(name);
                            }
                        )
                        .toArray(String[]::new)).flatMap(Arrays::stream).toArray(String[]::new);
            safeExecute(object, combinedArgs);
        });
    }

    /**
     * This method is a static method that takes
     * an Object instance as input
     * and returns a list of argument names for the given object.
     * The argument names are determined
     * using reflection
     * by finding all fields that are
     * annotated with the CommandLine.Option
     * annotation and extracting the names from the annotation.
     * @param o an Object instance as input
     * @return a list of argument names for the given object.
     */
    private static List<String> getArgNames(Object o){
         return Arrays
                .stream(o.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(CommandLine.Option.class))
                .map(field -> field.getAnnotation(CommandLine.Option.class))
                .map(CommandLine.Option::names)
                .flatMap(Arrays::stream)
                .toList();
    }

    /**
     * This method is a static method that takes
     * an Object instance and a variable number
     * of arguments as input and returns the subset
     * of the given arguments that should be parsed
     * by the given object. This method uses reflection
     * to determine the argument names for the given object
     * and filters the input arguments to only include those
     * that contain one of the argument names.
     * @param o an Object instance
     * @param args given arguments
     * @return the subset of the given arguments that should be parsed
     * by the given object
     */
    private static String[] getArgsToParse(Object o, String ... args){
        return  Arrays
                .stream(args)
                .filter(arg -> getArgNames(o).stream().anyMatch(arg::contains))
                .toArray(String[]::new);
    }


    /**
     * This method takes a Class instance
     * as input and uses reflection to fetch
     * the default properties file from resources.
     * It tries the child class first and then,
     * if that fails, it will try the Service class.
     * @param clazz the instance to start looking from
     */
    void loadDefaultProperties(Class<?> clazz){
        InputStream content = getDefaultPropertiesStream(clazz);
        logger.info("Loaded Default Config File");
        reader = new InputStreamReader(content);
    }

    /**
     * This method checks to see if there is a
     * custom properties file loaded via the
     * command-line interface (CLI) and, if found,
     * loads the custom properties file.
     * This method returns a boolean value indicating
     * whether a custom properties file was found and loaded.
     * @throws IOException If an I/O error occurs
     */
    boolean loadCustomProperties() throws IOException {
        if (config != null){
            logger.info("CLI Argument Found for Config File: " + config);
            reader = new FileReader(config);
        }

        return config != null;
    }

    /**
     * his method takes a Class instance as input and uses reflection to load the properties file for the given class.
     * @throws IOException if an I/O error occurs
     */
    void loadProperties(Class<?> clazz) throws IOException {
        if (!loadCustomProperties()) loadDefaultProperties(clazz);

        properties.load(reader);
        logger.info(
                "Properties File Contents: \n \u001B[38;5;205m"
                        + properties
                                .keySet()
                                .stream()
                                .map(key -> "\t" + key + " = " + properties.getProperty((String) key))
                                .collect(Collectors.joining("\n"))

                );

    }

    /**
     * This method takes a Class instance
     * as input and uses reflection to try
     * to fetch the default properties file
     * for the given class from the classpath.
     * If the default properties file cannot be found,
     * this method will throw an exception.
     * This method returns an InputStream
     * containing the contents of the default properties file.
     * @param child to fetch the default properties file
     * @return the contents of the default properties file
     */
    public static InputStream getDefaultPropertiesStream(Class<?> child){
        InputStream content;
        try {
            Path path = Path.of(new File(
                    child
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation().toURI()

            ).getPath()).resolve("default.properties");
            content = new FileInputStream(path.toFile());
            Objects.requireNonNull(content);
        } catch (NullPointerException | FileNotFoundException | URISyntaxException e){
            content = Properties.class.getResourceAsStream("/default.properties");
        }

        Objects.requireNonNull(content);

        return content;
    }

    /**
     * This method takes an Object instance and
     * a variable number of arguments as input
     * and uses reflection to try to parse the given
     * arguments using the CommandLine.parseArgs method
     * for the given object. If any exceptions occur during
     * parsing, they will be caught and ignored by this method.
     * @param o an Object instance as input
     * @param args variable number of arguments
     */
    private void safeExecute(Object o, String ... args){
        if (args.length == 0){
            logger.info(o.getClass().getSimpleName() + " has no arguments to parse.");
            return;
        }
        logger.info(
                "Parsing the following args to " + o.getClass().getSimpleName()
                        + "\n \u001B[38;5;205m"
                        + Arrays
                                .stream(args)
                                .map(arg -> "\t" + arg)
                                .collect(Collectors
                                .joining("\n"))
        );

        try {
            new CommandLine(o).parseArgs(args);
        } catch (CommandLine.InitializationException ignored) {}
    }

    /**
     * This method takes a list of Object instances
     * as input and uses reflection to populate the
     * empty fields of the given objects with the
     * corresponding property values.
     * @param objects to have their fields populated
     */
    private HashMap<Object, String[]> getArgsFromProperties(Object ... objects){
        HashMap<Object, String[]> argMap = new HashMap<>();
        Arrays
            .stream(objects)
            .forEach(object -> {
                            String[] args = Arrays
                                    .stream(object.getClass().getFields())
                                    .filter(field -> field.isAnnotationPresent(CommandLine.Option.class))
                                    .map(this::populateEmptyField)
                                    .filter(Objects::nonNull)
                                    .toArray(String[]::new);
                            argMap.put(object, args);
                    }
            );
        return argMap;
    }

    /**
     * This method takes an Object instance, a Field instance, and a String value as input and uses reflection to populate the given field of the given object with the given properties value. This method first checks to see if the given field is empty and, if it is, it sets the field to the given properties value.
     * @param field instance whereby the other values are found
     * @return a String representing the argument to be parsed.
     */
    private String populateEmptyField(Field field) {
        CommandLine.Option option = field.getDeclaredAnnotation(CommandLine.Option.class);
        if (properties.get(field.getName()) == null){
            return null;
        }
        String name = option.names()[0];
        String value = (String) properties.get(field.getName());
        return name + "=" + value;
    }
}
