package pl.magzik.algorithms;

import pl.magzik.structures.ImageRecord;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of the {@link Algorithm} interface that compares images pixel-by-pixel.
 * <p>
 * This algorithm uses a soft reference cache to store and reuse images during the comparison process.
 * Images are loaded from files and compared to find matching records.
 * </p>
 */
public class PixelByPixel implements Algorithm<ImageRecord, ImageRecord> {

    private final Map<ImageRecord, SoftReference<BufferedImage>> imageCache = new HashMap<>();

    @Override
    public Map<ImageRecord, List<ImageRecord>> apply(List<ImageRecord> group) {
        Set<ImageRecord> processed = new LinkedHashSet<>();

        return group.stream()
                .filter(record -> !processed.contains(record))
                .peek(processed::add)
                .collect(Collectors.toMap(
                    record -> record,
                    record -> process(record, processed, group)
                ));
    }

    private List<ImageRecord> process(ImageRecord record, Set<ImageRecord> processed, List<ImageRecord> group) {
        BufferedImage img = getCachedImage(record);

        List<ImageRecord> result = new ArrayList<>(
            group.stream()
                .filter(r -> record != r)
                .filter(r -> !processed.contains(r))
                .filter(r -> compareImages(img, getCachedImage(r)))
                .toList()
        );

        result.add(record);
        return result;
    }

    private BufferedImage getCachedImage(ImageRecord record) {
        SoftReference<BufferedImage> ref = imageCache.get(record);
        BufferedImage img = ref != null ? ref.get() : null;

        if (img == null) {
            img = FileUtils.readImage(record.getFile());
            imageCache.put(record, new SoftReference<>(img));
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
