package pl.magzik.Structures;

import pl.magzik.Structures.Utils.Checksum;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ImageRecord extends Record<BufferedImage> {
    public final static String[] acceptedTypes = ImageIO.getReaderFileSuffixes();

    public ImageRecord(File file) throws IOException {
        super(file);
        calculateAndSetChecksum(ImageIO.read(file));
    }

    public ImageRecord(ImageRecord r) throws IOException {
        this(r.getFile());
    }

    @Override
    public String toString() {
        return "ImageRecord{" +
                super.toString() +
                "}";
    }

    @Override
    public void calculateAndSetChecksum(BufferedImage e) {
        String[] fileNameArr = super.getFile().getName().split("\\.");
        String extension = fileNameArr[fileNameArr.length-1];

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try {
            ImageIO.write(e, extension, byteStream);
            Checksum.algorithm.update(byteStream.toByteArray());
            byteStream.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        super.setChecksum(Checksum.algorithm.getValue());
        Checksum.algorithm.reset();
    }
}
