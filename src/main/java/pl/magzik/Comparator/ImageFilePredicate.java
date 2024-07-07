package pl.magzik.Comparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

public class ImageFilePredicate implements FilePredicate {

    private final static Map<String, List<String>> MAGIC_NUMBERS = new HashMap<>();
    static {
        MAGIC_NUMBERS.put("JPG", List.of("FFD8FF"));
        MAGIC_NUMBERS.put("JPEG", List.of("FFD8FF"));
        MAGIC_NUMBERS.put("PNG", List.of("89504E470D0A1A0A"));
        MAGIC_NUMBERS.put("GIF", List.of("474946383761", "474946383961"));
        MAGIC_NUMBERS.put("BMP", List.of("424D"));
        MAGIC_NUMBERS.put("TIFF", List.of("49492A00", "4D4D002A"));
        MAGIC_NUMBERS.put("ICO", List.of("00000100"));
        MAGIC_NUMBERS.put("JP2", List.of("0000000C6A5020200D0A870A"));
        MAGIC_NUMBERS.put("J2K", List.of("FF4FFF51"));
        MAGIC_NUMBERS.put("JPC", List.of("FF4FFF51"));
    }

    @Override
    public boolean test(File file) throws IOException {
        String ext = getExtension(file);
        if (!MAGIC_NUMBERS.containsKey(ext)) return false;

        try {
            return MAGIC_NUMBERS.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(ext))
                .map(Map.Entry::getValue)
                .anyMatch(list -> {
                    for (String expectedMagicNumber : list) {
                        String fileMagicNumber;
                        try {
                            fileMagicNumber = getFileMagicNumber(file, expectedMagicNumber.length() / 2);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        if (fileMagicNumber.equalsIgnoreCase(expectedMagicNumber)) return true;
                    }
                    return false;
                });
        } catch (UncheckedIOException e) {
            throw new IOException(e);
        }
    }

    private static String getExtension(File file) {
        int idx = file.getName().lastIndexOf('.');
        return idx == -1 ? "" : file.getName().substring(idx+1).toUpperCase();
    }

    private static String getFileMagicNumber(File file, int bytesToRead) throws IOException {
        try (FileInputStream is = new FileInputStream(file)) {
            byte[] bytes = new byte[bytesToRead];
            if (is.read(bytes) == -1) throw new IOException(String.format("File %s is corrupted", file.getName()));

            StringBuilder magicNumber = new StringBuilder();
            for (byte b : bytes) {
                magicNumber.append(String.format("%02X", b));
            }

            return magicNumber.toString();
        }
    }
}
