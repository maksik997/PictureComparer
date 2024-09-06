package pl.magzik.utils;

import org.tinylog.Logger;

/**
 * Interface that implements few log methods using {@code Tinylog} library.
 */
public interface LoggingInterface {

    /**
     * Saves log message.
     * @param msg
     *        Exact message to be printed in log.
     */
    default void log(String msg) {
        Logger.info(msg);
    }

    /**
     * Saves log message with exception.
     * @param msg
     *        Exact message to be printed in log.
     * @param ex
     *        Exception to be printed in log.
     */
    default void log(Exception ex, String msg) {
        Logger.error(ex, msg +  " Please refer to error.txt file.");
    }

    /**
     * Saves log message with exception.
     * @param ex
     *        Exception to be printed in log.
     */
    default void log(Exception ex) {
        Logger.error(ex.getLocalizedMessage(), ex);
    }

    /**
     * Static method.
     * <p>
     * Saves log message with exception.
     * @param msg
     *        Exact message to be printed in log.
     */
    @Deprecated
    static void staticLog(String msg) {
        Logger.info(msg);
    }

    /**
     * Static method.
     * <p>
     * Saves log message with exception.
     * @param msg
     *        Exact message to be printed in log.
     * @param ex
     *        Exception to be printed in log.
     */
    @Deprecated
    static void staticLog(Exception ex, String msg) {
        Logger.error(ex, msg);
    }
}
