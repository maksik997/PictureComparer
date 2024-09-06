package pl.magzik.algorithms;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Utility class for file operations, particularly for reading image files.
 * Provides static methods to handle image file reading operations.
 * <p>
 * This class contains utility methods that assist in loading image files
 * into {@link BufferedImage} objects. It handles the conversion of file
 * formats supported by {@link ImageIO} and provides error handling for
 * unsupported file types.
 * </p>
 */
public interface FileUtils {

    /**
     * Reads an image file and returns it as a {@link BufferedImage}.
     * <p>
     * This method uses the {@link ImageIO#read(File)} method to load the image
     * from the provided file. If the file cannot be read or the image is null,
     * an {@link UncheckedIOException} is thrown.
     * </p>
     *
     * @param file the file to read the image from
     * @return the {@link BufferedImage} object loaded from the file
     * @throws UncheckedIOException if an I/O error occurs or if the image is unsupported
     * @see ImageIO#read(File)
     */
    static BufferedImage readImage(File file) {
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            if (image == null) throw new IOException("Unsupported file type.");
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException("Error loading image: " + file, e);
        }
    }
}
