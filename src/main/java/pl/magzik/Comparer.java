/*
    This class holds all simple operations needed to compare.
    However, you'll need to define three different methods.
    This class also is exclusively good for complex types (like images and files)
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

public abstract class Comparer<T extends Record<?>> implements LoggingInterface {
    protected CopyOnWriteArrayList<T> duplicates;
    protected ConcurrentHashMap<Long, CopyOnWriteArrayList<T>> mappedObjects;

    protected List<File> sourceFiles;
    protected File sourceDirectory, destDirectory;

    protected long totalObjectCount, processedObjectCount, duplicatesObjectCount;

    protected byte[][] formatMagicNumbers;

    protected Modes mode;

    public enum Modes{
        // All modes that you can use.
        RECURSIVE, NOT_RECURSIVE
    }

    private int longestHeaderLength;

    public Comparer(List<File> sourceFiles, File sourceDirectory, File destDirectory, Modes mode) {
        this.mode = mode;
        if (mode == null) {
            this.mode = Modes.NOT_RECURSIVE;
        }

        _setUp(sourceFiles, sourceDirectory, destDirectory);
    }

    public Comparer() {
        this.mode = Modes.NOT_RECURSIVE;

        _reset();
    }

    public Comparer(File sourceDirectory, File destDirectory) {
        this(null, sourceDirectory, destDirectory, Modes.NOT_RECURSIVE);
    }

    public Comparer(List<File> sourceFiles, File destDirectory) {
        this(sourceFiles, null, destDirectory, Modes.NOT_RECURSIVE);
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

    public Modes getMode() {
        return mode;
    }

    public void setMode(Modes mode) {
        this.mode = mode;
    }

    public ConcurrentHashMap<Long, CopyOnWriteArrayList<T>> getMappedObjects() {
        return mappedObjects;
    }

    public void _setUp(File sourceDirectory, File destDirectory) {
        _setUp(null, sourceDirectory, destDirectory, null);
    }

    public void _setUp(File sourceDirectory, File destDirectory, Modes mode) {
        _setUp(null, sourceDirectory, destDirectory, mode);
    }

    public void _setUp(List<File> sourceFiles, File destDirectory) {
        _setUp(sourceFiles, null, destDirectory, null);
    }

    public void _setUp(List<File> sourceFiles, File destDirectory, Modes mode) {
        _setUp(sourceFiles, null, destDirectory, mode);
    }

    public void _setUp(List<File> sourceFiles, File sourceDirectory, File destDirectory) {
        this._setUp(sourceFiles, sourceDirectory, destDirectory, null);
    }

    public void _setUp(List<File> sourceFiles, File sourceDirectory, File destDirectory, Modes mode){
        this.mode = mode;
        if(this.mode == null) {
            this.mode = Modes.NOT_RECURSIVE;
        }

        log("Setting up Comparer");
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
            log("Comparer reset complete");
        }

        this.sourceFiles = sourceFiles;
        this.sourceDirectory = sourceDirectory;
        this.destDirectory = destDirectory;

        log("Reducing data set only to valid files.");
        if (this.sourceFiles == null){
            if(this.sourceDirectory == null || !this.sourceDirectory.isDirectory()){
                log(new IOException("Couldn't find any source files."), "Couldn't find any source files.");
                throw new RuntimeException("Please refer to error.txt log file.");
            }

            this.sourceFiles = Arrays.asList(Objects.requireNonNull(
                this.sourceDirectory.listFiles()
            ));
        }

        log("Preparing magic headers for reduction.");
        // Take only valid files for this type of Comparer
        longestHeaderLength = Arrays.stream(formatMagicNumbers)
                .map(b -> b.length)
                .max(Integer::compareTo).orElse(0);
        if(longestHeaderLength == 0) {
            log(new RuntimeException(), "Magic header was not assigned.");
            throw new RuntimeException("Please refer to error.txt log file.");
        }

        log("Preparing data structures for reduction");
        List<File> directories = null;
        if (mode == Modes.RECURSIVE)
            directories = List.copyOf(this.sourceFiles).stream().filter(File::isDirectory).toList();

        log("Reducing not valid files");
        this.sourceFiles =
        this.sourceFiles.stream().filter(File::isFile).filter(this::filePredicate).collect(Collectors.toList());

        if (mode == Modes.RECURSIVE) {
            log("Recursive directory search.");
            directories.stream()
                .filter(File::isDirectory)
                .forEach(d -> {
                    File[] files = d.listFiles();
                    if (files == null || files.length == 0) {
                        return;
                    }
                    List<File> dirFiles =
                    Arrays.stream(files).filter(this::filePredicate).toList();

                    this.sourceFiles.addAll(dirFiles);
                });
        }

        totalObjectCount = this.sourceFiles.size();
    }

    public void _reset() {
        log("Resetting Comparer.");

        this.duplicates = null;
        this.mappedObjects = null;
        this.totalObjectCount = 0;
        this.processedObjectCount = 0;
        this.sourceFiles = null;
        this.sourceDirectory = null;
        this.destDirectory = null;
    }

    public abstract void map() throws IOException;

    public abstract void compare();

    public abstract void move();

    private boolean filePredicate(File f) {
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
            log(e, "There was an error while operating on files.");
            throw new RuntimeException("Please refer to error.txt log file.");
        }
    }

    @Override
    public void log(String msg) {
        Logger.info(msg);
    }

    @Override
    public void log(Exception ex, String msg) {
        Logger.error(ex, msg);
    }
}
