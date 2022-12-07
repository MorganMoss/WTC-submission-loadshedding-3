package wethinkcode.service.messages;

import javax.jms.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static wethinkcode.logger.Logger.formatted;
import static wethinkcode.service.messages.Broker.getDestination;
import static wethinkcode.service.messages.Broker.getSession;

public class Listener {

    private final Logger logger;
    private final String destinationName;


    public Listener(String destinationName, String name) {
        this.logger = formatted("Listener " + name, "\u001b[38;5;9m", "\u001b[38;5;209m");
        this.destinationName = destinationName;
    }

    public void listen(Consumer<String> messageConsumer) {
        try {

        Session session = getSession();
        Destination destination = getDestination(session, destinationName);

        MessageConsumer consumer = session.createConsumer(destination);
        long start = System.currentTimeMillis();
        long count = 1;
            logger.info("Waiting for messages...");
            while (true) {
                Message msg = consumer.receive();
                if (msg instanceof TextMessage) {
                    String body = ((TextMessage) msg).getText();
                    logger.info("Received Message: " + "\u001b[38;5;203m" + body);
                    if ("SHUTDOWN".equals(body)) {
                        long diff = System.currentTimeMillis() - start;
                        logger.info(String.format("Received %d in %.2f seconds", count, (1.0 * diff / 1000.0)));
                        System.exit(0);
                    }
                    messageConsumer.accept(body);
                    logger.info("Message Processed");
                } else {
                    logger.info("Unexpected message type: " + msg.getClass());
                }
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
