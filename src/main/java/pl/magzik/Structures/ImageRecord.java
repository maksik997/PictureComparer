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

    public static HashMap<Long, ArrayList<ImageRecord>> map(File dir) throws IOException {
        if (dir == null )
            return null;

        if(!dir.isDirectory())
            throw new IOException();

        StringBuilder types = new StringBuilder();
        Arrays.stream(acceptedTypes).forEach(
            e -> types.append(".*\\.").append(e).append("$|")
        );
        String pattern = types.toString();

        File[] files = dir.listFiles(File::isFile);
        if(files == null)
            return null;

        HashMap<Long, ArrayList<ImageRecord>> images = new HashMap<>();

        Arrays.stream(files)
                .filter(file -> file.getName().matches(pattern))
                .forEach(
                    file -> {
                        try {
                            ImageRecord ir = new ImageRecord(file);
                            if(!images.containsKey(ir.getChecksum()))
                                images.put(ir.getChecksum(), new ArrayList<>());
                            images.get(ir.getChecksum()).add(ir);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                );

        return images;
    }

    public static ArrayList<ImageRecord> findDuplicates(HashMap<Long, ArrayList<ImageRecord>> images){
        ArrayList<ImageRecord> duplicates = new ArrayList<>();

        images.forEach(
            (k, v) -> {
                for (int i = 0; i < v.size(); i++)
                    if( i > 0 ) duplicates.add(v.get(i));
            }
        );

        return duplicates;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(Arrays.stream(ImageIO.getReaderFileSuffixes()).toList());

        System.out.println(ImageRecord.map(new File("C:\\Users\\maksy\\OneDrive\\Pulpit\\ThousandPictureComapre\\data\\testingData")));

        System.out.println(findDuplicates(ImageRecord.map(new File("C:\\Users\\maksy\\OneDrive\\Pulpit\\ThousandPictureComapre\\data\\testingData"))));
    }

}
