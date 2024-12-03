package pl.magzik;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.magzik.algorithms.Algorithm;
import pl.magzik.grouping.Grouper;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The {@code Processor} class is responsible for identifying and grouping duplicate files
 * based on a multi-step processing workflow.
 * <p>
 * The processing workflow consists of three main steps:
 * <ol>
 *     <li><b>Initial Division:</b> Files are divided into subsets using a {@link Grouper}, based on a distinction predicate
 *     (e.g., CRC32 checksum). Each subset contains files that are similar according to the chosen predicate.</li>
 *     <li><b>Algorithm Application:</b> A set of {@link Algorithm} implementations is applied to further refine the grouping.
 *     The algorithms identify additional shared characteristics (e.g., perceptual hashes) and consolidate groups accordingly.</li>
 *     <li><b>Original File Identification:</b> The first file in each final group is designated as the "original," and the groups
 *     are reorganized into a map where each original file is the key, and the corresponding similar files are the values.</li>
 * </ol>
 * </p>
 *
 * <p>
 * This class is designed to process collections of files efficiently, leveraging parallel streams for processing subsets
 * and applying algorithms concurrently. It also provides robust error handling for I/O issues encountered during processing.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * Grouper grouper = new Crc32Grouper();
 * Set<Algorithm<?>> algorithms = Set.of(new PerceptualHashAlgorithm(), new SizeAlgorithm());
 * Processor processor = new Processor(grouper, algorithms);
 *
 * Collection<File> files = List.of(new File("image1.jpg"), new File("image2.jpg"));
 * Map<File, Set<File>> result = processor.process(files);
 * result.forEach((original, duplicates) -> {
 *     System.out.println("Original: " + original);
 *     duplicates.forEach(duplicate -> System.out.println("  Duplicate: " + duplicate));
 * });
 * }</pre>
 *
 * @see Grouper
 * @see Algorithm
 */
public class Processor {

    private static final Logger logger = LoggerFactory.getLogger(Processor.class);

    private final Grouper grouper;

    private final Set<Algorithm<?>> algorithms;

    public Processor(Grouper grouper, Set<Algorithm<?>> algorithms) {
        Objects.requireNonNull(grouper, "Grouper is null");
        Objects.requireNonNull(algorithms, "Algorithm set is null");
        if (algorithms.isEmpty() || algorithms.contains(null)) throw new NullPointerException("Algorithm set is empty or contains null.");

        this.grouper = grouper;
        this.algorithms = new LinkedHashSet<>(algorithms);
    }

    /**
     * Processes a collection of files to identify and group duplicated files based on a multi-step workflow.
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li><b>Initial Division:</b> Divides the input collection of files into subsets based on a distinction predicate
     *     (e.g., CRC32 checksum). Each subset contains files that are similar according to the chosen predicate.</li>
     *     <li><b>Algorithm Application:</b> For each subset, a series of algorithms is applied:
     *         <ul>
     *             <li><b>Processing:</b> Each algorithm processes the subset of files and generates a map where the key represents
     *             a shared characteristic (e.g., perceptual hash) and the value is the corresponding subset of files.</li>
     *             <li><b>Consolidation:</b> After each algorithm, the resulting maps are consolidated to merge groups with the same
     *             key, after which keys are lost and remove one-element groups (unique files).</li>
     *         </ul>
     *     </li>
     *     <li><b>Original File Identification:</b> Identifies the "original" file in each final group. The first file in each group
     *     is treated as the original, and the method reorganizes the groups into a map where each key is an original file, and the
     *     value is a set of similar files.</li>
     * </ol>
     * </p>
     *
     * @param files A collection of files to process. These files are typically of the same type (e.g., images).
     * @return A map where:
     * <ul>
     *     <li>The key is a file considered the "original" in a group of similar files.</li>
     *     <li>The value is a set of files similar to the "original," including the original file itself.</li>
     * </ul>
     * The map guarantees that each file from the input collection belongs to exactly one group.
     *
     * @throws NullPointerException If the input collection or any file in it is null.
     * @throws IOException If an I/O error occur.
     */
    public Map<File, Set<File>> process(Collection<File> files) throws IOException {
        if (files == null || files.contains(null)) {
            throw new NullPointerException("Input collection or its elements must not be null.");
        }

        logger.info("Processing started...");

        logger.info("Dividing input collection.");
        Set<Set<File>> groupedFiles = grouper.divide(files); // This variable must contain only subsets with more than 1 element.
        logger.info("Input collection division completed.");

        logger.info("Proceeding to algorithm application.");
        Map<?, Set<File>> algorithmOutput;
        for (Algorithm<?> algorithm : algorithms) {
            try {
                logger.info("Applying algorithm: {}", algorithm.getClass().getSimpleName());

                algorithmOutput = groupedFiles.parallelStream()
                        .map(algorithm::apply)
                        .flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                this::consolidate
                        ));
                logger.info("Algorithm applied. Eliminating unique files...");

                groupedFiles = algorithmOutput.values()
                        .stream().filter(s -> s.size() > 1)
                        .collect(Collectors.toSet());

                logger.info("Step finished.");
            } catch (UncheckedIOException e) {
                throw new IOException("Couldn't use algorithm: " + algorithm.getClass().getSimpleName() + "\nBecause: " + e.getMessage(), e.getCause());
            }
        }

        return groupedFiles.stream()
            .map(s -> s.stream().toList())
            .collect(Collectors.toMap(
                List::getFirst,
                s -> new HashSet<>(s.subList(1, s.size()))
            ));
    }

    /**
     * Merges two sets into a new set containing all elements from both input sets.
     *
     * @param s1 the first set to merge
     * @param s2 the second set to merge
     * @return a new set containing all elements from {@code s1} and {@code s2}
     */
    private Set<File> consolidate(Set<File> s1, Set<File> s2) {
        Set<File> out = new HashSet<>(s1);
        out.addAll(s2);
        return out;
    }
}
