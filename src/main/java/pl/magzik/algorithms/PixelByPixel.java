package pl.magzik.algorithms;

import org.jetbrains.annotations.NotNull;
import pl.magzik.cache.AdaptiveCache;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * A pixel-by-pixel comparison algorithm for image matching.
 * <p>
 * This implementation compares images from a given group of files, pixel-by-pixel, and groups those that are identical.
 * The comparison is based on exact pixel-level equivalence between two images, where the images are first loaded from
 * disk into memory using a cache to optimize repeated access.
 * </p>
 *
 * <p>
 * The algorithm uses an {@link AdaptiveCache} to store and retrieve images during the comparison process, improving
 * performance by avoiding reloading images from disk for every comparison.
 * It performs a depth-first search-like operation by iterating through the image group, selecting a 'key' image and
 * comparing it against the rest of the group, grouping identical images together.
 * The comparison operation is parallelized to speed up the process when checking multiple images.
 * </p>
 *
 * <p>
 * This algorithm performs the following steps:
 * <ol>
 *     <li>Load images from the disk or cache them if previously loaded.</li>
 *     <li>For each image in the group, compare it with all other images in the group pixel-by-pixel.</li>
 *     <li>Group images that are identical based on the pixel comparison.</li>
 *     <li>Store results in a map where the key is the original image and the value is the set of matching images.</li>
 * </ol>
 * </p>
 *
 * <p>
 * The image comparison involves checking the image dimensions and comparing the raw byte data of the image's raster.
 * The algorithm assumes that the images in the group are of the same format and resolution.
 * </p>
 *
 * <p>
 * The algorithm also uses a {@link ConcurrentLinkedQueue} to handle the group of images concurrently. This allows multiple
 * threads to process the images without blocking, speeding up the matching process, especially for large image datasets.
 * </p>
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 *     Set<File> imageFiles = new HashSet<>(Arrays.asList(file1, file2, file3));
 *     PixelByPixel algorithm = new PixelByPixel();
 *     Map<File, Set<File>> result = algorithm.apply(imageFiles);
 * }</pre>
 *
 * @see AdaptiveCache
 * @see ConcurrentLinkedQueue
 */
public class PixelByPixel implements Algorithm<File> {

    @Override
    public Map<File, Set<File>> apply(Set<File> group) {
        Map<File, Set<File>> result = new HashMap<>();
        Queue<File> groupQueue = new ConcurrentLinkedQueue<>(group);

        while (!groupQueue.isEmpty()) {
            process(result, groupQueue);
        }

        return result;
    }

    /**
     * Processes a queue of image files and groups identical images based on pixel-by-pixel comparison.
     * <p>
     * This method removes a file from the queue and compares it with all other files in the queue.
     * Identical images are removed from the queue and grouped together.
     * </p>
     *
     * @param result The map where the results (groups of identical images) will be stored. The map must be mutable.
     * @param groupQueue The queue of image files to be processed. The queue must be mutable.
     */
    private void process(@NotNull Map<File, Set<File>> result, @NotNull Queue<File> groupQueue) {
        File key = groupQueue.remove();
        Set<File> values = groupQueue.parallelStream()
            .filter(v -> compareImages(getCachedImage(key), getCachedImage(v)))
            .collect(Collectors.toSet());

        groupQueue.removeAll(values);
        result.put(key, values);

    }

    /**
     * Retrieves an image from the cache or loads it from the disk if not already cached.
     * <p>
     * This method calls the {@link AdaptiveCache} to get the image. If the image is not found in the cache,
     * it will be loaded from disk and added to the cache.
     * </p>
     *
     * @param file The image file to retrieve.
     * @return The {@link BufferedImage} corresponding to the file.
     * @throws UncheckedIOException If the image cannot be loaded due to IO errors.
     */
    private BufferedImage getCachedImage(@NotNull File file) {
        BufferedImage img;
        try {
            img = AdaptiveCache.getInstance().get(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return img;
    }

    /**
     * Compares two images pixel-by-pixel to determine if they are identical.
     * <p>
     * This method first compares the dimensions of the two images. If the dimensions are not the same,
     * it returns {@code false}. If the dimensions are the same, it compares the raw byte data of the images' raster
     * to check if the pixels are identical.
     * </p>
     *
     * @param img1 The first image to compare.
     * @param img2 The second image to compare.
     * @return {@code true} if the images are identical pixel-by-pixel, otherwise {@code false}.
     */
    private boolean compareImages(@NotNull BufferedImage img1, @NotNull BufferedImage img2) {
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
