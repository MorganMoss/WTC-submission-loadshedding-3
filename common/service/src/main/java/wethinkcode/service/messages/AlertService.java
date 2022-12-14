package wethinkcode.service.messages;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.eclipse.jetty.util.BlockingArrayQueue;
import wethinkcode.service.Service;

import java.util.Queue;
import java.util.logging.Logger;

import static wethinkcode.logger.Logger.formatted;

@Service.AsService
public class AlertService {
    @Service.Publish(destination = "alert")
    public static final Queue<String> ERRORS;
    private static final AlertService INSTANCE;
    private static final Thread RUNTIME_EXCEPTION_CATCHER ;

    private static final Logger LOGGER;

    static {
        LOGGER = formatted("ALERT", "", "");
        ERRORS = new BlockingArrayQueue<>();
        INSTANCE = new AlertService();

        RUNTIME_EXCEPTION_CATCHER = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    publishSevere("UNKNOWN", "Uncaught runtime exception found!", e);
                }
            }
        });
    }

    public static void start(){
        if (RUNTIME_EXCEPTION_CATCHER.isAlive()){
            return;
        }
        new Service<>(INSTANCE).execute();
        RUNTIME_EXCEPTION_CATCHER.start();
    }

    private static void postToNTFY(String message){
        try {
            HttpResponse<JsonNode> request = Unirest.post("https://ntfy.sh/morgan-moss-alerts").body(message).asJson();
            int status = request.getStatus();
        } catch (UnirestException e) {
            LOGGER.warning("Cant reach nfty");
        }
    }

    public static void publishSevere(String location, String message, Exception e){
        LOGGER.severe("["+location+ "] " + message);
        postToNTFY(message);
        ERRORS.add("["+location+ "] [SEVERE] " + message);
        e.printStackTrace();
        System.exit(6);
    }

    public static void publishWarning(String location, String message){
        LOGGER.warning("["+location+ "] " + message);
        postToNTFY(message);
        ERRORS.add("["+location+ "] [WARNING] " + message);
    }
}
