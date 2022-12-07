package wethinkcode.service.controllers;

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.Handler;

import java.util.function.BiConsumer;

public enum Verb {
    GET (ApiBuilder::get),
    POST (ApiBuilder::post);
    //TODO: Add the rest as needed
    private final BiConsumer<String, Handler> verb;
    public void invoke(String path, Handler handler){
        verb.accept(path, handler);
    }

    Verb(BiConsumer<String, Handler> verb){
        this.verb = verb;
    }
}
