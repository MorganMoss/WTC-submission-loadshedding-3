package wethinkcode.manager;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wethinkcode.model.Schedule;
import wethinkcode.places.PlacesService;
import wethinkcode.service.Service;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
class AlertsTest {

    static final int TEST_PORT = 7000;
    static Service<ManagerService> SERVICE;
    static ArrayList<String> alerts;

    @BeforeEach
    void startAllServices(){
//        Logger.showInfo = false;
        AlertListener.start();
        SERVICE = new Service<>(new ManagerService()).execute("-p="+TEST_PORT);
    }

    @AfterEach
    void closeAllServices(){
        AlertListener.stop();
        SERVICE.stop();
    }

    @Test
    void closePlaces(){
        int placesPort = SERVICE.instance.ports.keySet().stream().filter(key -> SERVICE.instance.ports.get(key).instance.getClass().getSimpleName().equals("PlacesService")).findFirst().get();
        Service<PlacesService> places = (Service<PlacesService>) SERVICE.instance.ports.get(placesPort);
        places.stop();


        HttpResponse<Schedule> response = Unirest
                .get(scheduleUrl() + "/Eastern%20Cape/Gqeberha" )
                .asObject( Schedule.class );
//        System.out.println(response.getBody());


        System.out.println(AlertListener.alerts);
    }

    private String scheduleUrl() {
        return Unirest.get(SERVICE.url() + "/service/ScheduleService").asObject(String.class).getBody();
    }

}