package wethinkcode.service.controllers;


import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import org.reflections.Reflections;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.reflections.scanners.Scanners.TypesAnnotated;
import static wethinkcode.logger.Logger.formatted;

public class Controllers {

    private final Object instance;
    private final Set<EndpointGroup> endpoints = new HashSet<>();

    private final Logger logger;

    public Controllers(Object instance) {
        this.instance = instance;
        logger = formatted(
            this.getClass().getSimpleName()
                    + " "
                    + instance.getClass().getSimpleName(),
                "\u001B[38;5;39m", "\u001B[38;5;45m");
    }

    public Set<EndpointGroup> getEndpoints(){
        Set<Class<?>> controllers = findControllers();
        if (controllers.size() == 0){
            logger.info("No Controllers were found for this service");
        } else {
            collectEndpoints(controllers);
        }

        return endpoints;
    }

    private Stream<Method> findMappings(Class<?> clazz){
        String path = clazz.getAnnotation(Controller.class).value();
        logger.info("Adding endpoints for '"+ path +"' from the class " + clazz.getSimpleName());
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Mapping.class));
        }
    private void addEndpoints(Class<?> clazz){
        String path = clazz.getAnnotation(Controller.class).value();
        endpoints.add(() -> ApiBuilder.path(path, () -> findMappings(clazz).forEach(this::runMethod)));
        findEndpoints(clazz).forEach(endpoints::add);
    }

    private Stream<EndpointGroup> findEndpoints(Class<?> clazz) {
//        logger.info("Getting Endpoint Groups");
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Endpoint.class))
                .map(method -> {
                    try {
                        return (EndpointGroup) method.invoke(instance, instance);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void runMethod(Method method) {
        Mapping annotation = method.getAnnotation(Mapping.class);
        String path = annotation.path();
        Verb verb = annotation.value();
        logger.info(verb.name()+ (path.equals("") ? "":" for '" + path + "'") + " from the method " + method.getName());
        verb.invoke(path, (ctx) -> {
            logger.info("Invoking " + method.getName());
            method.invoke(instance, ctx, instance);
        });
    }

    private Set<Class<?>> findControllers(){
        logger.info("Searching for Controller annotated classes...");
        Reflections reflections = new Reflections(instance.getClass().getPackageName());
        return reflections.get(TypesAnnotated.with(Controller.class).asClass());
    }

    private void collectEndpoints(Set<Class<?>> classes){
        logger.info("Collecting Endpoints from " + classes);
        classes.forEach(this::addEndpoints);
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Controller {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Mapping {
        Verb value();
        String path() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Endpoint{ }
}

