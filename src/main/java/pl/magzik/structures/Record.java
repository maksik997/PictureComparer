package pl.magzik.structures;

import pl.magzik.algorithms.Algorithm;
import pl.magzik.utils.LoggingInterface;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

public abstract class Record<T> implements LoggingInterface {

    @Deprecated
    private static final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    protected final File file;
    private final long checksum;
    private final String extension;


    public Record(File file) throws IOException {
        Objects.requireNonNull(file, "File cannot be null");
        this.file = file;
        this.extension = getExtension(file);
        this.checksum = createChecksum(file);
    }

    public Record(Record<T> r) throws IOException {
        this(r.file);
    }

    public File getFile() {
        return file;
    }

    public long getChecksum() {
        return checksum;
    }

    protected abstract long createChecksum(File e) throws IOException;

    /**
     * Retrieves the file extension based on the file's name.
     *
     * @param f the file from which to retrieve the extension
     * @return the file extension (without the dot) or an empty string if the extension is not found
     */
    protected String getExtension(File f) {
        int idx = f.getName().lastIndexOf('.');
        return idx == -1 ? "" : f.getName().substring(idx + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Record<?> record)) return false;
        return checksum == record.checksum && Objects.equals(extension, record.extension) && Objects.equals(file, record.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, checksum);
    }

    @Override
    public String toString() {
        return "Record{" +
                "file=" + file +
                ", checksum=" + checksum +
                '}';
    }

    @SafeVarargs
    @Deprecated
    public static <T, R extends Record<T>> Map<?, List<R>> process(Collection<File> files, Function<File, R> mapFunction, Algorithm<?, R>... processFunctions) throws IOException, ExecutionException {
        LoggingInterface.staticLog("Mapping input files.");
        Map<?, List<R>> map;

        try {
            map = groupByChecksum(files, mapFunction);
            for (var function : processFunctions) {
                map = map.values().parallelStream()
                    .map(function::apply)
                    .flatMap(m -> m.entrySet().stream())
                    .filter(e -> e.getValue().size() > 1)
                    .collect(Collectors.toMap(
                        r -> r.getKey(),
                        r -> r.getValue(),
                        (l1, l2) -> {
                            List<R> l = new ArrayList<>(l1);
                            l.addAll(l2);
                            return l;
                        }
                    ));
            }
        } catch (UncheckedIOException ex) {
            LoggingInterface.staticLog("Short circuit, stopping.");
            throw ex.getCause();
        }

        LoggingInterface.staticLog("Finished mapping files.");
        return map;
    }

    @Deprecated
    private static <T, R extends Record<T>> Map<Long, List<R>> groupByChecksum(Collection<File> files, Function<File, R> checksumFunction) throws ExecutionException {
        // The First step will groupByChecksum images using meta-data then checksum
        try {
            return files.parallelStream()
                    .map(File::toPath)
                    .map(Path::normalize)
                    .distinct()
                    .map(Path::toFile)
                    .map(file -> virtualExecutor.submit(() -> checksumFunction.apply(file)))
                    .map(ftr -> {
                        try {
                            return ftr.get();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (ExecutionException e) { // This will happen most likely when checksum creating method fails.
                            LoggingInterface.staticLog(e, "Skipped files, couldn't create checksum.");
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(ir -> ir.getChecksum() != 0L)
                    .collect(Collectors.groupingByConcurrent(Record::getChecksum)).entrySet()
                    .parallelStream().filter(e -> e.getValue().size() > 1)
                    .collect(Collectors.toConcurrentMap(e -> e.getKey(), e -> new ArrayList<>(e.getValue())));
        } catch (RuntimeException e) {
            LoggingInterface.staticLog("Error while grouping...");
            throw new ExecutionException(e);
        }
    }
}
