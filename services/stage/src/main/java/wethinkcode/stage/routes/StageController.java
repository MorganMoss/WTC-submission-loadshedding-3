package wethinkcode.stage.routes;

import com.google.gson.JsonParseException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import wethinkcode.BadStageException;
import wethinkcode.model.Stage;
import wethinkcode.service.controllers.Controllers;
import wethinkcode.service.controllers.Verb;
import wethinkcode.stage.StageService;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import static java.lang.Math.round;

@Controllers.Controller("stage")
@SuppressWarnings("unused")
public class StageController{

    /**
     * Gets a place by name
     */
    @Controllers.Mapping(Verb.GET)
    static public void getStage(Context ctx, StageService instance){
        ctx.json(instance.stage.json());
        ctx.status(HttpStatus.OK);
    }

    static void setStage(int stageNumber, StageService instance) throws BadStageException {
        setStage(Stage.stageFromNumber(stageNumber), instance);
    }

    static public void setStage(Stage stageObj, StageService instance) throws NullPointerException{
        Objects.requireNonNull(stageObj);
        instance.stage = stageObj;
        instance.stageUpdates.add(instance.stage.toString());
    }

    static public void setStageLegacy(Context ctx, StageService instance){
        try {
            setStage((int) round((Double) ctx.bodyAsClass(HashMap.class).get("stage")), instance);
            ctx.status(HttpStatus.OK);
        } catch (NullPointerException | ClassCastException | BadStageException e) {
            ctx.json(Arrays.deepToString(e.getStackTrace()));
            ctx.status(HttpStatus.BAD_REQUEST);
        }
    }

    @Controllers.Mapping(Verb.POST)
    static public void setStage(Context ctx, StageService instance){
        try {
            setStage(ctx.bodyAsClass(Stage.class), instance);
            ctx.status(HttpStatus.OK);
        } catch (JsonParseException | NullPointerException notStageJSON) {
            setStageLegacy(ctx, instance);
        }
    }
}
