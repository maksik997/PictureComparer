package pl.magzik.Utils;

import org.tinylog.Logger;

public interface LoggingInterface {

    default void log(String msg) {
        Logger.info(msg);
    }

    default void log(Exception ex, String msg) {
        Logger.error(ex, msg +  " Please refer to error.txt file.");
    }

    default void log(Exception ex) {
        Logger.error(ex);
    }

    static void staticLog(String msg) {
        Logger.info(msg);
    }

    static void staticLog(Exception ex, String msg) {
        Logger.error(ex, msg);
    }
}
