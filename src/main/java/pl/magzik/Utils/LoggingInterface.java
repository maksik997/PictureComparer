package pl.magzik.Utils;

import org.tinylog.Logger;

public interface LoggingInterface {

    default void log(String msg) {
        Logger.info(msg);
    }

    default void log(Exception ex, String msg) {
        Logger.error(ex, msg);
    }

    default <E extends Exception> void logAndThrow(E ex, String msg) throws E {
        log(ex, msg);
        throw ex;
    }

    default <E extends RuntimeException> void logAndThrow(E ex, String msg) {
        log(ex, msg);
        throw ex;
    }
}
