package wethinkcode.service.messages;

import javax.jms.*;
import java.util.Queue;
import java.util.logging.Logger;

import static wethinkcode.logger.Logger.formatted;
import static wethinkcode.service.messages.Broker.getDestination;
import static wethinkcode.service.messages.Broker.getSession;
import static wethinkcode.service.messages.ErrorHandler.publishError;

public class Publisher {
    private final Logger logger;
    private final String destinationName;

    public Publisher(String destinationName, String name) {
        this.logger = formatted("Listener " + name, "\u001b[38;5;9m", "\u001b[38;5;209m");
        this.destinationName = destinationName;
    }

    public void publish(Queue<String> messages) {
        logger.info("Starting Listener on " + destinationName);
        try {
            Session session = getSession();
            Destination destination = getDestination(session, destinationName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            while (Broker.ACTIVE){
                if (messages.isEmpty()){
                    Thread.sleep(10);
                    continue;
                }
                String message = messages.remove();
                producer.send(session.createTextMessage(message));
                logger.info("Sent Message: " + "\u001b[38;5;203m" + message);
            }
        } catch (JMSException | InterruptedException e) {
            publishError(e);
        }
    }
}
