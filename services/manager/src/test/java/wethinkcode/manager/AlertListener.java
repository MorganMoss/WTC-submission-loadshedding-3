package wethinkcode.manager;

import org.eclipse.jetty.util.BlockingArrayQueue;
import wethinkcode.service.Service;
import wethinkcode.service.messages.Prefix;

import java.util.ArrayList;
import java.util.Queue;

@Service.AsService
public class AlertListener {
    private static Service<AlertListener> service;
    public static final Queue<String> alerts =  new BlockingArrayQueue<>();

    @Service.Listen(destination = "alert", prefix = Prefix.TOPIC)
    public void captureError(String message){
        alerts.add(message);
    }

    public static void stop(){
        service.stop();
        alerts.clear();
    }

    public static void start(){
        AlertListener getAlerts = new AlertListener();
        service = new Service<>(getAlerts).execute("--port=0");
    }
}
