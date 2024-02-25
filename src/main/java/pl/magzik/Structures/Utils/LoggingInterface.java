package pl.magzik.Structures.Utils;

public interface LoggingInterface {

    void log(String msg);

    void log(Exception ex, String msg);
}
