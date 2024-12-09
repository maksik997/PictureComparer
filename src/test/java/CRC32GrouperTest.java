import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.magzik.grouping.CRC32Grouper;
import pl.magzik.io.FileOperator;
import pl.magzik.predicates.ImageFilePredicate;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CRC32GrouperTest {

    private static final String FILE_DIR = "src/test/files";
    private static final String FILE_1 = "src/test/files/a-picture.png";
    private static final String FILE_2 = "src/test/files/nave-7741260_960_720.jpg";
    private static final String FILE_2_COPY = "src/test/files/nave-7741260_960_720-copy.jpg";
    private static final String FILE_3 = "src/test/files/women-7341444_960_720.jpg";

    private static List<File> images;

    private static File image1;
    private static File image2;
    private static File image2Copy;
    private static File image3;

    @BeforeAll
    public static void setup() throws IOException {
        FileOperator fo = new FileOperator(new ImageFilePredicate(), 2);
        images = fo.load(new File(FILE_DIR));

        image1 = fo.load(new File(FILE_1)).getFirst();
        image2 = fo.load(new File(FILE_2)).getFirst();
        image2Copy = fo.load(new File(FILE_2_COPY)).getFirst();
        image3 = fo.load(new File(FILE_3)).getFirst();
    }

    @Test
    public void testDivideWithUniqueFiles() throws IOException {
        Set<File> files = new HashSet<>();
        files.add(image1);
        files.add(image2);
        files.add(image3);

        CRC32Grouper crc32Grouper = new CRC32Grouper();

        Set<Set<File>> groupedFiles = crc32Grouper.divide(files);

        assertEquals(0, groupedFiles.size(), "Should return 0 separate groups for 3 different files");
    }

    @Test
    public void testDivideWithDuplicateFiles() throws IOException {
        Set<File> files = new HashSet<>();
        files.add(image1);
        files.add(image2);
        files.add(image2Copy);

        CRC32Grouper crc32Grouper = new CRC32Grouper();

        Set<Set<File>> groupedFiles = crc32Grouper.divide(files);

        assertEquals(1, groupedFiles.size(), "Should return 1 group: one for image2");

        Set<File> groupForImage2 = groupedFiles.stream()
                .filter(group -> group.contains(image2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Group for image2 not found"));
        assertEquals(2, groupForImage2.size(), "Group for image2 should contain 2 files");
    }

    @Test
    public void testDivideWithIdenticalFiles() throws IOException {
        List<File> files = images;

        CRC32Grouper crc32Grouper = new CRC32Grouper();

        Set<Set<File>> groupedFiles = crc32Grouper.divide(files);

        assertEquals(3, groupedFiles.size(), "Should return 3 groups for identical files");

        for (Set<File> sf : groupedFiles) {
            assertEquals(2, sf.size(), "Group should contain 2 identical files");
        }

        Set<File> groupForImage2 = groupedFiles.stream()
                .filter(group -> group.contains(image2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Group for image2 not found"));
        assertTrue(groupForImage2.contains(image2Copy), "Group for image2 should also contain image2Copy");
    }
}
