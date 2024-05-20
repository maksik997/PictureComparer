import pl.magzik.Comparer;
import pl.magzik.PictureComparer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataSourceTest {

    // This should be a path to directory with some images.
    // More = better
    public static String sourcePath = "{source}";

    // This should be an array of images. The more, the better
    // Also should be only one duplicate
    public static String[] sources = {
        "{sourceFile}",
        "{sourceFile}"
    };

    // This should be a path, to not existent file. Check if exception is thrown correctly.
    public static String notExistentSources = "{sourceFile}";

    public static void main(String[] args) {
        int sum = 0;

        System.out.println("Case 1. Directory based");
        // Should leave 0 duplicates
        PictureComparer pc = null;
        try {
            pc = new PictureComparer(
                new File(System.getProperty("user.dir")),
                new File(sourcePath)
            );
        } catch (FileNotFoundException e) {
            System.out.println("Task failed." + e);
            System.exit(1);
        }

        pc.map();
        pc.compare();

        int duplicatesFound = pc.getDuplicatesObjectCount();
        sum += pc.getTotalObjectCount();

        System.out.printf(
                "Statistics:%nObjects found: %d,%nDuplicates found: %d,%nList: %s%n",
                pc.getTotalObjectCount(), pc.getDuplicatesObjectCount(),
                pc.getDuplicates()
        );

        System.out.printf("Test 1: %s%n", duplicatesFound == 0 ? "done" : "failed");

        System.out.println("Case 2. Source based");
        // Should leave 1
        List<File> sourcesL = new ArrayList<>(Arrays.stream(sources).map(File::new).toList());
        try {
            pc._setUp(
                new File(System.getProperty("user.dir")),
                sourcesL
            );
        } catch (FileNotFoundException e) {
            System.out.println("Task failed." + e);
            System.exit(2);
        }

        pc.map();
        pc.compare();

        duplicatesFound = pc.getDuplicatesObjectCount();
        sum += pc.getTotalObjectCount();

        System.out.printf(
                "Statistics:%nObjects found: %d,%nDuplicates found: %d,%nList: %s%n",
                pc.getTotalObjectCount(), pc.getDuplicatesObjectCount(),
                pc.getDuplicates()
        );

        System.out.printf("Test 2: %s%n", duplicatesFound == 1 ? "done" : "failed");

        System.out.println("Case 3. Mixed");
        pc._reset();
        pc.setMode(Comparer.Modes.RECURSIVE);

        sourcesL.add(new File(sourcePath));

        try {
            pc._setUp(
                new File(System.getProperty("user.dir")),
                sourcesL
            );
        } catch (FileNotFoundException e) {
            System.out.println("Task failed." + e);
            System.exit(3);
        }

        pc.map();
        pc.compare();

        System.out.printf(
                "Statistics:%nObjects found: %d,%nDuplicates found: %d,%nList: %s%n",
                pc.getTotalObjectCount(), pc.getDuplicatesObjectCount(),
                pc.getDuplicates()
        );

        System.out.printf("Test 3: %s%n", sum == pc.getTotalObjectCount() + sourcesL.size() ? "done" : "failed");

        System.out.println("Case 4. Not-existent sources");
        pc._reset();
        pc.setMode(Comparer.Modes.NOT_RECURSIVE);

        sourcesL.add(new File(notExistentSources));

        try {
            pc._setUp(
                    new File(System.getProperty("user.dir")),
                    sourcesL
            );
        } catch (FileNotFoundException e) {
            System.out.println("Test 4: done");
        }

    }
}
