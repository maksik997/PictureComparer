/*
    This class holds all simple operations needed to compare.
    However, you'll need to define three different methods
*/
package pl.magzik;

import pl.magzik.Structures.Record;
import pl.magzik.Structures.Utils.LoggingInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.tinylog.Logger;

public abstract class Comparer<T extends Record> implements LoggingInterface {
    protected CopyOnWriteArrayList<T> duplicates;
    protected ConcurrentHashMap<Long, CopyOnWriteArrayList<T>> mappedObjects;

    protected List<File> sourceFiles;
    protected File sourceDirectory, destDirectory;

    protected long totalObjectCount, processedObjectCount, duplicatesObjectCount;

    protected byte[][] formatMagicNumbers;

    public static boolean echo = false; // tmp

    public Comparer(List<File> sourceFiles, File sourceDirectory, File destDirectory) throws IOException {
        _setUp(sourceFiles, sourceDirectory, destDirectory);
    }

    public Comparer() throws IOException {
        _reset();
    }

    public Comparer(File sourceDirectory, File destDirectory) throws IOException {
        this(null, sourceDirectory, destDirectory);
    }

    public Comparer(List<File> sourceFiles, File destDirectory) throws IOException {
        this(sourceFiles, null, destDirectory);
    }

    public long getTotalObjectCount() {
        return totalObjectCount;
    }

    public long getProcessedObjectCount() {
        return processedObjectCount;
    }

    public long getDuplicatesObjectCount() {
        return duplicatesObjectCount;
    }

    public CopyOnWriteArrayList<T> getDuplicates() {
        return duplicates;
    }

    public ConcurrentHashMap<Long, CopyOnWriteArrayList<T>> getMappedObjects() {
        return mappedObjects;
    }

    public void _setUp(File sourceDirectory, File destDirectory) throws IOException {
        _setUp(null, sourceDirectory, destDirectory);
    }

    public void _setUp(List<File> sourceFiles, File destDirectory) throws IOException {
        _setUp(sourceFiles, null, destDirectory);
    }

    public void _setUp(List<File> sourceFiles, File sourceDirectory, File destDirectory) throws IOException {
        if (
            this.duplicates != null ||
            this.mappedObjects != null ||
            this.totalObjectCount > 0 ||
            this.processedObjectCount > 0 ||
            this.sourceFiles != null ||
            this.sourceDirectory != null ||
            this.destDirectory != null
        ) {
            _reset();
        }

        this.sourceFiles = sourceFiles;
        this.sourceDirectory = sourceDirectory;
        this.destDirectory = destDirectory;

        if (this.sourceFiles == null){
            if(this.sourceDirectory == null || !this.sourceDirectory.isDirectory())
                throw new IOException("Couldn't find any source files.");

            this.sourceFiles = Arrays.asList(Objects.requireNonNull(
                this.sourceDirectory.listFiles(File::isFile)
            ));
        }

        // Take only valid files for this type of Comparer
        int longestHeaderLength = Arrays.stream(formatMagicNumbers)
                .map(b -> b.length)
                .max(Integer::compareTo).orElse(0);
        if(longestHeaderLength == 0)
            throw new RuntimeException(); // magic header not assigned

        this.sourceFiles =
        this.sourceFiles.stream().filter(f -> {
            try (FileInputStream fis = new FileInputStream(f)) {
                byte[] header = new byte[longestHeaderLength];
                if(fis.read(header) == -1)
                    return false; // The file couldn't be an image if it's too small

                for (byte[] formatHeader : formatMagicNumbers) {
                    int i = 0;
                    boolean isValid = true;
                    for (byte formatByte : formatHeader) {
                        if (formatByte != header[i++]){
                            isValid = false;
                            break;
                        }
                    }
                    if (isValid) return true;
                }

                return false;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        totalObjectCount = this.sourceFiles.size();
    }

    public void _reset() {
        this.duplicates = null;
        this.mappedObjects = null;
        this.totalObjectCount = 0;
        this.processedObjectCount = 0;
        this.sourceFiles = null;
        this.sourceDirectory = null;
        this.destDirectory = null;

        System.gc(); // todo test if applicable and if it's needed
    }

    public abstract void map() throws IOException;

    public abstract void compare();

    public abstract void move();

    @Override
    public void log(String msg) {
        Logger.info(msg);
    }
}
