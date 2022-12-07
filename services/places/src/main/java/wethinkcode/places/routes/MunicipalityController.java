package wethinkcode.places.routes;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import wethinkcode.model.Municipality;

import java.util.List;
import java.util.Optional;

import wethinkcode.places.PlacesService;
import wethinkcode.service.controllers.Controllers;
import wethinkcode.service.controllers.Verb;

@Controllers.Controller("")
@SuppressWarnings("unused")
public class MunicipalityController{

    /**
     * Gets a municipality by name
     */
    @Controllers.Mapping(value = Verb.GET, path = "municipality/{name}")
    public static void getMunicipality(Context ctx, PlacesService instance){
        String name = ctx.pathParam("name");
        Optional<Municipality> municipality = instance.places.municipality(name);

        if (municipality.isPresent()){
            ctx.json(municipality.get());
            ctx.status(HttpStatus.OK);
        } else {
            ctx.status(HttpStatus.NOT_FOUND);
        }
    }

    @Controllers.Mapping(value = Verb.GET, path = "municipalities/{province}")
    public static void getMunicipalitiesInProvince(Context ctx, PlacesService instance){
        String province = ctx.pathParam("province");
        List<Municipality> municipalities = instance.places.municipalitiesIn(province);

        if (municipalities.size()>0){
            ctx.json(municipalities);
            ctx.status(HttpStatus.OK);
        } else {
            ctx.status(HttpStatus.NOT_FOUND);
        }
    }
}
