package wethinkcode.service.messages;



import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import picocli.CommandLine;

import javax.jms.*;
import java.net.URI;
import java.util.logging.Logger;

import static wethinkcode.logger.Logger.formatted;

public class Broker {
    @CommandLine.Option(
            names = {"--broker-port", "-bp"},
            description = "The port of the activeMQ broker",
            type = Integer.class
    )
    public static String PORT = "6969";
    @CommandLine.Option(
            names = {"--broker-domain", "-bd"},
            description = "The host name of the activeMQ broker"
    )
    public static String DOMAIN = "localhost";
    public static final String NAME = "Broker";
    private static final Logger BROKER_LOGGER = formatted("Message Queue", "\u001B[38;5;247m", "\u001B[38;5;249m");
    private static final String URL = DOMAIN + ":" + PORT;
    private static final BrokerService SERVICE = new BrokerService();
    private static ActiveMQConnectionFactory FACTORY;

    public static boolean ACTIVE = false;
    private static Connection CONNECTION;

    private static void startServer() throws Exception {
        BROKER_LOGGER.info("Starting Embedded Broker...");
        SERVICE.setPersistent(false);
        SERVICE.addConnector(new URI("tcp://"+ URL));
        SERVICE.setBrokerName(NAME);
        SERVICE.start();
        ACTIVE = true;
        FACTORY = new ActiveMQConnectionFactory(new URI("tcp://"+ URL));
        BROKER_LOGGER.info("Started Embedded Broker");

        Runtime.getRuntime().addShutdownHook(new Thread(Broker::stopBroker));
    }

    public static void stopBroker(){
        BROKER_LOGGER.info("Stopping Embedded Broker...");

        if (!SERVICE.isStarted()){
            return;
        }

        try {
            SERVICE.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BROKER_LOGGER.info("Stopped Embedded Broker");

    }

    public static void startBroker(){
        if (SERVICE.isStarted()){
            return;
        }

        try {
            startServer();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        BROKER_LOGGER.info("Started Embedded Broker");
    }

    private static void startConnection() throws JMSException {
        CONNECTION = FACTORY.createConnection();
        CONNECTION.start();
    }

    public static Session getSession() throws JMSException {
        startConnection();

        return CONNECTION.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    public static Destination getDestination(Session session, String destinationName) {
        Destination destination = null;
        try {
            if (destinationName.startsWith(Prefix.TOPIC.prefix)) {
                destination = session.createTopic(destinationName.substring(Prefix.TOPIC.prefix.length()));
            } else {
                destination = session.createQueue(destinationName.substring(Prefix.QUEUE.prefix.length()));
            }
        } catch (JMSException e){
            e.printStackTrace();
            System.exit(1);
        }
        return destination;
    }
}
