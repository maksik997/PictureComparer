package pl.magzik.io;

import pl.magzik.predicates.FilePredicate;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * A utility class for validating files based on a given {@link FilePredicate}.
 * <p>
 * This class provides methods for pre-validating a collection of files to ensure they exist
 * and for validating individual files or paths based on the provided predicate.
 * </p>
 *
 * @see FilePredicate
 */
public class FileValidator {

    private final FilePredicate predicate;

    /**
     * Constructs a {@code FileValidator} with the specified {@link FilePredicate}.
     *
     * @param predicate the {@code FilePredicate} used to validate files.
     */
    public FileValidator(FilePredicate predicate) {
        this.predicate = predicate;
    }

    /**
     * Pre-validates the given collection of files to ensure they exist.
     * <p>
     * If any file in the collection does not exist, an {@link IOException} is thrown.
     * The exception contains the path of the missing file.
     * </p>
     *
     * @param files a {@code Collection<File>} representing the files to be pre-validated.
     * @throws IOException if an I/O error occurs, specifically if any file does not exist.
     */
    public void preValidate(Collection<File> files) throws IOException {
        try {
            files.stream()
                .filter(f -> !f.exists())
                .findAny()
                .ifPresent(f -> {
                    throw new UncheckedIOException(new IOException(
                        "Couldn't find a file: " + f.getAbsolutePath()
                    ));
                });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Validates an individual file based on the provided {@link FilePredicate}.
     * <p>
     * The file is considered valid if it is a regular file and it satisfies the predicate.
     * </p>
     *
     * @param file the {@code File} to be validated.
     * @return {@code true} if the file is valid, according to the predicate; {@code false} otherwise.
     * @throws IOException if an I/O error occurs while accessing the file.
     */
    public boolean validate(File file) throws IOException {
        return file.isFile() && predicate.test(file);
    }

    /**
     * Validates an individual file represented by the given path.
     * <p>
     * This method converts the {@code Path} to a {@code File} and delegates to {@link #validate(File)}.
     * </p>
     *
     * @param path the {@code Path} representing the file to be validated.
     * @return {@code true} if the file is valid, according to the predicate; {@code false} otherwise.
     * @throws IOException if an I/O error occurs while accessing the file.
     */
    public boolean validate(Path path) throws IOException {
        return validate(path.toFile());
    }
}
