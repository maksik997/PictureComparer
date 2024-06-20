package pl.magzik.Structures;

import pl.magzik.Algorithms.DCT;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImageRecord extends Record<BufferedImage> {

    public final static Function<List<? extends Record<BufferedImage>>, Map<? ,List<Record<BufferedImage>>>> pHashFunction = list -> {
        /*
        * This Function will take List of some Records and will apply to it fast dct algorithm.
        * The Function will output Map of duplicates grouped by hash created from dct.
        * The output Map will contain lists of elements with the same hash, but without the first found element.
        * This function implements pHash comparing which will surely leave some true negatives.
        * Outputted collection should be then compared with some sequential algorithm.
        * However, to find similar images, this function is great.
        * Yay!!
        * This Function can throw NullPointerException
        * if a loaded image is null or if any of the created hashes is null.
        * This Function can throw UncheckedIOException when IOException was thrown.
        */
        int w = 64, h = 64;

        List<String> hashes = list.stream()
            .map(record -> { // Resizing to w, h
                BufferedImage image;
                try {
                    image = ImageIO.read(record.getFile());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (image == null) throw new NullPointerException("Loaded image is null. Probably unsupported file type.");

                BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g = resized.createGraphics();
                g.drawImage(image, 0, 0, w, h, null);
                g.dispose();
                return resized;
            })
            .map(img -> { // Pixel sample extraction
                double[][] samples = new double[w][h];
                for (int x = 0; x < w; x++)
                    for (int y = 0; y < h; y++)
                        samples[x][y] = img.getRaster().getSampleDouble(x, y, 0);
                return samples;
            })
            .map(samples -> DCT.quantization(DCT.transform(samples))) // Fast DCT Lee algorithm
            .map(dct -> {
                double avg = Arrays.stream(dct).mapToDouble(a -> Arrays.stream(a).sum()).sum()
                        / (dct.length + dct[0].length);
                StringBuilder hash = new StringBuilder();
                Arrays.stream(dct)
                .forEach(d -> Arrays.stream(d).forEach(val -> hash.append(val > avg ? "1" : "0")));
                return hash.toString();
            })
            .toList();

        if (hashes.stream().anyMatch(Objects::isNull))
            throw new NullPointerException("Some hashes are null.");

        return IntStream.range(0, list.size())
                .boxed()
                .collect(Collectors.groupingBy(
                    hashes::get,
                    Collectors.mapping(
                            i -> (Record<BufferedImage>) list.get(i),
                            Collectors.toList()
                )))
                .entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
    };

    public ImageRecord(File file) throws IOException {
        super(file);
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
    protected long createChecksum(File f) throws IOException {
        BufferedImage img = ImageIO.read(f);

        int idx = f.getName().lastIndexOf('.');
        String extension = f.getName().substring(idx+1);

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            ImageIO.write(img, extension, byteStream);
            Record.algorithm.update(byteStream.toByteArray());
        } catch (IOException ex) {
            log(ex, "Couldn't create checksum.");
            return 0L;
        }

        long v = Record.algorithm.getValue();
        Record.algorithm.reset();
        return v;
    }


}
