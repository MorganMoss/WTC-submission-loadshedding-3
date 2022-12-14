package wethinkcode.service.messages;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import picocli.CommandLine;

import javax.jms.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.logging.Logger;

import static wethinkcode.logger.Logger.formatted;
import static wethinkcode.service.messages.AlertService.*;

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
    /**
     * The URI that connects to the locally created broker.
     */
    public static final URI BROKER_URI;

    private static final HashMap<URI, Connection> CONNECTIONS;

    private static final String NAME;
    private static final Logger LOGGER;
    private static final BrokerService SERVICE ;


    static {
        LOGGER = formatted("Embedded Broker", "\u001B[38;5;247m", "\u001B[38;5;249m");

        CONNECTIONS = new HashMap<>();

        NAME = "Broker";
        PORT = String.valueOf(getOpenPort());
        DOMAIN = "localhost";

        try {
            BROKER_URI = new URI("tcp://" + DOMAIN + ":" + PORT);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        SERVICE = new BrokerService();
        ACTIVE = false;

        try {
            setupBroker();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int getOpenPort() {
        try {
            ServerSocket s = new ServerSocket(0);
            int port = s.getLocalPort();
            s.close();
            return port;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void setupBroker() throws Exception {
        LOGGER.info("Starting Embedded Broker...");
        SERVICE.setPersistent(false);
        SERVICE.addConnector(BROKER_URI);
        SERVICE.setBrokerName(NAME);
    }

    public static void start() throws Exception {
        if (ACTIVE) {
            LOGGER.info("Already started.");
        }
        SERVICE.start();
        ACTIVE = true;
        LOGGER.info("Started Embedded Broker");

        Runtime.getRuntime().addShutdownHook(new Thread(Broker::stopBroker));
    }

    private static void stopBroker(){
        LOGGER.info("Stopping Embedded Broker...");

        if (!SERVICE.isStarted()){
            return;
        }
        try {
            ACTIVE = false;
            SERVICE.stop();
        } catch (Exception e) {
            publishWarning("Embedded Broker", "Failed to stop cleanly.");
        }
        LOGGER.info("Stopped Embedded Broker");

    }

    private static Connection  getConnection(URI uri) {
        return CONNECTIONS.getOrDefault(uri, newConnection(uri));

    }

    private static Connection newConnection(URI uri){
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(uri);
        Connection connection = null;
        try {
            connection = factory.createConnection();
            connection.start();
        } catch (JMSException e) {
            publishSevere("Broker Service", "Failed to create connection to " + uri, e);
        }
        return connection;
    }

    /**
     * It first calls the startConnection method to establish a connection to the broker, and then creates a Session object using the Connection object. If an exception is thrown while establishing the connection or creating the session, the method logs the error using the publishError method of the AlertService class and exits the application.
     * @param uri of the mq service you're connecting to.
     * @return a Session object that can be used for sending and receiving messages
     * @throws JMSException if the connection fails
     */
    public static Session getSession(URI uri){
        try {
            return getConnection(uri).createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            publishSevere("Embedded Broker", "Failed to get a Session for " + uri, e);
            return null;
        }
    }

    /**
     * It uses the Prefix enum to determine whether the destination is a topic or a queue, and then creates the appropriate destination object using the provided Session object. If an exception is thrown while creating the destination, the method logs the error using the publishError method of the AlertService class and exits the application.
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
            publishSevere("Embedded Broker", "Failed to get a Destination", e);
        }
        return destination;
    }
}
