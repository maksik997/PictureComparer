package pl.magzik.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A cache system that adapts its memory usage based on available JVM memory.
 * <p>
 * The cache stores images loaded from disk and automatically manages the memory usage by adjusting its size
 * based on the available heap memory. It uses the Caffeine caching library and implements the Least Recently Used (LRU)
 * eviction policy.
 * </p>
 * <p>
 * This class is designed as a singleton, ensuring that there is only one instance of the cache in the application.
 * The cache also includes a periodic monitoring system to log cache statistics.
 * </p>
 */
public class AdaptiveCache {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveCache.class);

    /**
     * The maximum percentage of JVM memory to be allocated for the cache.
     * The cache will not exceed this limit in terms of memory usage.
     */
    private static final double MAXIMUM_MEMORY_PERCENTAGE = 0.6;

    /**
     * Singleton holder for the {@link AdaptiveCache} instance.
     */
    private static final class InstanceHolder {
        private static final AdaptiveCache instance = new AdaptiveCache(getMaximumWeight());
    }

    /**
     * Retrieves the singleton instance of the {@link AdaptiveCache}.
     *
     * @return the singleton {@link AdaptiveCache} instance.
     */
    @NotNull
    public static AdaptiveCache getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Calculates the maximum memory that can be allocated for the cache based on the available JVM memory.
     * The cache will use up to 60% of the available memory.
     *
     * @return the maximum memory weight for the cache.
     */
    private static long getMaximumWeight() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        return (long) (maxMemory * MAXIMUM_MEMORY_PERCENTAGE);
    }

    private final AtomicBoolean monitorStarted = new AtomicBoolean(false);

    /**
     * The cache instance used for storing images.
     */
    private final Cache<File, BufferedImage> cache;

    /**
     * Initializes the cache with a dynamic weight limit based on available memory.
     * The cache will use up to 60% of the available JVM heap memory and will evict entries
     * using the Least Recently Used (LRU) policy.
     *
     * @param maximumWeight the maximum weight (memory) the cache can use.
     */
    private AdaptiveCache(long maximumWeight) {
        logger.info("Initialising cache memory...");
        this.cache = Caffeine.newBuilder()
            .maximumWeight(maximumWeight)
            .weigher(this::getImageWeight)
            .expireAfterAccess(1, TimeUnit.SECONDS)
            .recordStats()
            .build();
        logger.info("Cache memory initialised.");
    }

    /**
     * Calculates the weight of an image based on its dimensions (width * height * 4).
     * This assumes that each pixel is represented by 4 bytes (RGBA).
     *
     * @param key the file associated with the image.
     * @param value the image whose weight is being calculated.
     * @return the weight of the image in bytes.
     */
    private int getImageWeight(File key, @NotNull BufferedImage value) {
        return value.getWidth() * value.getHeight() * 4;
    }

    /**
     * Retrieves an image from the cache, loading it from disk if it is not present.
     * If the image is not found in the cache, it will be loaded and stored.
     *
     * @param key the file representing the image.
     * @return the buffered image from the cache.
     * @throws IOException if the image cannot be loaded from the file.
     */
    public BufferedImage get(@NotNull File key) throws IOException {
        try {
            return cache.get(key, this::loadImage);
        } catch (UncheckedIOException e) {
            logger.error("Error loading image from file: {}", key, e);
            throw new IOException("Error loading image from file: " + key, e);
        }
    }

    /**
     * Starts a periodic monitoring system that logs cache statistics at a specified interval.
     *
     * @param period the period (in seconds) between cache status logs.
     */
    public void monitor(long period) {
        if (monitorStarted.compareAndSet(false, true)) {
            Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> logger.info("Cache status: {}", cache.stats()), 0, period, TimeUnit.SECONDS);
        }
    }

    /**
     * Loads an image from disk.
     *
     * @param key the file representing the image.
     * @return the loaded buffered image.
     * @throws UncheckedIOException if an error occurs while reading the image.
     */
    private BufferedImage loadImage(@NotNull File key) {
        try {
            BufferedImage image = ImageIO.read(key);
            if (image == null) throw new IOException("Unsupported file type for file: " + key);
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
