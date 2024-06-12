package pl.magzik.Structures;

import pl.magzik.Utils.LoggingInterface;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

public abstract class Record<T> implements Comparable<Record<T>>, LoggingInterface {

    private static ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Algorithm to calculate checksum
    protected final static CRC32 algorithm = new CRC32();

    // File Reference
    private final File file;

    // Checksum of the content
    private long checksum;

    private final String extension;


    public Record(File file) {
        this.file = file;
        this.extension = file.toPath().normalize().toString().substring(
            file.toPath().normalize().toString().lastIndexOf(".")+1
        );
        this.checksum = this.hashCode();
    }
    public Record(Record<T> r){
        this(r.file);
    }

    public File getFile() {
        return file;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    protected abstract void calculateAndSetChecksum(T e);

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

    @Override
    public int compareTo(Record<T> o) { // Simple as that, for now q:
        int c = Long.compare(checksum, o.checksum);
        if (c == 0) return extension.compareTo(o.extension);
        return c;
    }

    public static <T> Map<Long, List<Record<T>>> analyze(Collection<File> files, Function<File, ? extends Record<T>> mapFun) throws InterruptedException, IOException {
        LoggingInterface.staticLog("Mapping input files.");
        Map<Long, List<Record<T>>> map;

        //try {
            map = filter(files, mapFun);
                    /*files.parallelStream()
                .map(file -> virtualExecutor.submit(() -> mapFun.apply(file)))
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) { // This will happen most likely when checksum creating method will fail.
                        LoggingInterface.staticLog(e, "Skipped files, couldn't create checksum.");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingByConcurrent(Record::getChecksum));*/


            LoggingInterface.staticLog("Finished mapping files.");
            return map;
        /*} catch (RuntimeException e) {
            e.printStackTrace(); // todo for now
            throw new RuntimeException(e);
        }*/
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
                    .collect(Collectors.groupingByConcurrent(Record::getChecksum)).entrySet()
                    .parallelStream().filter(e -> e.getValue().size() > 1)
                    .collect(Collectors.toConcurrentMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
        } catch (RuntimeException e) {
            e.printStackTrace(); // todo for now
            throw new RuntimeException(e);
        }
    };
}
