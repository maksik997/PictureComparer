import pl.magzik.RecordProcessor;
import pl.magzik.algorithms.PerceptualHash;
import pl.magzik.algorithms.PixelByPixel;
import pl.magzik.io.FileOperator;
import pl.magzik.predicates.ImageFilePredicate;
import pl.magzik.structures.ImageRecord;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FTest {
    public static void main(String[] args) throws IOException {
        FileOperator fo = new FileOperator(new ImageFilePredicate(), Integer.MAX_VALUE);

        File[] files = {
            /*
            * Files to be loaded.
            * */
        };

        long time = System.currentTimeMillis();

        System.out.println("=========== LOADING ===========");
        List<File> f = fo.load(files);

        System.out.println(f);
        System.out.println("Found: "+ f.size());
        System.out.println("Found in: " + (System.currentTimeMillis() - time) + " milliseconds");

        time = System.currentTimeMillis();

        RecordProcessor rp = new RecordProcessor();
        var map = rp.process(f, ImageRecord::create, new PerceptualHash(), new PixelByPixel());

        System.out.println("=========== PROCESSING ===========");
        for (List<?> list : map.values()) {
            System.out.println(list);
        }
        System.out.println("Found: "+ map.size());
        System.out.println("Found in: " + (System.currentTimeMillis() - time) + " milliseconds");
    }
}
