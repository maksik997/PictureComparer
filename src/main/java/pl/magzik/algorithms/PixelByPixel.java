package pl.magzik.algorithms;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link Algorithm} interface that compares images pixel-by-pixel.
 * <p>
 * This algorithm uses a soft reference cache to store and reuse images during the comparison process.
 * Images are loaded from files and compared to find matching records.
 * </p>
 */
public class PixelByPixel implements Algorithm<BufferedImage> {

    private final Map<File, SoftReference<BufferedImage>> imageCache = new HashMap<>();

    @Override
    public Map<BufferedImage, Set<File>> apply(Set<File> group) {
        Set<File> processed = new LinkedHashSet<>();

        return group.stream()
                .filter(f -> !processed.contains(f))
                .peek(processed::add)
                .collect(Collectors.toMap(
                    this::getCachedImage,
                    f -> process(f, processed, group)
                ));
    }

    private Set<File> process(File file, Set<File> processed, Set<File> group) {
        BufferedImage img = getCachedImage(file);

        Set<File> result = group.stream()
            .filter(r -> file != r)
            .filter(r -> !processed.contains(r))
            .filter(r -> compareImages(img, getCachedImage(r)))
            .collect(Collectors.toSet());

        result.add(file);
        return result;
    }

    private BufferedImage getCachedImage(File file) {
        SoftReference<BufferedImage> ref = imageCache.get(file);
        BufferedImage img = ref != null ? ref.get() : null;

        if (img == null) {
            img = FileUtils.readImage(file);
            imageCache.put(file, new SoftReference<>(img));
        }

        return img;
    }

    private boolean compareImages(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight())
            return false;

        Raster raster1 = img1.getRaster();
        Raster raster2 = img2.getRaster();

        DataBufferByte dataBuffer1 = (DataBufferByte) raster1.getDataBuffer();
        DataBufferByte dataBuffer2 = (DataBufferByte) raster2.getDataBuffer();

        byte[] data1 = dataBuffer1.getData();
        byte[] data2 = dataBuffer2.getData();

        return Arrays.equals(data1, data2);
    }
}
