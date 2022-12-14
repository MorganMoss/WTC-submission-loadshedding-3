package wethinkcode.manager;

import wethinkcode.service.Service;
import wethinkcode.service.messages.Prefix;

import java.util.ArrayList;

@Service.AsService
public class AlertListener {
    private static Service<AlertListener> service;
    public static final ArrayList<String> alerts =  new ArrayList<>();

    @Service.Listen(destination = "alerts", prefix = Prefix.TOPIC)
    public void captureError(String message){
        alerts.add(message);
        System.out.println(message);
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
