import pl.magzik.comparator.ImageFilePredicate;
import pl.magzik.io.FileOperator;
import pl.magzik.structures.ImageRecord;
import pl.magzik.utils.LoggingInterface;


import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class FTest {
    public static void main(String[] args) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        FileOperator fo = new FileOperator(new ImageFilePredicate(), Integer.MAX_VALUE);

        File[] files = {
            new File("D:\\Data")
        };

        // Simple function that creates ImageRecords (groupByFunction)
        Function<File, ImageRecord> createImageRecord = file -> {
            try {
                return new ImageRecord(file);
            } catch (IOException ex) {
                LoggingInterface.staticLog(String.format("Skipping file: %s", file.getName()));
                LoggingInterface.staticLog(ex, String.format("Skipping file: %s", file.getName()));
            }
            return null;
        };

        List<File> f = fo.load(files);

        long time = System.currentTimeMillis();
        System.out.println(f);
        System.out.println("Found: "+ f.size());
        System.out.println("Found in: " + (System.currentTimeMillis() - time) + " milliseconds");
    }
}
