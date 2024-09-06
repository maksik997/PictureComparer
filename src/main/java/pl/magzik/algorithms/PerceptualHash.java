package pl.magzik.algorithms;

import pl.magzik.algorithms.math.DCT;
import pl.magzik.structures.ImageRecord;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of the {@link Algorithm} interface that computes perceptual hashes for images.
 * <p>
 * This algorithm resizes images, extracts samples, applies Discrete Cosine Transform (DCT),
 * and generates hashes. Images are grouped based on their hashes.
 * </p>
 */
public class PerceptualHash implements Algorithm<String, ImageRecord> {

    private static final int WIDTH = 8, HEIGHT = 8;

    @Override
    public Map<String, List<ImageRecord>> apply(List<ImageRecord> group) {
        List<String> hashes = group.stream()
                                    .map(this::resize)
                                    .map(this::extractSample)
                                    .map(DCT::apply)
                                    .map(this::buildHash)
                                    .toList();

        if (hashes.stream().anyMatch(Objects::isNull))
            throw new NullPointerException("Some hashes are null.");

        return IntStream.range(0, group.size())
                .boxed()
                .collect(Collectors.groupingBy(
                    hashes::get,
                    Collectors.mapping(group::get, Collectors.toList())
                )).entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private BufferedImage resize(ImageRecord record) {
        BufferedImage image = FileUtils.readImage(record.getFile());

        BufferedImage resizedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, WIDTH, HEIGHT, null);
        g.dispose();
        return resizedImage;
    }

    private double[][] extractSample(BufferedImage image) {
        double[][] sample = new double[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                sample[x][y] = image.getRaster().getSampleDouble(x, y, 0);
            }
        }
        return sample;
    }

    private String buildHash(double[][] matrix) {
        double avg = getAvg(matrix);
        return Arrays.stream(matrix)
                .flatMapToDouble(Arrays::stream)
                .mapToObj(value -> value > avg ? "1" : "0")
                .collect(Collectors.joining());
    }

    private double getAvg(double[][] matrix) {
        return Arrays.stream(matrix)
                .flatMapToDouble(Arrays::stream)
                .average()
                .orElse(0.0);
    }
}
