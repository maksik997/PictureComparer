package pl.magzik.IO;

import pl.magzik.Comparator.FilePredicate;
import pl.magzik.Utils.LoggingInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public class FileOperator implements LoggingInterface {
    private Path destination;

    private final List<File> output;

    private ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public FileOperator() {
        this(Path.of(System.getProperty("user.home"), "Documents"));
    }

    public FileOperator(Path destination) {
        log("Created FileOperator.");
        this.destination = destination;
        this.output = Collections.synchronizedList(new LinkedList<>());
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
        if (destination == null) {
            RuntimeException re = new NullPointerException("Destination is null.");
            log(re, "Error: ");
            throw re;
        }

        if (source == null) {
            RuntimeException re = new IllegalArgumentException("Source is null.");
            log(re, "Error:");
            throw re;
        }

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

        //List<File> output;

        log("Collecting files from " + source);
        try {
            output.addAll(source.parallelStream()
                    .filter(File::isFile)
                    .filter(f -> {
                        try {
                            return fp.test(f);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList());
        } catch (RuntimeException ex) {
            throw new IOException(ex.getMessage());
        }

        log("Directory files collection.");

        try {
            source.parallelStream()
                    .filter(File::isDirectory)
                    .map(File::toPath)
                    .forEach(p -> {
                        try (Stream<Path> s = Files.walk(p, depth)) {
                            s.filter(Files::isRegularFile)
                                    .forEach(path -> virtualExecutor.submit(() -> {
                                        try {
                                            processPath(fp, path, output);
                                        } catch (IOException e) {
                                            // tmp
                                            if (e instanceof FileSystemException) log("Skipping " + path + " because " + e.getMessage());
                                            else throw new RuntimeException("Could not process file " + p, e);
                                        }
                                    }));
                        } catch (IOException e) {
                            throw new RuntimeException("Could not walk " + p, e);
                        }
                    });
        } catch (RuntimeException ex) {
            // tmp
            if (ex.getMessage().contains("FileSystemException")) log("Skipping because " + ex.getMessage());
            else throw new IOException(ex.getMessage());
        }

        virtualExecutor.shutdown();
        long timeout = 1;
        while (!virtualExecutor.awaitTermination(timeout, TimeUnit.MINUTES)) {
            if (timeout > 10) {
                virtualExecutor.shutdownNow();
                if (virtualExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                    Thread.currentThread().interrupt();
                }
            }
            log("Waiting for " + (timeout*=2) + " minutes more...");
        }
        virtualExecutor.close();

        virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        return output;
    }

    private void processPath(FilePredicate fp, Path p, List<File> output) throws IOException {
        File f = p.toFile();
        if (f.isFile() && fp.test(f) && !output.contains(f)) {
            output.add(f);
        }
    }
}
