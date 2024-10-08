package pl.magzik.utils;

import org.tinylog.Logger;

/**
 * Interface that implements few log methods using {@code Tinylog} library.
 */
public interface LoggingInterface {

    /**
     * Saves log message.
     * @param msg
     *        Exact message to be printed in the log.
     */
    default void log(String msg) {
        Logger.info(msg);
    }

    /**
     * Saves a log message with exception.
     * @param ex
     *        Exception to be printed in the log.
     */
    default void log(Exception ex) {
        Logger.error(ex.getLocalizedMessage(), ex);
    }
}
