package pl.magzik;

import pl.magzik.Structures.ImageRecord;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PictureComparer extends Comparer<ImageRecord> {
    private static Logger logger = Logger.getLogger(PictureComparer.class.getName());

    public PictureComparer(List<File> sourceFiles, File sourceDirectory, File destDirectory) throws IOException {
        super(sourceFiles, sourceDirectory, destDirectory);
        super.acceptedTypes = ImageRecord.acceptedTypes;
    }

    public PictureComparer(File sourceDirectory, File destDirectory) throws IOException {
        super(sourceDirectory, destDirectory);
        super.acceptedTypes = ImageRecord.acceptedTypes;
    }

    public PictureComparer(List<File> sourceFiles, File destDirectory) throws IOException {
        super(sourceFiles, destDirectory);
        super.acceptedTypes = ImageRecord.acceptedTypes;
    }

/*    @Override
    public void _init() throws IOException {
        if (super.sourceFiles == null ){
            if(super.sourceDirectory == null || !super.sourceDirectory.isDirectory())
                throw new IOException();

            super.sourceFiles = Arrays.asList(Objects.requireNonNull(sourceDirectory.listFiles(File::isFile)));
        }

        StringBuilder types = new StringBuilder();
        Arrays.stream(acceptedTypes).forEach(
                type -> types.append(String.format(".*\\.%s$|", type))
        );

        String pattern = types.toString();

        sourceFiles.stream()
                .filter(file -> file.getName().matches(pattern))
                .forEach(f -> processedObjectCount++);
    }*/

    @Override
    public void map() throws IOException {
        String pattern = super.generateTypePattern();

        HashMap<Long, ArrayList<ImageRecord>> map = new HashMap<>();

        System.out.println(sourceFiles);

        sourceFiles.stream()
        .filter(file -> file.getName().matches(pattern))
        .forEach(
            file -> {
                try {
                    ImageRecord ir = new ImageRecord(file);
                    if(!map.containsKey(ir.getChecksum()))
                        map.put(ir.getChecksum(), new ArrayList<>());
                    map.get(ir.getChecksum()).add(ir);
                    super.processedObjectCount++;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );


        super.mappedObjects = map;
    }

    @Override
    public void findDuplicates() {
        ArrayList<ImageRecord> duplicates = new ArrayList<>();

        mappedObjects.forEach(
            (k, v) -> {
                for (int i = 0; i < v.size(); i++)
                    if( i > 0 ) duplicates.add(v.get(i));
            }
        );

        super.duplicates = duplicates;
    }

    @Override
    public void moveDuplicates() {
        String separator = File.separator;
        super.duplicates.forEach(
            ir -> {
                File f = ir.getFile();
                try {
                    Files.move(
                        f.toPath(),
                        Paths.get(destDirectory + separator + f.getName()),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    public static void main(String[] args) throws IOException {
        ConsoleHandler consoleHandler = new ConsoleHandler();

        consoleHandler.setLevel(Level.ALL);

        logger.addHandler(consoleHandler);
        logger.fine("HEH");



        PictureComparer pc = new PictureComparer(
            Arrays.asList(
                    new File("C:\\Users\\maksy\\OneDrive\\Pulpit\\ThousandPictureComapre\\data\\testingData\\data_convertToPNG1 — kopia (2).png"),
                    new File("C:\\Users\\maksy\\OneDrive\\Pulpit\\ThousandPictureComapre\\data\\testingData\\data_convertToPNG1 — kopia.png")

            ),
            new File("C:\\Users\\maksy\\OneDrive\\Pulpit\\ThousandPictureComapre\\data\\testingData")
        );

        pc._init();

        System.out.println("total: " + pc.getTotalObjectCount() + "\nprocessed: " + pc.getProcessedObjectCount());


        pc.map();

        pc.getMappedObjects().forEach(
                (k,v) -> System.out.println("key: " + k + "\n" + v)
        );

        pc.findDuplicates();

        System.out.println(pc.getDuplicates());

        System.out.println("total: " + pc.getTotalObjectCount() + "\nprocessed: " + pc.getProcessedObjectCount());

        logger.fine("HEH");
    }
}