import pl.magzik.PictureComparer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) throws IOException {
        System.out.println("Test 1");
        System.out.println("\tTake 5 files and find duplicates\n\tExpected output: 3 files should be removed.");

        File test1Dest = new File("src/main/resources/images/test_1");

        PictureComparer test1 = new PictureComparer(
            Arrays.asList(
                new File("src/main/resources/duplicates/test_1/_moved_3225.JPG"),
                new File("src/main/resources/duplicates/test_1/data_convertToPNG1.png"),
                new File("src/main/resources/duplicates/test_1/data_convertToPNG1 — kopia.png"),
                new File("src/main/resources/duplicates/test_1/data_convertToPNG1 — kopia (2).png"),
                new File("src/main/resources/duplicates/test_1/data_convertToPNG1 — kopia (3).png")
            ),
            test1Dest
        );
        test1._init();
        test1.map();
        test1.extractDuplicates();
        test1.moveDuplicates();

        System.out.println("\nTest 2");
        System.out.println("\tTake catalog and find duplicates\n\tExpected output: some files should be redundant");

        File test2Source = new File("src/main/resources/duplicates/test_2");
        File test2Dest = new File("src/main/resources/images/test_2");

        PictureComparer test2 = new PictureComparer(
            test2Source, test2Dest
        );
        test2._init();
        test2.map();
        test2.extractDuplicates();
        test2.moveDuplicates();

    }
}
