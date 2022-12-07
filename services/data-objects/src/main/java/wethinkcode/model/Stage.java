package wethinkcode.model;

import com.google.gson.JsonObject;
import wethinkcode.BadStageException;

public enum Stage {
    STAGE0 ("No Loadshedding", 0),
    STAGE1 ("Stage 1 Loadshedding", 1),
    STAGE2 ("Stage 2 Loadshedding", 2),
    STAGE3 ("Stage 3 Loadshedding", 3),
    STAGE4 ("Stage 4 Loadshedding", 4),
    STAGE5 ("Stage 5 Loadshedding", 5),
    STAGE6 ("Stage 6 Loadshedding", 6),
    STAGE7 ("Stage 7 Loadshedding", 7),
    STAGE8 ("No Electricity", 8);

    public final String description;
    public final int stage;
    Stage(String description, int stage) {
        this.description = description;
        this.stage = stage;
    }

    public static Stage stageFromNumber(int stage) throws BadStageException {
        switch (stage){
            case 0 -> {return STAGE0;}
            case 1 -> {return STAGE1;}
            case 2 -> {return STAGE2;}
            case 3 -> {return STAGE3;}
            case 4 -> {return STAGE4;}
            case 5 -> {return STAGE5;}
            case 6 -> {return STAGE6;}
            case 7 -> {return STAGE7;}
            case 8 -> {return STAGE8;}
            default -> throw new BadStageException(stage + " is an invalid stage");

        }
    }

    @Override
    public String toString() {
        return "{ \"stage\": " + this.stage + ", \"description\": \"" + this.description + "\" }";
    }

    public JsonObject json(){
        JsonObject j = new JsonObject();
        j.addProperty("stage", this.stage);
        j.addProperty("description", this.description);
        return j;
    }
}
