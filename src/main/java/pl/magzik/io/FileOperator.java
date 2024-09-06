package pl.magzik.io;

import pl.magzik.predicates.FilePredicate;
import pl.magzik.utils.LoggingInterface;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * {@code FileOperator} provides operations for managing files, including loading, moving, and deleting them.
 * It uses virtual threads for efficient asynchronous processing,
 * particularly suitable for I/O-bound operations.
 *
 * <p>The class performs operations in the following pipeline:</p>
 * <ol>
 *     <li><strong>Pre-validation:</strong> Validates the provided files to ensure they exist and can be accessed. If any file fails this validation, an {@link IOException} is thrown.</li>
 *     <li><strong>File validation:</strong> Validates individual files based on the provided predicate. Files that throw an {@link IOException} during validation are excluded from the result list.</li>
 *     <li><strong>Directory validation:</strong> Recursively processes directories up to the specified depth, extracting all files and validating them. Files that throw an {@link IOException} during extraction are excluded from the result list.</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * // Define a file predicate for validation (e.g., accept only image files).
 * FilePredicate filePredicate = new ImageFilePredicate();
 *
 * // Create an instance of FileOperator with a depth of 2 and using virtual threads.
 * FileOperator fileOperator = new FileOperator(filePredicate, 2);
 *
 * // Define a collection of files and directories to operate on.
 * Collection<File> files = Arrays.asList(new File("image1.jpg"), new File("directory1"));
 *
 * // Load files, which involves pre-validation, file validation, and directory validation.
 * try {
 *     List<File> validatedFiles = fileOperator.load(files);
 *     System.out.println("Validated files: " + validatedFiles);
 *
 *     // Define a destination directory for moving files.
 *     File destination = new File("destination_directory");
 *
 *     // Move validated files to the destination directory.
 *     fileOperator.move(destination, validatedFiles);
 *
 *     // Delete the files if needed.
 *     fileOperator.delete(validatedFiles);
 * } catch (IOException e) {
 *     e.printStackTrace();
 * }
 * }
 * </pre>
 *
 * <p>Note:</p>
 * <ul>
 *     <li>Make sure to handle exceptions appropriately when performing operations.</li>
 *     <li>Consider the impact of concurrent file operations on system performance and ensure that your use case is suitable for virtual threading.</li>
 * </ul>
 */
public class FileOperator implements FileOperation, LoggingInterface {

    private final FileValidator fileValidator;
    private int depth;
    private final ExecutorService executorService;

    /**
     * Constructs a {@code FileOperator} with the specified file predicate,
     * depth for directory traversal, and custom executor service.
     *
     * @param filePredicate the {@code FilePredicate} to be used for validating files.
     * @param depth the depth for directory traversal.
     * @param executorService the {@code ExecutorService} to use it for asynchronous tasks.
     */
    public FileOperator(FilePredicate filePredicate, int depth, ExecutorService executorService) {
        this.fileValidator = new FileValidator(filePredicate);
        this.depth = depth;
        this.executorService = executorService;
    }

    /**
     * Constructs a {@code FileOperator} with the specified file predicate,
     * depth for directory traversal, and a default virtual thread executor.
     *
     * @param filePredicate the {@code FilePredicate} to be used for validating files.
     * @param depth the depth for directory traversal.
     */
    public FileOperator(FilePredicate filePredicate, int depth) {
        this(filePredicate, depth, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Sets the depth for directory traversal.
     *
     * @param depth the depth to set.
     * @throws IllegalArgumentException if the depth is negative.
     */
    @SuppressWarnings("unused")
    public void setDepth(int depth) {
        if (depth < 0) throw new IllegalArgumentException("Depth must not be negative");

        this.depth = depth;
    }

    @Override
    public List<File> load(Collection<File> files) throws IOException {
        Objects.requireNonNull(files, "files must not be null");
        log("Loading input sources...");
        log("Pre-validating input sources...");
        fileValidator.preValidate(files);
        log("Sources pre-validated.");

        log("Regular file validation...");
        List<File> out = handleRegularFiles(files);

        log("Directory validation...");

        out = Stream.concat(out.stream(), handleDirectories(files).stream())
                .distinct()
                .toList();

        log("Input files validated.");

        return out;
    }

    /**
     * Handles validation and processing of regular files.
     * <p>
     * This method filters the provided collection of files to include only regular files (not directories),
     * and processes each file asynchronously using the configured {@link ExecutorService}.
     * Each file is validated using the {@link FileValidator}. If a file is valid, it is included in the result list;
     * otherwise, it is ignored. Any {@link IOException} encountered during validation is logged.
     * </p>
     *
     * @param files a {@code Collection<File>} of files to be processed. Must not be {@code null}.
     * @return a {@code List<File>} containing the files that passed validation.
     * @throws NullPointerException if {@code files} is {@code null}.
     */
    private List<File> handleRegularFiles(Collection<File> files) {
        Objects.requireNonNull(files, "files must not be null");

        List<CompletableFuture<File>> futures = files.stream()
                .filter(File::isFile)
                .map(f -> CompletableFuture.supplyAsync(() -> {
                    try {
                        if (fileValidator.validate(f))
                            return f;
                    } catch (IOException e) {
                        log(e);
                    }
                    return null;
                }, executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Handles validation and processing of directories.
     * <p>
     * This method filters the provided collection of files to include only directories,
     * and processes each directory asynchronously using the configured {@link ExecutorService}.
     * It recursively walks through each directory up to the specified depth, extracting all files and validating them.
     * Any {@link IOException} encountered during directory traversal or file extraction is logged.
     * </p>
     *
     * @param files a {@code Collection<File>} of files to be processed. Must not be {@code null}.
     * @return a {@code List<File>} containing all files extracted from the validated directories.
     * @throws NullPointerException if {@code files} is {@code null}.
     */
    private List<File> handleDirectories(Collection<File> files) {
        Objects.requireNonNull(files, "files must not be null");

        FileVisitor fv = new FileVisitor(executorService, fileValidator);
        List<CompletableFuture<Void>> futures = files.stream()
                .filter(File::isDirectory)
                .map(File::toPath)
                .map(d -> CompletableFuture.runAsync(() -> {
                    try {
                        Files.walkFileTree(d, new HashSet<>(), depth, fv);
                    } catch (IOException e) {
                        log(e);
                    }
                }, executorService))
                .toList();

        futures.forEach(CompletableFuture::join);

        return List.copyOf(fv.getFiles()).stream()
                .map(Path::toFile)
                .toList();
    }

    @Override
    public void move(File destination, Collection<File> files) throws IOException {
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(files, "files must not be null");

        if (files.isEmpty()) return;

        try {
            files.forEach(f -> CompletableFuture.runAsync(() -> {
                try {
                    Files.move(
                        f.toPath(),
                        Path.of(String.valueOf(destination), f.getName()),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException e) {
                    log(e);
                    throw new UncheckedIOException(e);
                }
            }));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    public void delete(Collection<File> files) throws IOException {
        Objects.requireNonNull(files, "files must not be null");

        if (files.isEmpty()) return;

        try {
            files.stream()
                .map(File::toPath)
                .forEach(f -> CompletableFuture.runAsync(() -> {
                    try {
                        Files.delete(f);
                    } catch (IOException e) {
                        log(e);
                        throw new UncheckedIOException(e);
                    }
                }));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
