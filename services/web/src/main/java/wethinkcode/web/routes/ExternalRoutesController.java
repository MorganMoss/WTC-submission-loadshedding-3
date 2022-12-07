package wethinkcode.web.routes;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import wethinkcode.service.controllers.Controllers;
import wethinkcode.service.controllers.Verb;
import wethinkcode.web.WebService;


import static wethinkcode.helpers.Helpers.getURL;
import static wethinkcode.web.WebService.manager;


@Controllers.Controller("url")
@SuppressWarnings("unused")
public class ExternalRoutesController{
    private static void sendURLToContext(Context ctx, String url){
        ctx.json(url);
        ctx.status(HttpStatus.OK);
    }

    @Controllers.Mapping(value = Verb.GET, path = "stage")
    public static void stageURL(Context ctx, WebService instance) {
        sendURLToContext(ctx,getURL("StageService", manager));
    }

    @Controllers.Mapping(value = Verb.GET, path = "schedule")
    public static void scheduleURL(Context ctx, WebService instance) {
        sendURLToContext(ctx,getURL("ScheduleService", manager));
    }

    @Controllers.Mapping(value = Verb.GET, path = "places")
    public static void placesURL(Context ctx, WebService instance) {
        sendURLToContext(ctx,getURL("PlacesService", manager));
    }
}
