package wethinkcode.manager;

import wethinkcode.places.PlacesService;
import wethinkcode.schedule.ScheduleService;
import wethinkcode.service.Service;
import wethinkcode.stage.StageService;
import wethinkcode.web.WebService;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

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

    public static void main(String[] args) {
        new Service<>(new ManagerService()).execute(args);
    }
}