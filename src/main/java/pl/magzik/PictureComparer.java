package pl.magzik;

import pl.magzik.Structures.ImageRecord;
import pl.magzik.Structures.Record;
import pl.magzik.Structures.Utils.LoggingInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PictureComparer implements Comparer<ImageRecord>, LoggingInterface {

    private Map<Long, List<ImageRecord>> mappedObjects;
    private Queue<ImageRecord> duplicates;
    private List<File> sourceFiles;
    private File destDirectory;
    private Comparer.Modes mode;

    private int processedObjectCount;

    // Magic numbers table
    // 0 -> JPEG, 1 -> PNG, 2 -> GIF(v1), 3 -> GIF(v2), 4 -> BMP, 5 -> TIFF(v1), 6 -> TIFF(v2), 7 -> ICO
    // 8 -> JPEG2000(v1) cut, 9 -> JPEG2000(v2),
    private final static byte[][] formatMagicNumbers = {
        {(byte)0xff, (byte)0xd8, (byte)0xff,},
        {(byte)0x89, (byte)0x50, (byte)0x4e, (byte)0x47, (byte)0x0d, (byte)0x0a, (byte)0x1a, (byte)0x0a,},
        {(byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x37, (byte)0x61,},
        {(byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x39, (byte)0x61,},
        {(byte)0x42, (byte)0x4d,},
        {(byte)0x49, (byte)0x49, (byte)0x2a, (byte)0x00,},
        {(byte)0x4d, (byte)0x4d, (byte)0x00, (byte)0x2a,},
        {(byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00,},
        {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0c, (byte)0x6a, (byte)0x50, (byte)0x20, (byte)0x20, (byte)0x0d, (byte)0x0a, (byte)0x87, (byte)0x0a},
        {(byte)0xff, (byte)0x4f, (byte)0xff, (byte)0x51,},
    };
    private final static int longestHeaderLength = Arrays.stream(formatMagicNumbers)
                                                            .map(b -> b.length)
                                                            .max(Integer::compareTo).orElse(0);

    // deprecated 0.5
    /*public PictureComparer(List<File> sourceFiles, File sourceDirectory, File destDirectory, Comparer.Modes mode) {
        super(sourceFiles, sourceDirectory, destDirectory, mode);
        //super.formatMagicNumbers  = imageMagicNumbers;
        _setUp(sourceFiles, sourceDirectory, destDirectory);

        log("Picture Comparer initialized");
    }

    public PictureComparer() {
        super();
        super.formatMagicNumbers  = imageMagicNumbers;
        log("Picture Comparer initialized");
    }

    public PictureComparer(File sourceDirectory, File destDirectory) {
        this(null, sourceDirectory, destDirectory, null);
    }

    public PictureComparer(List<File> sourceFiles, File destDirectory) {
        this(sourceFiles, null, destDirectory, null);
    }*/

    public PictureComparer() {
        _reset();
    }

    public PictureComparer(File dest, Collection<File> source) {
        this();
        _setUp(dest, source);
    }

    public PictureComparer(File dest, File... source) {
        this(dest, Arrays.asList(source));
    }

    public Map<Long, List<ImageRecord>> getMappedObjects() {
        return mappedObjects;
    }

    public Queue<ImageRecord> getDuplicates() {
        return duplicates;
    }

    public List<File> getSourceFiles() {
        return sourceFiles;
    }

    public File getDestDirectory() {
        return destDirectory;
    }

    public Modes getMode() {
        return mode;
    }

    public int getProcessedObjectCount() {
        return processedObjectCount;
    }

    public int getTotalObjectCount() {
        return sourceFiles.size();
    }

    public int getDuplicatesObejctCount() {
        return duplicates.size();
    }

    public void setMode(Modes mode) {
        this.mode = mode;
    }

    @Override
    public void _setUp(File dest, Collection<File> source) {
        log("Setting Comparer up.");
        _reset();

        log("Validating data.");
        if(dest == null || source == null) {
            throw new RuntimeException();
        }
        if(!dest.exists() || !dest.isDirectory()) {
            throw new RuntimeException();
        }

        this.destDirectory = dest;

        if(source.isEmpty()) {
            throw new RuntimeException();
        }

        source.stream()
                .filter(f -> !f.exists())
                .findAny()
                .orElseThrow(RuntimeException::new);

        log("Collecting data from source.");

        sourceFiles.addAll(
            source.stream()
            .filter(File::isFile)
            .toList()
        );

        if (mode == Modes.RECURSIVE) {
            log("Recursive data search.");
            source.stream()
            .filter(File::isDirectory)
            .map(File::toPath)
            .forEach(f -> {
                try {
                    Files.walkFileTree(f, new SimpleFileVisitor<>(){
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            File tmp = file.toFile();
                            if (tmp.isFile() && filePredicate(tmp)) {
                                sourceFiles.add(tmp);
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }


        log("Comparer ready to work!");
    }

    @Override
    public void _reset() {
        log("Resetting Comparer.");

        this.mode = Comparer.Modes.NOT_RECURSIVE;
        this.mappedObjects = null;
        this.duplicates = null;
        this.processedObjectCount = 0;
        this.sourceFiles = new ArrayList<>();
        this.destDirectory = null;

        log("Reset complete!");
    }

    @Override
    public void map() {
        // This method will map all files using parallel stream to map Checksum -> List of images.
        // Represents a relation where Checksum is checksum calculated of input image.

        log("Mapping files.");
        // 0.5 - deprecated
        mappedObjects = //new ConcurrentHashMap<>();
            sourceFiles.parallelStream()
                .map(file -> {
                    try {
                        processedObjectCount++;
                        log(String.format("Processed %d images from %d total", processedObjectCount, sourceFiles.size()));
                        return new ImageRecord(file);
                    } catch (IOException ex) {
                        log(String.format("Skipping file: %s", file.getName()));
                        log(ex, String.format("Skipping file: %s", file.getName()));
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Record::getChecksum));

        /*sourceFiles 0.5 - deprecated
            .parallelStream()
            .forEach(file -> {
                try {
                    ImageRecord ir = new ImageRecord(file);
                    if(!map.containsKey(ir.getChecksum())) // calculating checksum
                        map.put(ir.getChecksum(), new LinkedList<>());
                    map.get(ir.getChecksum()).add(ir);
                    super.processedObjectCount++;
                    log(String.format("Processed %d images from %d total", processedObjectCount, totalObjectCount));
                } catch (IOException e) {
                    // skip that file
                    log(String.format("Skipping file: %s", file.getName()));
                    log(e, String.format("Skipping file: %s", file.getName()));
                }
            }
        );*/
    }

    @Override
    public void compare() {
        log("Extracting duplicates");

        // Queue of elements to remove.
        Queue<ImageRecord> duplicates = new LinkedList<>();

        // 0.5 - new
        mappedObjects.keySet()
            .forEach(k -> duplicates.addAll(
                mappedObjects.entrySet().stream()
                    .filter(e -> !e.getValue().contains(null) && k.equals(e.getKey()))
                    .flatMap(e -> Stream.of((ImageRecord[]) e.getValue().toArray())) // In theory, it's impossible to encounter here a null value
                    .toList()
            ));

        // 0.5 - deprecated
        /*mappedObjects.forEach(
            (k, v) -> {
                for (int i = 0; i < v.size(); i++)
                    if( i > 0 ) duplicates.add(v.get(i));
            }
        );*/

        this.duplicates = duplicates;
        log(String.format("Found %d duplicates from %d all images", duplicates.size(), sourceFiles.size()));
    }

    @Override
    public void move() {
        log("Moving duplicated images");
        String sep = File.separator;

        duplicates.parallelStream()
        .map(Record::getFile)
        .forEach(f -> {
            try {
                Files.move(
                    f.toPath(),
                    Paths.get(destDirectory + sep + f.getName()),
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException e) {
                log(e, e.getMessage());
                throw new RuntimeException("Please refer to error.txt log file.");
            }
        });

        // deprecated v0.5
        /*super.duplicates
            .parallelStream()
            .forEach(
                ir -> {
                    File f = ir.getFile();
                    try {
                        Files.move(
                            f.toPath(),
                            Paths.get(destDirectory + separator + f.getName()),
                            StandardCopyOption.REPLACE_EXISTING
                        );
                    } catch (IOException e) {
                        log(e, e.getMessage());
                        throw new RuntimeException("Please refer ti error.txt log file.");
                    }
                }
            );*/
    }

    @Override
    public boolean filePredicate(File f) {
        try (FileInputStream is = new FileInputStream(f)) {
            byte[] header = new byte[longestHeaderLength];

            if(is.read(header) == -1)
                return false; // The file couldn't be an image if it's too small

            for (byte[] formatHeader : formatMagicNumbers) {
                int i = 0;
                boolean isValid = true;
                for (byte formatByte : formatHeader) {
                    if (formatByte != header[i++]) {
                        isValid = false;
                        break;
                    }
                }
                if (isValid) return true;
            }
        } catch (IOException e) {
            log(e, "There was an error while operating on files.");
            throw new RuntimeException("Please refer to error.txt log file.");
        }

        return false;
    }
}