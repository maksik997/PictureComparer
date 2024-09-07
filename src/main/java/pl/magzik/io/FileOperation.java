package pl.magzik.io;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Interface providing operations for file management, such as loading, moving, and deleting files.
 * It allows operations on both collections of files and individual files.
 */
public interface FileOperation {

    /**
     * Loads the given collection of files.
     *
     * @param files
     *        A {@code Collection<File>} representing the files to be loaded.
     * @return {@code List<File>} containing the loaded files.
     * @throws IOException
     *         If an I/O error occurs while loading the files.
     */
    List<File> load(Collection<File> files) throws IOException;

    /**
     * Loads the given array of files. This is a default method that delegates
     * the operation to {@link #load(Collection)}.
     *
     * @param files
     *        An array of {@code File} objects to be loaded.
     * @return {@code List<File>} containing the loaded files.
     * @throws IOException
     *         If an I/O error occurs while loading the files.
     */
    default List<File> load(File... files) throws IOException {
        return load(Arrays.asList(files));
    }

    /**
     * Moves the given collection of files to the specified destination directory.
     *
     * @param destination
     *        The destination {@code File} (directory) where the files will be moved.
     * @param files
     *        A {@code Collection<File>} representing the files to be moved.
     * @throws IOException
     *         If an I/O error occurs during the move operation.
     */
    void move(File destination, Collection<File> files) throws IOException;

    /**
     * Moves the given array of files to the specified destination directory.
     * This is a default method that delegates the operation to {@link #move(File, Collection)}.
     *
     * @param destination
     *        The destination {@code File} (directory) where the files will be moved.
     * @param files
     *        An array of {@code File} objects to be moved.
     * @throws IOException
     *         If an I/O error occurs during the move operation.
     */
    @SuppressWarnings("unused")
    default void move(File destination, File... files) throws IOException {
        move(destination, Arrays.asList(files));
    }

    /**
     * Deletes the given collection of files.
     *
     * @param files
     *        A {@code Collection<File>} representing the files to be deleted.
     * @throws IOException
     *         If an I/O error occurs while deleting the files.
     */
    void delete(Collection<File> files) throws IOException;

    /**
     * Deletes the given array of files. This is a default method that delegates
     * the operation to {@link #delete(Collection)}.
     *
     * @param files
     *        An array of {@code File} objects to be deleted.
     * @throws IOException
     *         If an I/O error occurs while deleting the files.
     */
    @SuppressWarnings("unused")
    default void delete(File... files) throws IOException {
        delete(Arrays.asList(files));
    }

}
