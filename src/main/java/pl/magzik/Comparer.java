package pl.magzik;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public interface Comparer {
  
    // Comparer manipulation

    void _setUp(File dest, Collection<File> source) throws FileNotFoundException;
    default void _setUp(File dest, File...source) throws FileNotFoundException {
        _setUp(dest, Arrays.asList(source));
    }

    default void _reset() {
        // In case someone wants to create a one-time Comparer
    }
    
    // Data manipulation
  
    void map() throws IOException;
    void compare();
    void move() throws IOException;

    // utils

    default boolean filePredicate(File f) {
        // Returns always true.
        return true;
    }
    enum Modes {
        RECURSIVE, NOT_RECURSIVE;

        public static Modes get(String s) {
            return switch (s.toLowerCase()) {
                case "recursive" -> RECURSIVE;
                case "not recursive" -> NOT_RECURSIVE;
                default -> throw new NullPointerException();
            };
        }

        public static Modes get(int i) {
            if (i < 0 || i > 1)
                throw new NullPointerException();

            return i == 0 ? RECURSIVE : NOT_RECURSIVE;
        }
    }
}
