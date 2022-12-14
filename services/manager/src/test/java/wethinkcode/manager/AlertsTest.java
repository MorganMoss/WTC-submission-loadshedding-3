package wethinkcode.manager;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.junit.jupiter.api.*;
import wethinkcode.model.Schedule;
import wethinkcode.places.PlacesService;
import wethinkcode.schedule.ScheduleService;
import wethinkcode.service.Service;
import wethinkcode.stage.StageService;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
class AlertsTest {

    static final int TEST_PORT = 7000;
    static Service<ManagerService> SERVICE;

    @BeforeEach
    void startAllServices(){
        SERVICE = new Service<>(new ManagerService()).execute("-p="+TEST_PORT);
    }

    @AfterEach
    void closeAllServices(){
        SERVICE.stop();
    }

    @BeforeAll
    static void startListening(){
        AlertListener.start();
    }

    @AfterAll
    static void stopListening(){
        AlertListener.stop();
    }

    @Test
    void closePlaces() throws InterruptedException {
        int placesPort = SERVICE.instance.ports.keySet().stream().filter(key -> SERVICE.instance.ports.get(key).instance.getClass().getSimpleName().equals("PlacesService")).findFirst().get();
        Service<PlacesService> places = (Service<PlacesService>) SERVICE.instance.ports.get(placesPort);
        AlertListener.alerts.clear();
        places.stop();

        Thread.sleep(50);

        assertEquals("[PlacesService] [WARNING] Service stopped", AlertListener.alerts.remove());

        Unirest
                .get(scheduleUrl() + "/Eastern%20Cape/Gqeberha" )
                .asObject( Schedule.class ).getBody();

        Thread.sleep(50);

        assertEquals("[ScheduleService] [WARNING] Failed to invoke getSchedule", AlertListener.alerts.remove());
    }

    @Test
    void closeStage() throws InterruptedException {
        int stagePort = SERVICE.instance.ports.keySet().stream().filter(key -> SERVICE.instance.ports.get(key).instance.getClass().getSimpleName().equals("StageService")).findFirst().get();
        Service<StageService> stage = (Service<StageService>) SERVICE.instance.ports.get(stagePort);
        AlertListener.alerts.clear();
        stage.stop();

        Thread.sleep(50);

        assertEquals("[StageService] [WARNING] Service stopped", AlertListener.alerts.remove());
    }

    @Test
    void closeSchedule() throws InterruptedException {
        int schedulePort = SERVICE.instance.ports.keySet().stream().filter(key -> SERVICE.instance.ports.get(key).instance.getClass().getSimpleName().equals("ScheduleService")).findFirst().get();
        Service<ScheduleService> schedule = (Service<ScheduleService>) SERVICE.instance.ports.get(schedulePort);
        AlertListener.alerts.clear();
        schedule.stop();

        Thread.sleep(50);

        assertEquals("[ScheduleService] [WARNING] Service stopped", AlertListener.alerts.remove());

        assertThrows(UnirestException.class, () -> Unirest
                .get(scheduleUrl() + "/Eastern%20Cape/Gqeberha" )
                .asObject( Schedule.class ).getBody());

    }


    private String scheduleUrl() {
        return Unirest.get(SERVICE.url() + "/service/ScheduleService").asObject(String.class).getBody();
    }

}