package pl.magzik.Structures;

import pl.magzik.Algorithms.DCT;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
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
                    BufferedImage image = loadImage(record.getFile());

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
    },
            pixelByPixelFunction = list -> {
                Set<Record<BufferedImage>> processed = new CopyOnWriteArraySet<>();

                // Here not a parallel stream is needed !!!
                return list.stream()
                        .filter(r1 -> !processed.contains(r1))
                        .peek(processed::add)
                        .collect(Collectors.toConcurrentMap(
                                r1 -> r1,
                                r1 -> {
                                    BufferedImage image = loadImage(r1.getFile());
                                    return list.stream()
                                        .filter(r2 -> r1 != r2)
                                        .filter(r2 -> !processed.contains(r2))
                                        .filter(r2 -> compareImages(image, loadImage(r2.getFile())))
                                        .peek(processed::add)
                                        .map(r2 -> (Record<BufferedImage>) r2)
                                        .toList();
                                }
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

    private static BufferedImage loadImage(File file) {
        BufferedImage img;
        try {
            img = ImageIO.read(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (img == null) throw new NullPointerException("Loaded image is null. Probably unsupported file type.");
        return img;
    }

    private static boolean compareImages(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) return false;

        return IntStream.range(0, img1.getWidth()*img1.getHeight())
                .allMatch(idx -> {
                    int x = idx % img1.getWidth(),
                            y = idx / img1.getWidth();
                    return img1.getRGB(x,y) == img2.getRGB(x,y);
                });
    }
}