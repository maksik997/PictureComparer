import pl.magzik.PictureComparer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) throws IOException {
        // Usage instruction:
        // Firstly, we need to create PictureComparer object,
        // next we prepare our files/directories, we use _setUp method with valid arguments,
        // next we use combo of a map, compare and move methods (in that order),
        // map method will map whole input, such as files/directories,
        // compare method will point which of these are duplicates,
        // and lastly, move method will move all duplicates to specified directory,
        // in case we want to use Comparer again we should invoke _reset method.

        PictureComparer pc = new PictureComparer();

        File test1Dest = new File("{destDir}");
        List<File> files = new ArrayList<>();
        files.add(new File("{file1}"));
        files.add(new File("{file2}"));
        files.add(new File("{file3}"));
        files.add(new File("{file4}"));
        files.add(new File("{file5}"));

        pc._setUp(files, test1Dest);
        pc.map();
        pc.compare();
        pc.move();

        pc._reset();

        File test2Source = new File("{sourceDir}");
        File test2Dest = new File("{destDir}");

        pc._setUp(test2Source, test2Dest);
        pc.map();
        pc.compare();
        pc.move();

        pc._reset();
    }
}
