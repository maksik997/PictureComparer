package pl.magzik;

import pl.magzik.Structures.Record;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class Comparer<T extends Record> {

    protected ArrayList<T> duplicates;
    protected HashMap<Long, ArrayList<T>> mappedObjects;

              // total number of valid files
    protected int totalObjectCount, processedObjectCount;
                              //  total number of processed Objects

    protected List<File> sourceFiles;

    protected File sourceDirectory, destDirectory;

    protected String[] acceptedTypes;

    public Comparer(List<File> sourceFiles, File sourceDirectory, File destDirectory) throws IOException {
        this.duplicates = null;
        this.mappedObjects = null;
        this.totalObjectCount = 0;
        this.processedObjectCount = 0;
        this.sourceFiles = sourceFiles;
        this.sourceDirectory = sourceDirectory;
        this.destDirectory = destDirectory;
        this.acceptedTypes = null;

        if (this.sourceFiles == null ){
            if(this.sourceDirectory == null || !this.sourceDirectory.isDirectory())
                throw new IOException();

            this.sourceFiles = Arrays.asList(Objects.requireNonNull(sourceDirectory.listFiles(File::isFile)));
        }
    }

    public Comparer(File sourceDirectory, File destDirectory) throws IOException {
        this(null, sourceDirectory, destDirectory);
    }

    public Comparer(List<File> sourceFiles, File destDirectory) throws IOException {
        this(sourceFiles, null, destDirectory);
    }

    public ArrayList<T> getDuplicates() {
        return duplicates;
    }

    public HashMap<Long, ArrayList<T>> getMappedObjects() {
        return mappedObjects;
    }

    public int getTotalObjectCount() {
        return totalObjectCount;
    }

    public int getProcessedObjectCount() {
        return processedObjectCount;
    }

    public String generateTypePattern(){
        StringBuilder types = new StringBuilder();
        Arrays.stream(acceptedTypes).forEach(
                type -> types.append(String.format(".*\\.%s$|", type))
        );

        return types.toString();
    }

    public void _init() throws IOException{
        /*if (this.sourceFiles == null ){
            if(this.sourceDirectory == null || !this.sourceDirectory.isDirectory())
                throw new IOException();

            this.sourceFiles = Arrays.asList(Objects.requireNonNull(sourceDirectory.listFiles(File::isFile)));
        }*/

        String pattern = generateTypePattern();

        sourceFiles.stream()
                .filter(file -> file.getName().matches(pattern))
                .forEach(f -> totalObjectCount++);
    }

    public abstract void map() throws IOException;

    public abstract void findDuplicates();

    public abstract void moveDuplicates();
}
