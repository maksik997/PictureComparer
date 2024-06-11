package pl.magzik.IO;

import pl.magzik.Comparator.FilePredicate;
import pl.magzik.Utils.LoggingInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class FileOperator implements LoggingInterface {
    private Path destination;

    private ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public FileOperator() {
        this(Path.of(System.getProperty("user.home"), "Documents"));
    }

    public FileOperator(Path destination) {
        log("Created FileOperator.");
        this.destination = destination;
    }

    public Path getDestination() {
        return destination;
    }

    public void setDestination(Path destination) {
        log("Changed directory to " + destination);
        this.destination = destination;
    }

    public List<File> loadFiles(int depth, FilePredicate fp, File... source) throws IOException, InterruptedException, TimeoutException {
        return loadFiles(depth, fp, Arrays.asList(source));
    }

    public List<File> loadFiles(int depth, FilePredicate fp, Collection<File> source) throws IOException, InterruptedException, TimeoutException {
        log("Loading files from " + source);

        log("Validating files");
        Objects.requireNonNull(destination, "Destination is null");
        Objects.requireNonNull(source, "Depth is null");

        try {
            source.parallelStream()
                .filter(f -> !f.exists())
                .findAny()
                .ifPresent(f -> {
                    throw new RuntimeException("Could not find file " + f);
                });
        } catch (RuntimeException e) {
            throw new FileNotFoundException(e.getMessage());
        }

        log("Files validated.");

        List<File> output;

        log("Collecting files from " + source);
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
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }

        log("Directory files collection.");

        List<Future<File>> filesTasks = new LinkedList<>();

        source.stream()
                .filter(File::isDirectory)
                .map(File::toPath)
                .forEach(p -> {
                    try {
                        Files.walkFileTree(p, EnumSet.noneOf(FileVisitOption.class), depth, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                if (Files.isRegularFile(file)) {
                                    filesTasks.add(virtualExecutor.submit(() -> processPath(fp, file, output)));
                                }

                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                                log("Skipping " + file + " because " + exc);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not walk " + p, e);
                    }
                });

        virtualExecutor.shutdown();
        // For now time for file searching is 10 minutes.
        long timeout = 600;
        if (!virtualExecutor.awaitTermination(timeout, TimeUnit.SECONDS)) {
            virtualExecutor.shutdownNow();
            if (!virtualExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new TimeoutException("Executor couldn't end created threads.");
            }
        }
        
        virtualExecutor.close();
        virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        try {
            for (Future<File> future : filesTasks) {
                try {
                    File f = future.get();
                    if (f != null) output.add(f);
                } catch (ExecutionException e) {
                    log(e, "Skipping file... Refer to error.txt");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Couldn't get files from future", e);
        }

        return output;
    }

    private File processPath(FilePredicate fp, Path p, List<File> output) throws IOException {
        File f = p.toFile();
        if (f.isFile() && fp.test(f) && !output.contains(f)) return f;
        return null;

    }
}
