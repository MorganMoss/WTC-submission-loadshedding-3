package wethinkcode.service.messages;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import picocli.CommandLine;

import javax.jms.*;
import java.net.URI;
import java.util.logging.Logger;

import static wethinkcode.logger.Logger.formatted;
import static wethinkcode.service.messages.ErrorHandler.publishError;

/**
 * The Broker class is a singleton that provides methods for starting and stopping an embedded messaging broker
 * using Apache ActiveMQ. The class also provides methods for creating connections, sessions, and destinations for
 * sending and receiving messages. The Broker class is intended to be used as a command line interface, as it defines
 * command line options for the broker's port and domain.
 */
public class Broker {
    /**
     * This field holds the port number of the activeMQ broker. It is initialized to the value 6969 and can be set using the --broker-port or -bp command line options.
     */
    @CommandLine.Option(
            names = {"--broker-port", "-bp"},
            description = "The port of the activeMQ broker",
            type = Integer.class
    )
    public static String PORT;
    /**
     * This field holds the domain name or hostname of the activeMQ broker. It is initialized to the value localhost and can be set using the --broker-domain or -bd command line options.
     */
    @CommandLine.Option(
            names = {"--broker-domain", "-bd"},
            description = "The host name of the activeMQ broker"
    )
    public static String DOMAIN;
    /**
     * This field holds a boolean value indicating whether the embedded broker is currently active. It is initialized to false and is set to true when the broker is started, and set to false when the broker is stopped.
     */
    public static boolean ACTIVE;
    private static ActiveMQConnectionFactory FACTORY;
    private static Connection CONNECTION;

    private static final String NAME;
    private static final Logger LOGGER;
    private static final String URL;
    private static final BrokerService SERVICE ;


    static {
        LOGGER = formatted("Embedded Broker", "\u001B[38;5;247m", "\u001B[38;5;249m");

        NAME = "Broker";
        PORT = "6969";
        DOMAIN = "localhost";
        URL = DOMAIN + ":" + PORT;

        SERVICE = new BrokerService();
        ACTIVE = false;

        try {
            setupBroker();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setupBroker() throws Exception {
        LOGGER.info("Starting Embedded Broker...");
        SERVICE.setPersistent(false);
        SERVICE.addConnector(new URI("tcp://"+ URL));
        SERVICE.setBrokerName(NAME);
    }

    public static void start() throws Exception {
        if (ACTIVE) {
            LOGGER.info("Already started.");
        }
        SERVICE.start();
        ACTIVE = true;
        FACTORY = new ActiveMQConnectionFactory(new URI("tcp://"+ URL));
        LOGGER.info("Started Embedded Broker");

        Runtime.getRuntime().addShutdownHook(new Thread(Broker::stopBroker));
    }

    private static void stopBroker(){
        LOGGER.info("Stopping Embedded Broker...");

        if (!SERVICE.isStarted()){
            return;
        }
        try {
            SERVICE.stop();
            ACTIVE = false;
        } catch (Exception e) {
            publishError(e);
        }
        LOGGER.info("Stopped Embedded Broker");

    }

    private static void startConnection() throws JMSException {
        CONNECTION = FACTORY.createConnection();
        CONNECTION.start();
    }

    /**
     * It first calls the startConnection method to establish a connection to the broker, and then creates a Session object using the Connection object. If an exception is thrown while establishing the connection or creating the session, the method logs the error using the publishError method of the ErrorHandler class and exits the application.
     * @return a Session object that can be used for sending and receiving messages
     * @throws JMSException if the connection fails
     */
    public static Session getSession() throws JMSException {
        startConnection();
        return CONNECTION.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    /**
     * It uses the Prefix enum to determine whether the destination is a topic or a queue, and then creates the appropriate destination object using the provided Session object. If an exception is thrown while creating the destination, the method logs the error using the publishError method of the ErrorHandler class and exits the application.
     * @param session The session that this destination will be focused to
     * @param destinationName The name of the destination, (Prefix + Destination name)
     * @return a Destination object for the specified destination name.
     */
    public static Destination getDestination(Session session, String destinationName) {
        Destination destination = null;
        try {
            if (destinationName.startsWith(Prefix.TOPIC.prefix)) {
                destination = session.createTopic(destinationName.substring(Prefix.TOPIC.prefix.length()));
            } else {
                destination = session.createQueue(destinationName.substring(Prefix.QUEUE.prefix.length()));
            }
        } catch (JMSException e){
            publishError(e);
            System.exit(1);
        }
        return destination;
    }
}
