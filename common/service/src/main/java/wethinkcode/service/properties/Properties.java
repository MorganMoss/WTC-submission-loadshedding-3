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

import static wethinkcode.logger.Logger.formatted;
import static wethinkcode.service.messages.AlertService.publishSevere;

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
            publishSevere("Properties", "Failed to populate the fields of " + service.instance.getClass().getSimpleName(), e);
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

        logger.info(
                "CLI Argument Contents: \n \u001B[38;5;205m"
                        + Arrays.stream(args)
                        .map(arg -> "\t" + arg)
                        .collect(Collectors.joining("\n"))

        );

        safeExecute(this, getArgsForObject(this, args));
        loadProperties(service.instance.getClass());

        if (!Broker.ACTIVE){
            Broker b = new Broker();
            safeExecute(b, getArgsForObject(b, args));
        }

        List
                .of(service, service.instance)
                .forEach(object -> safeExecute(object, getArgsForObject(object, args)));
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
     * Looks in CLI then Properties to try get an argument to parse for each @Option annotated field.
     * @param object that will be used when looking for args to parse
     * @param args CLI args given
     * @return The args to parse
     */
    private String[] getArgsForObject(Object object, String[] args){
        return Arrays
                .stream(object.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(CommandLine.Option.class))
                .map(field -> getArgForField(field, args))
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    /**
     * This method takes an Object instance, a Field instance, and a String value as input and uses reflection to populate the given field of the given object with the given properties value. This method first checks to see if the given field is empty and, if it is, it sets the field to the given properties value.
     * @param field instance whereby the other values are found
     * @return a String representing the argument to be parsed.
     */
    private String getArgForField(Field field, String[] args) {
        CommandLine.Option option = field.getDeclaredAnnotation(CommandLine.Option.class);
        Optional<String> cliArg = Arrays.stream(args).filter(arg -> Arrays
                        .stream(option.names())
                        .anyMatch(name -> arg.split("=")[0].equals(name))
                )
                .findFirst();

        if (cliArg.isPresent()) {
            return cliArg.get();
        }

        if (properties.get(field.getName()) == null){
            return null;
        }

        String name = option.names()[0];
        String value = (String) properties.get(field.getName());
        return name + "=" + value;
    }
}
