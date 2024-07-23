package pl.magzik.IO;

import pl.magzik.Comparator.FilePredicate;
import pl.magzik.Utils.LoggingInterface;
import pl.magzik.Utils.UncheckedInterruptedException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

public class FileOperator implements LoggingInterface {

    private static final String FILE_SEPARATOR = File.separator;

    private final ExecutorService virtualExecutor;

    /**
     * Construct an instance of this class.
     * */
    public FileOperator() {
        virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * This method will load all files that are in given collection
     *  (if depth is greater than 1 all files from given directories will be loaded).
     * @param depth
     *        An {@code Integer} that indicates the depth of recursive file tree walk.
     * @param fp
     *        The {@link FilePredicate} used for validating files.
     * @param source
     *        Files separated by commas to be validated.
     * @return {@code List<File>}
     *         An validated files list.
     * @throws IOException
     *         When {@link FilePredicate} throws it.
     * @throws InterruptedException
     *         When thread is interrupted.
     */
    public List<File> loadFiles(int depth, FilePredicate fp, File... source) throws IOException, InterruptedException {
        return loadFiles(depth, fp, Arrays.asList(source));
    }

    /**
     * This method will load all files that are in given collection
     *  (if depth is greater than 1 all files from given directories will be loaded).
     * @param depth
     *        An {@code Integer} that indicates the depth of recursive file tree walk.
     * @param fp
     *        The {@link FilePredicate} used for validating files.
     * @param source
     *        The {@code Collection<File>} that represents input file collection to be loaded.
     * @return {@code List<File>}
     *         An validated files list.
     * @throws IOException
     *         When {@link FilePredicate} throws it.
     * @throws InterruptedException
     *         When thread is interrupted.
     */
    public List<File> loadFiles(int depth, FilePredicate fp, Collection<File> source) throws IOException, InterruptedException {
        // This method will load files (either from file path or directory path (in depth directories)),
        // then, a method will check if given files fulfil given FilePredicate.
        // Due to recreating ExecutorService, I advise to use it for larger collections of files more rarely (to avoid any memory leaks etc.)
        log("Loading files from " + source);

        log("Validating files");
        Objects.requireNonNull(source, "Depth is null.");

        try {
            source.parallelStream()
                .filter(f -> !f.exists())
                .findAny()
                .ifPresent(f -> {
                    throw new UncheckedIOException(new IOException("Could not find a file: " + f));
                });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        log("Files validated.");

        List<File> output;
        log("Collecting from files");
        try {
            output = new LinkedList<>(source.parallelStream()
                .filter(File::isFile)
                .filter(f -> {
                    try {
                        return fp.test(f);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .toList());
        } catch (UncheckedIOException e) {
            log(e);
            throw e.getCause();
        }

        log("Directory files collection.");
        List<Future<File>> filesTasks = Collections.synchronizedList(new LinkedList<>());
        try {
            source.parallelStream()
                .filter(File::isDirectory)
                .map(File::toPath)
                .forEach(p -> {
                    try {
                        Files.walkFileTree(p, EnumSet.of(FileVisitOption.FOLLOW_LINKS), depth, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                if (Files.isRegularFile(file)) {
                                    filesTasks.add(virtualExecutor.submit(
                                        () -> validatePath(fp, file, output)
                                    ));
                                }

                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                log("Skipping: " + file + ", because of: " + exc);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not walk a path: " + p, e);
                    }
            });
        } catch (UncheckedIOException e) {
            log(e);
            throw e.getCause();
        }

        try {
            filesTasks.forEach(future -> {
                try {
                    File f = future.get();
                    if (f != null) output.add(f);
                } catch (ExecutionException e) {
                    log(e, "Error while validating the file. Skipping...");
                } catch (InterruptedException e) {
                    throw new UncheckedInterruptedException(e);
                }
            });
        } catch (UncheckedInterruptedException e) {
            Thread.currentThread().interrupt();
            log(e);
            throw e.getCause();
        }

        log("Files collected.");
        return output;
    }


    /**
     * This method validates path (take path and check if it's a file and performs {@link FilePredicate} and haven't been processed yet.
     * @param fp
     *        The {@link FilePredicate}.
     * @param p
     *        The {@code Path} to be validated.
     * @param output
     *        The {@code List<File>} to indicate processed files.
     * @throws IOException
     *         If the {@link FilePredicate} throws it.
     *
     * @return {@code null}
     *         If the {@code Path} isn't valid.
     *         <p>
     *         {@link File}
     *         If the {@code Path} is valid.
     */
    private File validatePath(FilePredicate fp, Path p, List<File> output) throws IOException {
        File f = p.toFile();
        if (f.isFile() && fp.test(f) && !output.contains(f)) return f;
        return null;
    }

    /**
     * Moves given collection of files to specified destination, replace existing files.
     * @param destination
     *        The {@code File} where files will be moved.
     * @param files
     *        Files separated by commas to be moved.
     * @throws IOException
     *         If an I/O error occurs.
     */
    public void moveFiles(File destination, File... files) throws IOException {
        Objects.requireNonNull(destination, "Destination is null.");
        Objects.requireNonNull(files, "Files is null.");

        moveFiles(destination, Arrays.asList(files));
    }

    /**
     * Moves given collection of files to specified destination, replace existing files.
     * @param destination
     *        The {@code File} where files will be moved.
     * @param files
     *        The {@code Collection<File>} to be moved.
     * @throws IOException
     *         If an I/O error occurs.
     */
    public void moveFiles(File destination, Collection<File> files) throws IOException {
        Objects.requireNonNull(destination, "Destination is null.");
        Objects.requireNonNull(files, "Files is null.");

        log("Moving files.");

        if (files.isEmpty()) return;

        try {
            files.parallelStream().forEach(file -> {
                try {
                    Files.move(
                            file.toPath(),
                            Paths.get(destination + FILE_SEPARATOR + file.getName()),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
