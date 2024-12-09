import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import pl.magzik.Processor;
import pl.magzik.algorithms.Algorithm;
import pl.magzik.grouping.Grouper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the {@link Processor} class.
 * <p>
 * This class contains unit tests for the {@link Processor} class, specifically for the {@code process} method,
 * which processes a collection of files to identify and group duplicated files based on a multistep workflow.
 * The tests ensure that the method handles various input scenarios correctly, including valid inputs, empty sets,
 * null inputs, and performance considerations.
 * </p>
 */
public class ProcessorTest {

    @Mock
    private Grouper grouper;

    @Mock
    private Algorithm<Object> algorithm;

    private Processor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Set<Algorithm<?>> algorithms = new HashSet<>();
        algorithms.add(algorithm);
        processor = new Processor(grouper, algorithms);
    }

    /**
     * Test for the `process` method with a valid input set of files.
     * <p>
     * This test ensures that the {@code process} method works as expected when given a valid set of files.
     * The method should divide the files into groups based on the chosen distinction predicate and apply the
     * algorithms correctly.
     * </p>
     */
    @Test
    void testProcess_withValidInput() throws IOException {
        File file1 = new File("file1.txt");
        File file2 = new File("file2.txt");
        File file3 = new File("file3.txt");

        Set<File> files = new HashSet<>(Arrays.asList(file1, file2, file3));

        Set<Set<File>> groupedFiles = new HashSet<>();
        groupedFiles.add(new HashSet<>(Arrays.asList(file1, file2)));
        groupedFiles.add(new HashSet<>(Collections.singletonList(file3)));

        Mockito.when(grouper.divide(ArgumentMatchers.anyCollection()))
                .thenReturn(groupedFiles);

        Map<Object, Set<File>> algorithmOutput = new HashMap<>();
        algorithmOutput.put("someKey", new HashSet<>(Arrays.asList(file1, file2)));
        Mockito.when(algorithm.apply(ArgumentMatchers.anySet()))
                .thenReturn(algorithmOutput);

        Map<File, Set<File>> result = processor.process(files);

        assertNotNull(result);
        assertEquals(1, result.size(), "Result should contain only one group.");

        assertTrue(result.containsKey(file1) || result.containsKey(file2), "Result should contain any of the files as the original.");

        Set<File> group = result.values().iterator().next();
        assertTrue(group.contains(file1) || result.containsKey(file1), "'file1' should be in the result group or 'file1' is the key.");
        assertTrue(group.contains(file2) || result.containsKey(file2), "'file2' should be in the result group  or 'file2' is the key.");
    }

    /**
     * Test for the `process` method with an empty set of files.
     * <p>
     * This test verifies that when an empty set of files is passed to the {@code process} method,
     * the result should not be null and should be an empty map.
     * </p>
     */
    @Test
    void testProcess_withEmptyFilesSet() throws IOException {
        Set<File> files = Collections.emptySet();

        Mockito.when(grouper.divide(ArgumentMatchers.anyCollection()))
                .thenReturn(Collections.emptySet());

        Map<File, Set<File>> result = processor.process(files);

        assertNotNull(result, "Should not return null");
        assertTrue(result.isEmpty(), "Should return empty result");
    }

    @Test
    void testProcess_withNullFiles() {
        assertThrows(IllegalArgumentException.class, () -> processor.process(null), "Should throw exception");
    }

    /**
     * Test for the `process` method when the input set contains null values.
     * <p>
     * This test ensures that the {@code process} method throws a {@link NullPointerException}
     * when any file in the input set is null.
     * </p>
     */
    @Test
    void testProcess_withNullFileInInput() {
        Set<File> files = new HashSet<>(Arrays.asList(new File("file1.txt"), null));

        assertThrows(NullPointerException.class, () -> processor.process(files),
                "Should throw exception");
    }

    /**
     * Test for the private method {@code algorithmsApplication} using reflection.
     * <p>
     * This test ensures that the algorithms are correctly applied to the grouped files.
     * The method should process the groups and return the correct result.
     * </p>
     */
    @Test
    void testAlgorithmsApplication() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Set<Set<File>> groupedFiles = new HashSet<>();
        File file1 = new File("file1.txt");
        File file2 = new File("file2.txt");
        groupedFiles.add(new HashSet<>(Arrays.asList(file1, file2)));

        Map<Object, Set<File>> algorithmOutput = new HashMap<>();
        algorithmOutput.put("someKey", new HashSet<>(Arrays.asList(file1, file2)));

        Mockito.when(algorithm.apply(ArgumentMatchers.anySet()))
                .thenReturn(algorithmOutput);

        Method method = Processor.class.getDeclaredMethod("algorithmsApplication", Set.class);
        method.setAccessible(true);

        Set<Set<File>> result = (Set<Set<File>>) method.invoke(processor, groupedFiles);

        assertNotNull(result);
        assertEquals(1, result.size(), "Should have one group");
        assertTrue(result.stream().anyMatch(group -> group.contains(file1)), "Group should contain file1");
    }

    /**
     * Test for the private method {@code postAlgorithmConsolidation} using reflection.
     * <p>
     * This test verifies that after applying algorithms, the resulting file groups are properly consolidated
     * into a single group, and redundant groups are merged.
     * </p>
     */
    @Test
    void testPostAlgorithmConsolidation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Map<Object, Set<File>> algorithmOutput = new HashMap<>();
        Set<File> fileGroup1 = new HashSet<>(Arrays.asList(new File("file1.txt"), new File("file2.txt")));
        Set<File> fileGroup2 = new HashSet<>(Collections.singletonList(new File("file3.txt")));
        algorithmOutput.put("someKey", fileGroup1);
        algorithmOutput.put("otherKey", fileGroup2);

        Method method = Processor.class.getDeclaredMethod("postAlgorithmConsolidation", Map.class);
        method.setAccessible(true);

        Set<Set<File>> consolidated = (Set<Set<File>>) method.invoke(processor, algorithmOutput);

        assertEquals(1, consolidated.size(), "Should return only one group");
    }

    /**
     * Test for the private method {@code originalDistinction} using reflection.
     * <p>
     * This test ensures that the final groups of files are correctly reorganized by the {@code originalDistinction}
     * method, with the first file in each group being considered the original.
     * </p>
     */
    @Test
    void testOriginalDistinction() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Set<File> group1 = new HashSet<>(Arrays.asList(new File("file1.txt"), new File("file2.txt")));
        Set<File> group2 = new HashSet<>(Arrays.asList(new File("file3.txt"), new File("file4.txt")));

        Set<Set<File>> groupedFiles = new HashSet<>(Arrays.asList(group1, group2));

        Method method = Processor.class.getDeclaredMethod("originalDistinction", Set.class);
        method.setAccessible(true);

        Map<File, Set<File>> originalFiles = (Map<File ,Set<File>>) method.invoke(processor, groupedFiles);

        assertEquals(2, originalFiles.size(), "Should return 2 groups");
        assertTrue(originalFiles.containsKey(new File("file1.txt")) || originalFiles.containsKey(new File("file2.txt")) , "Group should contain 'file1' or `file2` as original");
        assertTrue(originalFiles.containsKey(new File("file3.txt")) || originalFiles.containsKey(new File("file4.txt")), "Group should contain 'file3' or `file4` as original");
    }

    /**
     * Performance test for the `process` method with a large input set.
     * <p>
     * This test verifies that the {@code process} method completes within a reasonable amount of time
     * (less than 1 second) when processing a large set of files.
     * </p>
     */
    @Test
    void testProcess_withPerformance() throws IOException {
        Set<File> files = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            files.add(new File("file" + i + ".jpg"));
        }

        long startTime = System.nanoTime();
        processor.process(files);
        long duration = System.nanoTime() - startTime;

        assertTrue(duration < 1000000000, "Process should complete in less than 1 second");
    }

}
