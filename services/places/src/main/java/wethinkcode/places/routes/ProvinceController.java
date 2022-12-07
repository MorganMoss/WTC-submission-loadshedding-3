package wethinkcode.places.routes;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import wethinkcode.model.Province;

import java.util.Collection;


import wethinkcode.places.PlacesService;
import wethinkcode.service.controllers.Controllers;
import wethinkcode.service.controllers.Verb;

@Controllers.Controller("provinces")
@SuppressWarnings("unused")
public class ProvinceController {
    /**
     * Gets a municipality by name
     */
    @Controllers.Mapping(Verb.GET)
    public static void getAllProvinces(Context ctx, PlacesService instance){
        Collection<Province> provinces = instance.places.provinces();

            ctx.json(provinces);
            ctx.status(HttpStatus.OK);
    }
}
