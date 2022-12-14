package wethinkcode.service.messages;

import javax.jms.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static wethinkcode.logger.Logger.formatted;
import static wethinkcode.service.messages.AlertService.publishWarning;
import static wethinkcode.service.messages.Broker.getDestination;
import static wethinkcode.service.messages.Broker.getSession;

public class Listener {

    private final Logger logger;
    private final String destinationName;

    private URI uri;

    private final String name;
    public Listener(String destinationName, String name) {
        this.logger = formatted("Listener " + name, "\u001b[38;5;9m", "\u001b[38;5;209m");
        this.destinationName = destinationName;
        this.uri = Broker.BROKER_URI;
        this.name = name;
    }

    public void listen(Consumer<String> messageConsumer) throws JMSException {
        Session session = getSession(uri);
        Destination destination = getDestination(session, destinationName);

        MessageConsumer consumer = session.createConsumer(destination);
            logger.info("Waiting for messages...");
            while (Broker.ACTIVE) {
                Message msg;
                try {
                    msg = consumer.receive();
                } catch (JMSException e) {
                    publishWarning("Listener " + name, "Could not receive messages");
                    return;
                }

                if (!(msg instanceof TextMessage)) {
                    logger.info("Unexpected message type: " + msg.getClass());
                    continue;
                }

                String body = ((TextMessage) msg).getText();
                logger.info("Received Message: " + "\u001b[38;5;203m" + body);

                if ("SHUTDOWN".equals(body)) {
                    logger.info("Shutting down Listener");
                    return;
                }
                messageConsumer.accept(body);
                logger.info("Message Processed");
            }
    }



    public void overrideURL(String overrideURL) throws URISyntaxException {
        uri = new URI(overrideURL);
    }
}
