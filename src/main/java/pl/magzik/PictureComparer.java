package pl.magzik;

import pl.magzik.Structures.ImageRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class PictureComparer extends Comparer<ImageRecord> {

    // 0 -> JPEG, 1 -> PNG, 2 -> GIF(v1), 3 -> GIF(v2), 4 -> BMP, 5 -> TIFF(v1), 6 -> TIFF(v2), 7 -> ICO
    // 8 -> JPEG2000(v1) cut, 9 -> JPEG2000(v2),
    private final static byte[][] imageMagicNumbers = {
        {(byte)0xff, (byte)0xd8, (byte)0xff,},
        {(byte)0x89, (byte)0x50, (byte)0x4e, (byte)0x47, (byte)0x0d, (byte)0x0a, (byte)0x1a, (byte)0x0a,},
        {(byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x37, (byte)0x61,},
        {(byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x39, (byte)0x61,},
        {(byte)0x42, (byte)0x4d,},
        {(byte)0x49, (byte)0x49, (byte)0x2a, (byte)0x00,},
        {(byte)0x4d, (byte)0x4d, (byte)0x00, (byte)0x2a,},
        {(byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00,},
        {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0c, (byte)0x6a, (byte)0x50, (byte)0x20, (byte)0x0d,},
        {(byte)0xff, (byte)0x4f, (byte)0xff, (byte)0x51,},
    };

    public PictureComparer(List<File> sourceFiles, File sourceDirectory, File destDirectory) throws IOException {
        super(sourceFiles, sourceDirectory, destDirectory);
//        super.acceptedTypes = ImageRecord.acceptedTypes;
        super.formatMagicNumbers  = imageMagicNumbers;
        _setUp(sourceFiles, sourceDirectory, destDirectory);

        log("Picture Comparer initialized");
    }

    public PictureComparer() throws IOException {
        super();
//        super.acceptedTypes = ImageRecord.acceptedTypes;

        log("Picture Comparer initialized");
    }

    public PictureComparer(File sourceDirectory, File destDirectory) throws IOException {
        this(null, sourceDirectory, destDirectory);
    }

    public PictureComparer(List<File> sourceFiles, File destDirectory) throws IOException {
        this(sourceFiles, null, destDirectory);
    }

    @Override
    public void map() {
        // Method

        log("Mapping files.");
        HashMap<Long, ArrayList<ImageRecord>> map = new HashMap<>();

        sourceFiles.stream()
        .filter(f -> {
            try (FileInputStream fis = new FileInputStream(f)) {
                byte[] header = new byte[8];

                if(fis.read(header) == -1)
                    return false; // The file couldn't be an image if it's too small

                return isHeaderClear(header);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        })
        .forEach(
            file -> {
                try {
                    ImageRecord ir = new ImageRecord(file);
                    if(!map.containsKey(ir.getChecksum())) // calculating checksum
                        map.put(ir.getChecksum(), new ArrayList<>());
                    map.get(ir.getChecksum()).add(ir);
                    super.processedObjectCount++;
                    log(String.format("Processed %d images from %d total", processedObjectCount, totalObjectCount));
                } catch (IOException e) {
                    // skip that file
                    log(String.format("Skipping file: %s", file.getName()));
                }
            }
        );


        super.mappedObjects = map;
    }

    @Override
    public void compare() {
        log("Extracting duplicates");

        ArrayList<ImageRecord> duplicates = new ArrayList<>();

        mappedObjects.forEach(
            (k, v) -> {
                for (int i = 0; i < v.size(); i++)
                    if( i > 0 ) duplicates.add(v.get(i));
            }
        );

        super.duplicates = duplicates;
        super.duplicatesObjectCount = duplicates.size();
        log(String.format("Found %d duplicates from %d all images", duplicates.size(), totalObjectCount));
    }

    @Override
    public void move() {
        log("Moving duplicated images");
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

    private static boolean isHeaderClear(byte[] header) {
        // This method will check whether the file is corrupted
        // First step is header checking

        if(header.length != 8)
            throw new IllegalArgumentException("Header array should be 8 element array.");

        for (byte[] magicHeader : imageMagicNumbers) {
            int i = 0;
            boolean isValid = true;
            for (byte magicWord : magicHeader) {
                if(magicWord != header[i++]) isValid = false;
            }
            if(isValid) return true;
        }

        return false;
    }
}