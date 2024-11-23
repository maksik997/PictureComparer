package pl.magzik.grouping;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public interface Grouper {

    /**
     * Divides a collection of files into subsets based on a distinction predicate (e.g., checksum).
     * Each subset will contain files that share the same characteristic (e.g., identical checksum).
     *
     * @param col <i>A collection of files of one type (e.g., images) to be divided into groups.</i>
     * @return <i>A set of subsets of the input collection, where each subset contains files that are similar according to the chosen distinction predicate (e.g., checksum).</i>
     * @throws IOException If an I/O error occurs while reading or processing the files.
     * */
    Set<Set<File>> divide(Collection<File> col) throws IOException;

}
