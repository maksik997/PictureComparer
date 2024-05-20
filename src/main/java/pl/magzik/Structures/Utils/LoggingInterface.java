package pl.magzik.Structures.Utils;

import org.tinylog.Logger;

public interface LoggingInterface {

    default void log(String msg) {
        Logger.info(msg);
    }

    default void log(Exception ex, String msg) {
        Logger.error(ex, msg);
    }
}
