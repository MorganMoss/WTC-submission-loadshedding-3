package wethinkcode.service.messages;

import org.eclipse.jetty.util.BlockingArrayQueue;
import wethinkcode.service.Service;

import java.util.Queue;
import java.util.logging.Logger;

import static wethinkcode.logger.Logger.formatted;

@Service.AsService
public class ErrorHandler {
    @Service.Publish(destination = "errors", prefix = Prefix.QUEUE)
    public static final Queue<String> ERRORS;
    private static final ErrorHandler INSTANCE;
    private static final Service<ErrorHandler> SERVICE;
    private static final Thread RUNTIME_EXCEPTION_CATCHER ;

    private static final Logger LOGGER ;


    static {
        LOGGER = formatted("Error Handler", "\u001B[38;5;88m", "\u001B[38;5;196m");
        ERRORS = new BlockingArrayQueue<>();
        INSTANCE = new ErrorHandler();
        SERVICE = new Service<>(INSTANCE).execute();
        RUNTIME_EXCEPTION_CATCHER = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    publishError(e);
                }
            }
        });
    }

    public static void start(){
        if (RUNTIME_EXCEPTION_CATCHER.isAlive()){
            LOGGER.info("Already started.");
        }
        LOGGER.info("Starting Error Handler...");
        RUNTIME_EXCEPTION_CATCHER.start();
    }

    public static void publishError(Exception e){
        ERRORS.add(e.toString());
        if (SERVICE == null) {
            handleError(ERRORS.remove());
        }
    }

    @Service.Listen(destination = "errors", prefix = Prefix.QUEUE)
    public static void handleError(String message){
        LOGGER.info(message);
    }


}
