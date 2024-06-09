package pl.magzik.Comparator;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface FilePredicate {
    boolean test(File file) throws IOException;
}
