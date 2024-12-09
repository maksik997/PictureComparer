import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.magzik.algorithms.PixelByPixel;
import pl.magzik.cache.AdaptiveCache;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PixelByPixelAlgorithmTest {

    private PixelByPixel algorithm;
    private AdaptiveCache mockCache;

    @BeforeEach
    void setUp() {
        algorithm = new PixelByPixel();
        mockCache = mock(AdaptiveCache.class);

        AdaptiveCache.setInstance(mockCache);
    }

    @AfterEach
    void tearDown() {
        AdaptiveCache.setInstance(null);
    }


    @Test
    void testApply_withIdenticalImages_shouldGroupTogether() throws IOException {
        File image1 = Files.createTempFile("image1", ".png").toFile();
        File image2 = Files.createTempFile("image2", ".png").toFile();

        BufferedImage identicalImage = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);

        when(mockCache.get(image1)).thenReturn(identicalImage);
        when(mockCache.get(image2)).thenReturn(identicalImage);

        Set<File> input = new HashSet<>(Arrays.asList(image1, image2));
        Map<File, Set<File>> result = algorithm.apply(input);

        assertEquals(1, result.size(), "Should return one image group");
        assertTrue(result.values().stream().anyMatch(s -> s.containsAll(input)));
        assertTrue(result.keySet().stream().anyMatch(f -> f.equals(image1) || f.equals(image2)));
    }

    @Test
    void testApply_withDifferentImages_shouldNotGroup() throws IOException {
        File image1 = Files.createTempFile("image1", ".png").toFile();
        File image2 = Files.createTempFile("image2", ".png").toFile();

        BufferedImage imageA = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage imageB = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
        imageB.setRGB(0, 0, 0xFFFFFF);

        when(mockCache.get(image1)).thenReturn(imageA);
        when(mockCache.get(image2)).thenReturn(imageB);

        Set<File> input = new HashSet<>(Arrays.asList(image1, image2));
        Map<File, Set<File>> result = algorithm.apply(input);

        assertEquals(2, result.size(), "Should return two image groups");
        assertNotSame(result.get(image1), result.get(image2), "Images should be in different groups");
    }

    @Test
    void testApply_withImagesOfDifferentSizes_shouldNotGroup() throws IOException {
        File image1 = Files.createTempFile("image1", ".png").toFile();
        File image2 = Files.createTempFile("image2", ".png").toFile();

        BufferedImage imageA = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage imageB = new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY);

        when(mockCache.get(image1)).thenReturn(imageA);
        when(mockCache.get(image2)).thenReturn(imageB);

        Set<File> input = new HashSet<>(Arrays.asList(image1, image2));
        Map<File, Set<File>> result = algorithm.apply(input);

        assertEquals(2, result.size(), "Should return two image groups due to different sizes");
        assertNotEquals(result.get(image1), result.get(image2), "Images with different sizes should not be grouped together");
    }

}
