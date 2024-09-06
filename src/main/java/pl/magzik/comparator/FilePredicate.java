package pl.magzik.comparator;

import java.io.File;
import java.io.IOException;

/**
 * Based on {@link java.util.function.Predicate} interface.
 * Represents predicate with one {@code File} argument.
 * This is a functional interface whose functional method is {@link #test(File)}
 * 
 */
@FunctionalInterface
public interface FilePredicate {
    /**
     * Evaluates this predicate on the given argument.
     *
     * @param file the input {@code File}
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}.
     * @throws IOException
     *         when I/O error occurs.
     */
    boolean test(File file) throws IOException;
}
