package wethinkcode.places.routes;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import wethinkcode.model.Place;

import java.util.List;
import java.util.Optional;


import wethinkcode.places.PlacesService;
import wethinkcode.service.controllers.Controllers;
import wethinkcode.service.controllers.Verb;

@Controllers.Controller("")
@SuppressWarnings("unused")
public class PlaceController{

    /**
     * Gets a place by name
     */
    @Controllers.Mapping(value = Verb.GET, path = "place/{name}")
    public static void getPlace(Context ctx, PlacesService instance){
        String name = ctx.pathParam("name");
        Optional<Place> place = instance.places.place(name);

        if (place.isPresent()){
            ctx.json(place.get());
            ctx.status(HttpStatus.FOUND);
        } else {
            ctx.status(HttpStatus.NOT_FOUND);
        }
    }

    @Controllers.Mapping(value = Verb.GET, path = "places/province/{province}")
    public static void getPlacesInProvince(Context ctx, PlacesService instance){
        String province = ctx.pathParam("province");
        List<Place> placeList = instance.places.placesInProvince(province);

        if (placeList.size()>0){
            ctx.json(placeList);
            ctx.status(HttpStatus.FOUND);
        } else {
            ctx.status(HttpStatus.NOT_FOUND);
        }
    }

    @Controllers.Mapping(value = Verb.GET, path = "places/municipality/{municipality}")
    public static void getPlacesInMunicipality(Context ctx, PlacesService instance){
        String municipality = ctx.pathParam("municipality");
        List<Place> placesList = instance.places.placesInMunicipality(municipality);

        if (placesList.size()>0){
            ctx.json(placesList);
            ctx.status(HttpStatus.FOUND);
        } else {
            ctx.status(HttpStatus.NOT_FOUND);
        }
    }

    @Controllers.Mapping(value = Verb.GET, path = "exists/{province}/{place}")
    public static void placeExists(Context context, PlacesService instance) {
        String province = context.pathParam("province");
        if (instance.places
                .provinces()
                .stream()
                .noneMatch(p -> p.name().equals(province))
        ) {
            context.status(HttpStatus.NOT_FOUND);
            context.json("Province does not exist: " + province);
            return;
        }

        String place = context.pathParam("place");

        if (instance.places
                .placesInProvince(province)
                .stream()
                .noneMatch(p -> p.name().equals(place))
        ) {
            context.status(HttpStatus.NOT_FOUND);
            context.json("Place does not exist in province: " + province);
            return;
        }

        context.status(HttpStatus.FOUND);


    }
}
