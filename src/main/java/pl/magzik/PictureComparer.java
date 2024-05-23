package pl.magzik;

import pl.magzik.Structures.ImageRecord;
import pl.magzik.Structures.Record;
import pl.magzik.Structures.Utils.LoggingInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PictureComparer implements Comparer, LoggingInterface {

    private Map<Long, List<ImageRecord>> mappedObjects;
    private List<ImageRecord> duplicates;
    private List<File> sourceFiles;
    private File destDirectory;
    private Comparer.Modes mode;
    private final FileVisitor fileVisitor;

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

    public PictureComparer() {
        fileVisitor = new FileVisitor();
        _reset();
    }

    public PictureComparer(File dest, Collection<File> source) throws FileNotFoundException {
        this();
        _setUp(dest, source);
    }

    public PictureComparer(File dest, File... source) throws FileNotFoundException {
        this(dest, Arrays.asList(source));
    }

    public Map<Long, List<ImageRecord>> getMappedObjects() {
        return mappedObjects;
    }

    public List<ImageRecord> getDuplicates() {
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

    public int getTotalObjectCount() {
        return sourceFiles == null ? 0 : sourceFiles.size();
    }

    public int getDuplicatesObjectCount() {
        return duplicates == null ? 0 : duplicates.size();
    }

    public void setMode(Modes mode) {
        this.mode = mode;
    }

    @Override
    public void _setUp(File dest, Collection<File> source) throws FileNotFoundException {
        // This method is called to set up whole comparer without calling this method
        // (it is called also via constructor)
        // you'll surely encounter major bugs.

        log("Setting Comparer up.");
        _reset();

        log("Validating data.");
        if(dest == null || source == null) {
            throw new NullPointerException("An argument is null.");
        }
        if(!dest.exists() || !dest.isDirectory()) {
            throw new IllegalArgumentException("Destination directory isn't a directory or doesn't exist.");
        }

        this.destDirectory = dest;

        if(source.isEmpty()) {
            throw new IllegalArgumentException("Sources are empty!");
        }

        AtomicBoolean notExistentCheck = new AtomicBoolean(false);
        // if true -> there are not-existent files.
        // if false -> there aren't not-existent files.

        source.parallelStream()
                .filter(f -> !f.exists())
                .findAny()
                .ifPresent(f -> notExistentCheck.set(true));

        if (notExistentCheck.get())
            throw new FileNotFoundException("Couldn't find given file.");

        log("Collecting data from source.");

        sourceFiles.addAll(
            source.stream()
            .filter(File::isFile)
            .filter(this::filePredicate)
            .toList()
        );

        if (mode == Modes.RECURSIVE) {
            log("Recursive data search.");
            source.parallelStream()
            .filter(File::isDirectory)
            .map(File::toPath)
            .forEach(f -> {
                try {
                    Files.walkFileTree(f, fileVisitor);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            log("Non-recursive data search.");
            source.parallelStream()
                .filter(File::isDirectory)
                .map(File::toPath)
                .forEach(f -> {
                    try {
                        Files.walkFileTree(f, Collections.singleton(FileVisitOption.FOLLOW_LINKS), 1, fileVisitor);
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

        this.mappedObjects = null;
        this.duplicates = null;
        this.sourceFiles = new ArrayList<>();
        this.destDirectory = null;

        log("Reset complete!");
    }

    @Override
    public void map() {
        // This method will map all files using parallel stream to map Checksum -> List of images.
        // Represents a relation where Checksum is checksum calculated of input image.

        Function<File, ImageRecord> createImageRecord = file -> {
            try {
                return new ImageRecord(file);
            } catch (IOException ex) {
                log(String.format("Skipping file: %s", file.getName()));
                log(ex, String.format("Skipping file: %s", file.getName()));
            }
            return null;
        };

        log("Mapping files.");

        mappedObjects = sourceFiles.parallelStream()
                .map(createImageRecord)
                .filter(Objects::nonNull) // Important!
                .collect(Collectors.groupingBy(Record::getChecksum));
    }

    @Override
    public void compare() {
        // This method will extract duplicates from mapped objects
        // Will throw Null Pointer Exception in case of calling before map method.

        if (mappedObjects == null)
            throw new NullPointerException("Invalid call. Compare method called before map method.");

        log("Extracting duplicates");

        // Collection of elements to remove.
        List<ImageRecord> duplicates = new LinkedList<>();

        mappedObjects.keySet()
            .forEach(k -> duplicates.addAll(
                mappedObjects.entrySet().parallelStream()
                    .filter(e -> !e.getValue().contains(null) && k.equals(e.getKey()))
                    .map(Map.Entry::getValue)
                    .map(e -> e.subList(1, e.size()))
                    .flatMap(List::stream)
                    .toList()
            ));

        this.duplicates = duplicates;

        log(String.format("Found %d duplicates from %d all images", duplicates.size(), sourceFiles.size()));
    }

    @Override
    public void move() {
        // This method will move any duplicates found by compare method.
        // If you call it before compare method, Null Pointer Exception will be thrown.

        if (duplicates == null)
            throw new NullPointerException("Invalid call. Move method called before compare method.");

        if (duplicates.isEmpty())
            return;

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
    }

    @Override
    public boolean filePredicate(File f) {
        // This method will return boolean if the file is valid.
        // Magic numbers for files formats.
        // Listed at the beginning.

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

    private class FileVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            File fileAsFile = file.toFile();

            if (fileAsFile.isFile() && filePredicate(fileAsFile) && !sourceFiles.contains(fileAsFile)) {
                sourceFiles.add(fileAsFile);
            }

            return FileVisitResult.CONTINUE;
        }
    }
}