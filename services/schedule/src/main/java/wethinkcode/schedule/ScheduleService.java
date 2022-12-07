package wethinkcode.schedule;

import io.javalin.json.JavalinJackson;
import io.javalin.json.JsonMapper;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import picocli.CommandLine;
import wethinkcode.schedule.transfer.ScheduleDAO;
import wethinkcode.service.Service;
import wethinkcode.service.messages.Prefix;

/**
 * I provide a REST API providing the current loadshedding schedule for a
 * given town (in a specific province) at a given loadshedding stage.
 */
@Service.AsService
public class ScheduleService{
    @CommandLine.Option(
            names = {"-m", "--manager"},
            description = {"The URL of the manager service."}
    )
    String manager;

    @CommandLine.Option(
            names = {"-pl", "--places"},
            description = {"The URL of the places service."}
    )
    String places;

    /**
     * Is updated by a listener
     * Contains the current stage number
     */
    int stage;

    /**
     * Handles the getting of schedules
     */
    public final ScheduleDAO scheduleDAO = new ScheduleDAO();

    @Service.RunBefore
    public void setPlaceURL(){
        scheduleDAO.setPlacesURL((manager == null)? places :
        Unirest.get(manager + "/service/PlacesService").asObject(String.class).getBody());
    }

    /**
     * This service uses Jackson over the default GSON
     */
    @Service.CustomJSONMapper
    public JsonMapper createJsonMapper() {
        return new JavalinJackson();
    }

    @Service.Listen(prefix = Prefix.TOPIC, destination = "stage")
    public void stageListener(String message){
        stage = new JSONObject(message).getInt("stage");
    }

    public int getStage() {
        return stage;
    }

    public static void main(String[] args ) {
        new Service<>(new ScheduleService()).execute(args);
    }
}
