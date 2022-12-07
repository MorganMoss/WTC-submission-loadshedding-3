package wethinkcode.web;

import io.javalin.config.JavalinConfig;
import picocli.CommandLine;
import wethinkcode.service.Service;

/**
 * I am the front-end web server for the LightSched project.
 * <p>
 * Remember that we're not terribly interested in the web front-end part of this
 * server, more in the way it communicates and interacts with the back-end
 * services.
 */
@Service.AsService
public class WebService{
    @CommandLine.Option(
            names = {"-m", "--manager"},
            description = {"The URL of the manager service."}
    )
    public static String manager;

    @Service.CustomJavalinConfig
    public void customJavalinConfig(JavalinConfig config) {
        config.staticFiles.add("/public");
    }

    public static void main( String[] args){
        new Service<>(new WebService()).execute(args);
    }
}
