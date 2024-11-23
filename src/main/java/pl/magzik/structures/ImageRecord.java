package pl.magzik.structures;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.CRC32;

/**
 * Represents an image record that extends the {@link Record} class and provides image-related functionalities.
 * <p>
 * This class allows for loading images from files, computing checksums, and creating {@link ImageRecord}
 * objects based on image files.
 * </p>
 */
@Deprecated
public class ImageRecord extends Record<BufferedImage> {

    /**
     * Creates a new image record from a file.
     *
     * @param file the image file
     * @throws IOException if an error occurs while reading the file
     */
    public ImageRecord(File file) throws IOException {
        super(file);
    }

    /**
     * Creates a copy of an existing image record.
     *
     * @param r the existing image record
     * @throws IOException if an error occurs while reading the file
     */
    public ImageRecord(ImageRecord r) throws IOException {
        this(r.getFile());
    }

    @Override
    public String toString() {
        return "ImageRecord{" +
                super.toString() +
                "}";
    }

    /**
     * Computes the checksum (CRC32) for the image stored in the file.
     *
     * @param f the image file
     * @return the checksum of the image
     * @throws IOException if an error occurs while reading the image from the file or computing the checksum
     */
    @Override
    protected long createChecksum(File f) throws IOException {
        BufferedImage img = ImageIO.read(f);
        if (img == null)
            throw new IOException("Failed to read image from file: " + f);

        String extension = getExtension(f);

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            ImageIO.write(img, extension, byteStream);
            byte[] imageBytes = byteStream.toByteArray();

            CRC32 crc = new CRC32();
            crc.update(imageBytes);
            return crc.getValue();
        }
    }



    /**
     * Creates a new {@link ImageRecord} object based on the given file.
     *
     * @param file the image file
     * @return a new {@link ImageRecord} object
     * @throws UncheckedIOException if an error occurs while reading the file
     */
    public static ImageRecord create(File file) {
        try {
            return new ImageRecord(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}