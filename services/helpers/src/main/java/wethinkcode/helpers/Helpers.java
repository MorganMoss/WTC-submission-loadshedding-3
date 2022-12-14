package wethinkcode.helpers;

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.javalin.apibuilder.ApiBuilder.path;

public final class Helpers {

    /**
     * Gets the URL of a specific service using an API call to the manager
     * @param from the service you're looking for
     * @return the URL of that server.
     */
    public static String getURL(String from, String managerURL){
        try {
            String URL = Unirest.get(managerURL + "/service/" + from).asString().getBody();
            return URL.replace("\"", "");
        } catch (UnirestException e){
            throw new RuntimeException(e);
        }
    }


    /**
     * Takes a route, assuming the same names for path parameters,
     * fills the parameters with context values and fetches that new request
     * and passes it on to the context
     * @param thisURL for this service
     * @param theirURL for the other service being forwarded
     * @param request the type of request function, i.e. get, post etc.
     *                Pass the Javalin ApiBuilder methods into here
     */
    public static void forward(String thisURL, String theirURL, Consumer<Handler> request){
        path(thisURL , () -> request.accept((context) -> {
            AtomicReference<String> newURL = new AtomicReference<>(theirURL);
            Arrays
                    .stream(thisURL.split("/"))
                    .filter(s -> s.charAt(0) == '{' && s.charAt(s.length()-1) == '}')
                    .forEach(s -> newURL.set(newURL.get().replace(
                            s,
                            context.pathParam(s
                                    .replace("{","")
                                    .replace("}","")
                            ))));

            forwardRoute(context, newURL.get());
        }));
    }

    /**
     * Takes a route, assuming the same names for path parameters,
     * fills the parameters with context values and fetches that new request
     * and passes it on to the context.
     * Specifying no Consumer defaults to get()
     * @param thisURL for this service
     * @param theirURL for the other service being forwarded
     */
    public static void forward(String thisURL, String theirURL) {
        forward(thisURL, theirURL, ApiBuilder::get);
    }

    public static void forwardRoute(Context context, String route){
        GetRequest request = Unirest.get(route);
        HttpResponse<String> response = request.asString();
        context.result(response.getBody());
        context.status(response.getStatus());
    }
}
