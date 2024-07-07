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

public class FileOperator implements LoggingInterface {

    private ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public FileOperator() {}

    public List<File> loadFiles(int depth, FilePredicate fp, File... source) throws IOException, InterruptedException, TimeoutException {
        return loadFiles(depth, fp, Arrays.asList(source));
    }

    public List<File> loadFiles(int depth, FilePredicate fp, Collection<File> source) throws IOException, InterruptedException, TimeoutException {
        // This method will load files (either from file path or directory path (in depth directories)),
        // then, a method will check if given files fulfil given FilePredicate.
        // Due to recreating ExecutorService, I advise to use it for larger collections of files more rarely (to avoid any memory leaks etc.)
        log("Loading files from " + source);

        log("Validating files");
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

        List<Future<File>> filesTasks = Collections.synchronizedList(new LinkedList<>());

        source.parallelStream()
                .filter(File::isDirectory)
                .map(File::toPath)
                .forEach(p -> {
                    try {
                        Files.walkFileTree(p, EnumSet.of(FileVisitOption.FOLLOW_LINKS), depth, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                if (Files.isRegularFile(file)) {
                                    filesTasks.add(virtualExecutor.submit(() -> processPath(fp, file, output)));
                                }

                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                log("Skipping " + file + " because " + exc);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not walk " + p, e);
                    }
                });

        log("Waiting for files to be loaded.");
        virtualExecutor.shutdown();
        // For now time for file searching is 60 minutes.
        long timeout = 60;
        if (!virtualExecutor.awaitTermination(timeout, TimeUnit.MINUTES)) {
            log("Time exceeded, closing executor.");
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
                    log(e, "Skipped file. Refer to error.txt...");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Couldn't get files from future", e);
        }

        log("File loading completed");
        return output;
    }

    private File processPath(FilePredicate fp, Path p, List<File> output) throws IOException {
        File f = p.toFile();
        if (f.isFile() && fp.test(f) && !output.contains(f)) return f;
        return null;
    }
}
