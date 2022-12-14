package wethinkcode.logger;

import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class Logger {
    public static java.util.logging.Logger formatted(String name, String info_colour, String message_colour){
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
        changeFormat(logger, info_colour, message_colour);
        return logger;
    }

    public static boolean showInfo = true;

    public static void changeFormat(java.util.logging.Logger logger, String info_colour, String message_colour){
        logger.setUseParentHandlers(false);
        Arrays.stream(logger.getHandlers()).forEach(logger::removeHandler);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            public synchronized String format(LogRecord lr) {
                String colour = info_colour;
                String messageColour = message_colour;
                if (lr.getLevel().equals(Level.SEVERE)) {
                    colour = "\u001B[38;5;196m";
                    messageColour = "\u001B[38;5;202m";
                }
                if (lr.getLevel().equals(Level.WARNING)) {
                    colour = "\u001B[38;5;226m";
                    messageColour = "\u001B[38;5;229m";
                }
                if (!showInfo && lr.getLevel().equals(Level.INFO)){
                    return "";
                }
                return colour + "[" + lr.getLoggerName() + "] " + lr.getLevel().getLocalizedName() + messageColour + " " + lr.getMessage() + "\n";
            }
        });

        logger.addHandler(handler);

    }
}
