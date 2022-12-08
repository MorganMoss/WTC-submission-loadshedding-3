package wethinkcode.stage;

import kong.unirest.json.JSONObject;
import org.eclipse.jetty.util.BlockingArrayQueue;
import wethinkcode.BadStageException;
import wethinkcode.model.Stage;
import wethinkcode.service.Service;
import wethinkcode.service.messages.Prefix;

import java.util.Queue;

import static wethinkcode.stage.routes.StageController.setStage;

/**
 * I provide a Stages Service_OLD for South Africa.
 */
@Service.AsService
public class StageService{
    public Stage stage;

    @Service.Publish(destination = "stage", prefix = Prefix.TOPIC)
    public Queue<String> stageUpdates = new BlockingArrayQueue<>();
    
    @Service.Listen(destination = "stage", prefix = Prefix.QUEUE)
    public void stageUpdater(String message){
        int stage = new JSONObject(message).getInt("stage");
        try {
            setStage(Stage.stageFromNumber(stage), this);
        } catch (BadStageException e) {
            e.printStackTrace();
        }
    }

    @Service.RunBefore
    public void customServiceInitialisation() {
        setStage(Stage.STAGE0, this);
    }


    public static void main(String ... args){
       new Service<>(new StageService()).execute(args);
    }
}