package wethinkcode.manager;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import wethinkcode.places.PlacesService;
import wethinkcode.schedule.ScheduleService;
import wethinkcode.service.Service;
import wethinkcode.stage.StageService;
import wethinkcode.web.WebService;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;

import static wethinkcode.service.messages.AlertService.publishWarning;
import static wethinkcode.service.properties.Properties.getDefaultPropertiesStream;

@Service.AsService
public class ManagerService {
    private static final ArrayList<Object> services = new ArrayList<>(){{
        add(new PlacesService());
        add(new StageService());
        add(new ScheduleService());
        add(new WebService());
    }};
    public final HashMap<Integer, Service<?>> ports = new HashMap<>();

    private void addToProperties(Properties properties, Service<ManagerService> s) {
        properties.setProperty("manager", s.url());
        properties.setProperty("port", String.valueOf(s.port + ports.size() + 1));
        properties.setProperty("commands", "false");
    }
    private void setUpProperties(File f, InputStream defaults, Service<ManagerService> s){
        Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(defaults));
            addToProperties(properties, s);
            properties.store(new FileOutputStream(f), f.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Service.RunAfter(withServiceAsArg = true)
    public void startAllServices(Service<ManagerService> s){
        Path folder;
        try {
            folder = Path.of(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        for (Object service : services){
            File f =new File(folder.resolve(service.getClass().getSimpleName() + ".properties").toUri());

            setUpProperties(f, getDefaultPropertiesStream(service.getClass()), s);

            ports.put(s.port + ports.size() + 10, new Service<>(service).execute("-c="+f.getAbsolutePath()));
        }
    }

    @Service.RunAfter
    public void startManualRouteDebugger(){
        Thread requester = new Thread(() ->
        {
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();

                String[] inputArr = input.split(" ");

                switch (inputArr[0].toLowerCase()) {
                    case "get" ->
                            runGetRequest(inputArr);
                    case "post" ->
                            runPostRequest(inputArr);
                    default -> {
                    }
                }
            }
        });
        requester.start();
    }

    // Method to run a Unirest.get request
    static void runGetRequest(String[] inputArr) {
        try {
            String url = inputArr[1];
            HttpResponse<JsonNode> response;
            if (inputArr.length > 2) {
                // If a JSON string was provided, include it in the request
                String jsonString = inputArr[2];
                response = Unirest.get(url).queryString("json", jsonString).asJson();
            } else {
                // If no JSON string was provided, run the request without it
                response = Unirest.get(url).asJson();
            }
            // Log the result
            System.out.println("Result: " + response.getBody());
        } catch (UnirestException e) {
            publishWarning("Manager Service", "Failed to run Get Request \"" + Arrays.toString(inputArr) +"\"");

        }
    }

    // Method to run a Unirest.post request
    static void runPostRequest(String[] inputArr) {
        try {
            String url = inputArr[1];
            String jsonString = inputArr[2];
            HttpResponse<JsonNode> response = Unirest.post(url).body(jsonString).asJson();
            // Log the result
            System.out.println("Result: " + response.getBody());
        } catch (UnirestException e) {
            publishWarning("Manager Service", "Failed to run Post Request \"" + Arrays.toString(inputArr) +"\"");
        }
    }

    public static void main(String[] args) {
        new Service<>(new ManagerService()).execute(args);
    }
}