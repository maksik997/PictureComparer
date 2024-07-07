import pl.magzik.Comparator.ImageFilePredicate;
import pl.magzik.IO.FileOperator;
import pl.magzik.Structures.ImageRecord;
import pl.magzik.Structures.Record;
import pl.magzik.Utils.LoggingInterface;


import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class FTest {
    public static void main(String[] args) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        long s = System.currentTimeMillis(), m = 0;
        FileOperator fo = new FileOperator();

        File[] files = {
            new File("D:\\Data\\_Photos"),
            //new File("C:\\Users\\maksy"),
            //new File("./src/test/java/")
        };

        Function<File, ImageRecord> createImageRecord = file -> {
            try {
                return new ImageRecord(file);
            } catch (IOException ex) {
                LoggingInterface.staticLog(String.format("Skipping file: %s", file.getName()));
                LoggingInterface.staticLog(ex, String.format("Skipping file: %s", file.getName()));
            }
            return null;
        };

        List<File> f = fo.loadFiles(Integer.MAX_VALUE, new ImageFilePredicate(), files);

        System.out.println(f);
        System.out.println("Found: "+ f.size());
        System.out.println((System.currentTimeMillis() - s) + " ms time.");
        m = System.currentTimeMillis();

        System.out.println("Record.process output: ");
        System.out.println(Record.process(f, createImageRecord, ImageRecord.pHashFunction, ImageRecord.pixelByPixelFunction));

        System.out.println((System.currentTimeMillis() - m) + " ms time.");

    }
}
