package pl.magzik.algorithms;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Functional interface representing algorithm.
 * Applies some operation set to files, resulting in divided input set of files mapped by some key K.
 * */
@FunctionalInterface
public interface Algorithm <K> {

    /**
     * Applies the algorithm to the given set of files.
     * The algorithm should divide the given set into smaller subsets, where each subset contains files
     * that share a specific characteristic (e.g., checksum, hash, metadata, etc.).
     * <p>
     * The resulting key (K) should be deterministic, meaning it should consistently map the same set of
     * files to the same key based on the characteristic used for grouping.
     *
     * @param group A set of files to be processed by the algorithm. These files are of a specific type
     *             (e.g., images), and the goal is to group them based on a shared characteristic.
     * @return A map where each key corresponds to a subset of files (Set<File>) that share the same characteristic.
     *        The key should be calculated using data associated with each file, such as file metadata or content.
     *        The key-value mapping should be deterministic (i.e., the same input group will always produce the same output map).
     * */
    Map<K, Set<File>> apply(Set<File> group);
}
