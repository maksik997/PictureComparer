package pl.magzik.Structures;

import pl.magzik.Utils.LoggingInterface;

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

    private static ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Algorithm to calculate checksum
    protected final static CRC32 algorithm = new CRC32();

    // File Reference
    protected final File file;

    // Checksum of the content
    private final long checksum;

    private final String extension;


    public Record(File file) throws IOException {
        this.file = file;
        this.extension = file.toPath().normalize().toString().substring(
            file.toPath().normalize().toString().lastIndexOf(".")+1
        );
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
    public static <T> Map<?, List<Record<T>>> process(Collection<File> files, Function<File, ? extends Record<T>> mapFunction, Function<List<? extends Record<T>>, Map<?, List<Record<T>>>>... processFunctions) throws InterruptedException, IOException {
        LoggingInterface.staticLog("Mapping input files.");
        Map<?, List<Record<T>>> map;

        map = filter(files, mapFunction);

        try {
            for (Function<List<? extends Record<T>>, Map<?, List<Record<T>>>> function : processFunctions) {
                map = map.values().parallelStream()
                    .map(function)
                    .flatMap(m -> m.entrySet().stream())
                    .filter(e -> e.getValue().size() > 1)
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (l1, l2) -> {
                            List<Record<T>> l = new ArrayList<>(l1);
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

    private static <T> Map<Long, List<Record<T>>> filter(Collection<File> files, Function<File, ? extends Record<T>> checksumFunction) {
        // The First step will filter images using meta-data then checksum
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
                    .collect(Collectors.toConcurrentMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
        } catch (RuntimeException e) {
            e.printStackTrace(); // todo for now
            throw new RuntimeException(e);
        }
    }
}
