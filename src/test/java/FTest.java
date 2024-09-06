import pl.magzik.algorithms.PerceptualHash;
import pl.magzik.algorithms.PixelByPixel;
import pl.magzik.predicates.ImageFilePredicate;
import pl.magzik.io.FileOperator;
import pl.magzik.structures.ImageRecord;
import pl.magzik.structures.Record;
import pl.magzik.utils.LoggingInterface;


import java.awt.image.BufferedImage;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class FTest {
    public static void main(String[] args) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        FileOperator fo = new FileOperator(new ImageFilePredicate(), Integer.MAX_VALUE);

        File[] files = {
            new File("D:\\Data")
        };

        List<File> f = fo.load(files);

        long time = System.currentTimeMillis();
        System.out.println(f);
        System.out.println("Found: "+ f.size());
        System.out.println("Found in: " + (System.currentTimeMillis() - time) + " milliseconds");


        time = System.currentTimeMillis();

        var map = Record.process(f, ImageRecord::create, new PerceptualHash(), new PixelByPixel());
        System.out.println("===========");
        for (List<?> list : map.values()) {
            System.out.println(list);
        }
        System.out.println("Found: "+ map.size());
        System.out.println("Found in: " + (System.currentTimeMillis() - time) + " milliseconds");
    }
}
