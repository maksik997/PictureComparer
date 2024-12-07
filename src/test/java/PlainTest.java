import pl.magzik.Processor;
import pl.magzik.algorithms.Algorithm;
import pl.magzik.algorithms.PerceptualHash;
import pl.magzik.algorithms.PixelByPixel;
import pl.magzik.cache.AdaptiveCache;
import pl.magzik.grouping.CRC32Grouper;
import pl.magzik.io.FileOperator;
import pl.magzik.predicates.ImageFilePredicate;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Test to check some test set of images.
 * As long as the exit code is 0 you can consider it as passed.
 * Can be used to check whole process on real data. But without assertions.
 * */
public class PlainTest {

    public static void main(String[] args) throws IOException {
        FileOperator fo = new FileOperator(new ImageFilePredicate(), Integer.MAX_VALUE);
        Set<Algorithm<?>> algs = new LinkedHashSet<>(List.of(new PerceptualHash(), new PixelByPixel()));
        Processor pr = new Processor(new CRC32Grouper(), algs);
        AdaptiveCache.getInstance().monitor(1);

        File[] files = {
            /* FILES/DIRECTORIES TO CHECK */
        };

        long time = System.currentTimeMillis();
        System.out.println("=========== LOADING ===========");
        List<File> f = fo.load(files);
        System.out.println("Found " + f.size() + " files.");
        System.out.println("Found in: " + (System.currentTimeMillis() - time) + " milliseconds");
        time = System.currentTimeMillis();

        System.out.println("=========== PROCESSING ===========");
        var map = pr.process(f);

        System.out.println("Processed " + map.size() + " image groups.");
        System.out.println("Processing completed in: " + (System.currentTimeMillis() - time) + " milliseconds");

        System.out.println("=========== RESULTS ===========");
        System.out.println(map);

        System.exit(0);
    }
}
