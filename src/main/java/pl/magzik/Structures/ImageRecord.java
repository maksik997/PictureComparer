package pl.magzik.Structures;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageRecord extends Record<BufferedImage> {

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
    protected void calculateAndSetChecksum(BufferedImage e) {
        String[] fileNameArr = super.getFile().getName().split("\\.");
        String extension = fileNameArr[fileNameArr.length-1];

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            ImageIO.write(e, extension, byteStream);
            Record.algorithm.update(byteStream.toByteArray());
        } catch (IOException ex) {
            // skip the file and set checksum = 0
            System.out.println(ex.getMessage());
            setChecksum(0L);
            return;
        }

        super.setChecksum(Record.algorithm.getValue());
        Record.algorithm.reset();
    }
}
