package pl.magzik.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;

/**
 * A file visitor that processes files and directories during a file tree walk operation.
 * <p>
 * This class extends {@link SimpleFileVisitor} to traverse a file tree and processes each file asynchronously.
 * It validates files using a {@link FileValidator} and collects valid files in a concurrent set.
 * The processing is performed asynchronously using the provided {@link ExecutorService}.
 * </p>
 * <p>
 * The {@link #getFiles()} method waits for all asynchronous tasks to complete before returning the set of files.
 * </p>
 *
 * @see SimpleFileVisitor
 * @see FileValidator
 */
public class FileVisitor extends SimpleFileVisitor<Path> {
    private static final Logger logger = LoggerFactory.getLogger(FileVisitor.class);

    private final ExecutorService executorService;
    private final FileValidator fileValidator;
    private final Set<Path> files;
    private final Queue<CompletableFuture<Void>> futures;

    /**
     * Constructs a {@code FileVisitor} with the specified executor service and file validator.
     *
     * @param executorService the {@code ExecutorService} used for asynchronous file processing.
     * @param fileValidator   the {@code FileValidator} used to validate files.
     */
    public FileVisitor(ExecutorService executorService, FileValidator fileValidator) {
        this.executorService = executorService;
        this.fileValidator = fileValidator;
        this.files = new ConcurrentSkipListSet<>();
        this.futures = new ConcurrentLinkedQueue<>();
    }

    /**
     * Processes each file encountered during the file tree walk.
     * <p>
     * The file is processed asynchronously to validate it using the provided {@link FileValidator}.
     * If the file is a regular file and passes validation, it is added to the set of files.
     * </p>
     *
     * @param file the path of the file being visited.
     * @param attrs the file attributes of the file being visited.
     * @return {@code FileVisitResult.CONTINUE} to continue visiting files.
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        futures.offer(CompletableFuture.runAsync(() -> {
            try {
                if (Files.isRegularFile(file) && fileValidator.validate(file)) {
                    files.add(file);
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }, executorService));

        return FileVisitResult.CONTINUE;
    }

    /**
     * Handles failures when visiting a file.
     * <p>
     * This method logs the exception and continues with the file tree walk.
     * </p>
     *
     * @param file the path of the file that could not be visited.
     * @param exc the exception that was thrown.
     * @return {@code FileVisitResult.CONTINUE} to continue visiting files.
     */
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        logger.warn("Skipping file: {}, because of: {}", file, exc.getMessage());
        return FileVisitResult.CONTINUE;
    }

    /**
     * Returns the set of files collected during the file tree walk.
     * <p>
     * This method waits for all asynchronous tasks to complete before returning the result.
     * </p>
     *
     * @return a {@code Set<Path>} containing the paths of all valid files.
     */
    public Set<Path> getFiles() {
        futures.forEach(CompletableFuture::join); // Hold the current thread until processing is completed.
        return files;
    }
}
