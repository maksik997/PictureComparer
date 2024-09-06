package pl.magzik.predicates;

import java.io.File;
import java.io.IOException;

/**
 * Represents a predicate (boolean-valued function) of a single {@code File} argument.
 * <p>
 * This functional interface is similar to {@link java.util.function.Predicate}, but with the
 * added capability of throwing {@link IOException}. It allows you to define conditions
 * or filters based on file attributes and perform validation checks that might involve I/O operations.
 * </p>
 * <p>
 * This interface's functional method is {@link #test(File)}.
 * </p>
 *
 * <pre>{@code
 * // Example usage of FilePredicate
 * FilePredicate filePredicate = file -> {
 *     // Check if the file is readable
 *     return file.canRead();
 * };
 *
 * File file = new File("path/to/file.txt");
 * try {
 *     boolean isReadable = filePredicate.test(file);
 *     if (isReadable) {
 *         System.out.println(file.getName() + " is readable.");
 *     } else {
 *         System.out.println(file.getName() + " is not readable.");
 *     }
 * } catch (IOException e) {
 *     System.err.println("An error occurred: " + e.getMessage());
 * }
 * }</pre>
 *
 * @see java.util.function.Predicate
 */
@FunctionalInterface
public interface FilePredicate {

    /**
     * Evaluates this predicate on the given {@code File}.
     *
     * @param file the input {@code File} to be tested.
     * @return {@code true} if the file matches the predicate; otherwise, {@code false}.
     * @throws IOException if an I/O error occurs while processing the file.
     */
    boolean test(File file) throws IOException;
}
