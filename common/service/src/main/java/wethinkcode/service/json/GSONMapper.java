package wethinkcode.service.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.logging.Logger;

import static wethinkcode.logger.Logger.formatted;

/**
 * GSON serializer as a JsonMapper
 */
public class GSONMapper implements JsonMapper {
    final GsonBuilder builder = new GsonBuilder();
    final Gson gson = builder.create();
    final Logger logger;

    public GSONMapper(String serviceName) {
        this.logger = formatted(this.getClass().getSimpleName() + " " + serviceName,
                "\u001B[38;5;40m", "\u001B[38;5;83m");
    }

    @NotNull
    @Override
    public String toJsonString(@NotNull Object obj, @NotNull Type type) {
        logger.info("To JSON: " + obj + " of type " + type.getTypeName());
        String result = gson.toJson(obj);
        logger.info("Result: " + result);
        return result;
    }

    @NotNull
    @Override
    public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
        logger.info("From JSON : " + json + " to type " + targetType.getTypeName());
        T result = gson.fromJson(json, targetType);
        logger.info("Result: " + result.toString());
        return result;
    }
}