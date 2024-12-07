import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.magzik.algorithms.PerceptualHash;
import pl.magzik.cache.AdaptiveCache;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PerceptualHashAlgorithmTest {

    private PerceptualHash algorithm;
    private AdaptiveCache mockCache;

    @BeforeEach
    void setUp() {
        algorithm = new PerceptualHash();
        mockCache = mock(AdaptiveCache.class);

        AdaptiveCache.setInstance(mockCache);
    }

    @Test
    void testApply_withIdenticalImages_shouldGroupTogether() throws IOException {
        File image1 = Files.createTempFile("image1", ".png").toFile();
        File image2 = Files.createTempFile("image2", ".png").toFile();

        BufferedImage identicalImage = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);

        when(mockCache.get(image1)).thenReturn(identicalImage);
        when(mockCache.get(image2)).thenReturn(identicalImage);

        Set<File> input = new HashSet<>(Arrays.asList(image1, image2));
        Map<String, Set<File>> result = algorithm.apply(input);

        assertEquals(1, result.size(), "Should return one hash group");
        assertTrue(result.values().stream().anyMatch(s -> s.containsAll(input)));
    }

    @Test
    void testApply_withNullHashes_shouldThrowException() throws IOException {
        File badImage = new File("nonexistent.png");
        when(mockCache.get(badImage)).thenThrow(new IOException("File not found"));

        Set<File> input = Set.of(badImage);

        assertThrows(UncheckedIOException.class, () -> algorithm.apply(input));
    }

    @Test
    void testApply_withDifferentImagesButSameHashes_shouldGroupTogether() throws IOException {
        File image1 = Files.createTempFile("image1", ".png").toFile();
        File image2 = Files.createTempFile("image2", ".png").toFile();

        BufferedImage image1Content = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage image2Content = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);

        when(mockCache.get(image1)).thenReturn(image1Content);
        when(mockCache.get(image2)).thenReturn(image2Content);

        Set<File> input = new HashSet<>(Arrays.asList(image1, image2));
        Map<String, Set<File>> result = algorithm.apply(input);

        assertEquals(1, result.size(), "Should return one hash group");
        assertTrue(result.values().stream().anyMatch(s -> s.containsAll(input)),
                "Both images should be in the same group");
    }

    @Test
    void testApply_withEmptySet_shouldReturnEmptyMap() {
        Set<File> input = Collections.emptySet();
        Map<String, Set<File>> result = algorithm.apply(input);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be an empty map");
    }

    @Test
    void testApply_withLargeSet_shouldGroupCorrectly() throws IOException {
        Set<File> input = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            File file = Files.createTempFile("image" + i, ".png").toFile();
            input.add(file);
            BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
            when(mockCache.get(file)).thenReturn(image);
        }

        Map<String, Set<File>> result = algorithm.apply(input);
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Result should contain at least one group");
    }

}
