package pl.magzik.predicates;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A predicate for validating image files based on their magic numbers.
 * <p>
 * This class checks if a file matches known magic numbers for specific image formats.
 * Supported formats include JPG, JPEG, PNG, GIF, BMP, TIFF, ICO, JP2, J2K, and JPC.
 * </p>
 * <p>
 * Magic numbers are used to identify or validate file types based on their binary signatures.
 * This class uses these signatures to verify that a file is of the expected type.
 * </p>
 */
public class ImageFilePredicate implements FilePredicate {

    private final Map<String, Set<String>> magicNumbers;

    /**
     * Constructs an {@code ImageFilePredicate} with a specific map of magic numbers.
     *
     * @param magicNumbers a map of file extensions to their corresponding magic numbers.
     *                     The map is immutable and should not be modified after creation.
     */
    public ImageFilePredicate(Map<String, Set<String>> magicNumbers) {
        this.magicNumbers = Collections.unmodifiableMap(magicNumbers);
    }

    /**
     * Constructs an {@code ImageFilePredicate} with default magic numbers for common image formats.
     * The default magic numbers include signatures for JPG, JPEG, PNG, GIF, BMP, TIFF, ICO, JP2, J2K, and JPC.
     */
    public ImageFilePredicate() {
        this(Map.of(
            "JPG", Set.of("FFD8FF"),
            "JPEG", Set.of("FFD8FF"),
            "PNG", Set.of("89504E470D0A1A0A"),
            "GIF", Set.of("474946383761", "474946383961"),
            "BMP", Set.of("424D"),
            "TIFF", Set.of("49492A00", "4D4D002A"),
            "ICO", Set.of("00000100"),
            "JP2", Set.of("0000000C6A5020200D0A870A"),
            "J2K", Set.of("FF4FFF51"),
            "JPC", Set.of("FF4FFF51")
        ));
    }

    /**
     * Tests if the given file is a valid image based on its extension and magic number.
     *
     * @param file the file to be tested.
     * @return {@code true} if the file matches one of the known magic numbers for its extension;
     *         {@code false} otherwise.
     * @throws IOException if an I/O error occurs while reading the file.
     */
    @Override
    public boolean test(File file) throws IOException {
        String ext = getExtension(file);
        if (!magicNumbers.containsKey(ext)) return false;

        Set<String> expectedMagicNumbers = magicNumbers.get(ext);
        for (String expected : expectedMagicNumbers) {
            String fileMagicNumber = getFileMagicNumber(file, expected.length() / 2);
            if (fileMagicNumber.equalsIgnoreCase(expected)) return true;
        }

        return false;
    }

    /**
     * Extracts the file extension from the given file.
     *
     * @param file the file from which the extension is to be extracted.
     * @return the file extension in uppercase; returns an empty string if no extension is present.
     */
    private String getExtension(File file) {
        String name = file.getName();
        int idx = name.lastIndexOf('.');
        return idx == -1 ? "" : name.substring(idx + 1).toUpperCase();
    }

    /**
     * Reads the magic number from the beginning of the file.
     *
     * @param file the file from which the magic number is to be read.
     * @param bytesToRead the number of bytes to read to get the magic number.
     * @return the magic number as a hexadecimal string.
     * @throws IOException if an I/O error occurs while reading the file.
     */
    private String getFileMagicNumber(File file, int bytesToRead) throws IOException {
        try (FileInputStream is = new FileInputStream(file)) {
            byte[] bytes = new byte[bytesToRead];
            int bytesRead = is.read(bytes);
            if (bytesRead < bytesToRead)
                throw new IOException(String.format("File %s is corrupted", file.getName()));

            return bytesToHex(bytes);
        }
    }

    /**
     * Converts an array of bytes to a hexadecimal string representation.
     *
     * @param bytes the array of bytes to be converted.
     * @return the hexadecimal string representation of the bytes.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
}
