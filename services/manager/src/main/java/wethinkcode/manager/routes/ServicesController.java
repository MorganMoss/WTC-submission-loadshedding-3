package wethinkcode.manager.routes;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import wethinkcode.manager.ManagerService;
import wethinkcode.service.controllers.Controllers;
import wethinkcode.service.controllers.Verb;
import wethinkcode.service.Service;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Controllers.Controller("service")
@SuppressWarnings("unused")
public class ServicesController {
    @Controllers.Mapping(value = Verb.GET, path = "{name}")
    public static void getURL(Context context, ManagerService instance) {
        String name = Objects.requireNonNull(context.pathParam("name"));

        Optional<String> url = findURL(name, instance.ports);

        if (url.isPresent()) {
            context.json(url.get());
            context.status(HttpStatus.OK);
            return;
        }

        context.status(HttpStatus.NOT_FOUND);
    }

    public static Optional<String> findURL(String name, HashMap<Integer, Service<?>> ports){
        AtomicReference<Optional<String>> url = new AtomicReference<>(Optional.empty());
        ports
                .keySet()
                .stream()
                .filter((port) -> ports.get(port).instance.getClass().getSimpleName().equals(name))
                .findFirst().ifPresent(port -> url.set(Optional.of(ports.get(port).url())));

        return url.get();
    }
}


