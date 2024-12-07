package pl.magzik;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
 * based on a multistep processing workflow.
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

    /**
     * Creates new Processor instance.
     * <p>
     *     <strong>Important!</strong><br />
     *     When passing algorithms you should use a collection that defines some kind of ordering.
     *     Otherwise, results may be unpredictable.
     * </p>
     * @param grouper A {@link Grouper} to be used in "Initial division" step.
     * @param algorithms A {@link Collection} of {@link Algorithm} objects to be used in "Algorithms application" step.
     * */
    public Processor(Grouper grouper, Collection<Algorithm<?>> algorithms) {
        Objects.requireNonNull(grouper, "Grouper is null");
        Objects.requireNonNull(algorithms, "Algorithm set is null");
        if (algorithms.isEmpty() || algorithms.contains(null)) throw new NullPointerException("Algorithm set is empty or contains null.");

        this.grouper = grouper;
        this.algorithms = new LinkedHashSet<>(algorithms);
    }

    /**
     * Processes a collection of files to identify and group duplicated files based on a multistep workflow.
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
    @NotNull
    @Contract("_ -> new")
    public Map<File, Set<File>> process(@NotNull Collection<File> files) throws IOException {
        Objects.requireNonNull(files, "Input collection must not be null");
        if (files.contains(null)) {
            throw new NullPointerException("Input collection must not contain null.");
        }
        logger.info("Processing started...");

        logger.info("Dividing input collection.");

        Set<Set<File>> groupedFiles = grouper.divide(files);
                        // This variable must contain only subsets with more than 1 element.

        logger.info("Input collection division completed.");
        groupedFiles = algorithmsApplication(groupedFiles);
        return originalDistinction(groupedFiles);
    }

    /**
     * Applies a series of algorithms to group files and consolidates the results by merging groups with the same key.
     * <p>This method processes the grouped files using each algorithm in the {@code algorithms} collection. After each algorithm is applied,
     * it consolidates the resulting maps by merging groups that have the same key and removing groups that only contain a single file.</p>
     *
     * @param groupedFiles a {@link Set} of {@link Set} of {@link File} instances representing the grouped files to which the algorithms should be applied.
     *                     Each group represents a set of similar files.
     * @return a {@link Set} of {@link Set} of {@link File} instances after applying the algorithms and consolidating the results.
     * @throws IOException if an error occurs during algorithm application or file processing.
     */
    @NotNull
    @Contract("_ -> new")
    private Set<Set<File>> algorithmsApplication(@NotNull Set<Set<File>> groupedFiles) throws IOException {
        logger.info("Proceeding to algorithm application.");
        Map<?, Set<File>> algorithmOutput;
        for (Algorithm<?> algorithm : algorithms) {
            try {
                algorithmOutput = applyAlgorithm(algorithm, groupedFiles);
                groupedFiles = postAlgorithmConsolidation(algorithmOutput);
                logger.info("Step finished.");
            } catch (UncheckedIOException e) {
                throw new IOException("Couldn't use algorithm: " + algorithm.getClass().getSimpleName() + "\nBecause: " + e.getMessage(), e.getCause());
            }
        }
        return groupedFiles;
    }

    /**
     * Applies a single algorithm to the grouped files and returns a map of results.
     * <p>This method processes the input groups using the specified algorithm and generates a map, where the key represents
     * a shared characteristic, and the value is a set of files that share that characteristic.</p>
     *
     * @param algorithm the {@link Algorithm} to apply to the grouped files.
     * @param groupedFiles a {@link Set} of {@link Set} of {@link File} instances representing the grouped files to be processed.
     * @param <T> the type of the key in the generated map (e.g., perceptual hash, CRC32 checksum).
     * @return a {@link Map} where the key is the characteristic (e.g., hash value), and the value is a set of files sharing that characteristic.
     */
    @NotNull
    @Contract("_,_ -> new")
    private <T> Map<T, Set<File>> applyAlgorithm(@NotNull Algorithm<T> algorithm, @NotNull Set<Set<File>> groupedFiles) {
        logger.info("Applying algorithm: {}", algorithm.getClass().getSimpleName());
        return groupedFiles.parallelStream()
            .map(algorithm::apply)
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                this::consolidate
            ));
    }

    /**
     * Consolidates the results of the applied algorithm by removing groups with only one file and merging groups with identical keys.
     * <p>This method processes the output of an algorithm, eliminating groups that contain only one file and merging groups with the same key.</p>
     *
     * @param algorithmOutput a {@link Map} containing the results of the algorithm, where the key is a shared characteristic (e.g., hash) and the value is a set of files.
     * @return a {@link Set} of {@link Set} of {@link File} instances after consolidation, where only groups with more than one file are kept.
     */
    @NotNull
    @Contract("_ -> new")
    private Set<Set<File>> postAlgorithmConsolidation(@NotNull Map<?, Set<File>> algorithmOutput) {
        logger.info("Algorithm applied. Eliminating unique files...");
        return algorithmOutput.values()
            .stream().filter(s -> s.size() > 1)
            .collect(Collectors.toSet());
    }

    /**
     * Identifies the "original" file in each group and reorganizes the groups into a map.
     * Each group is represented as a set of files, where the first file in the group is
     * treated as the "original". This method creates a map where:
     * <ul>
     *     <li>The key is the original file (the first file in each group).</li>
     *     <li>The value is a set of files representing duplicates or similar files for that original.</li>
     * </ul>
     * Note that each subset of passed {@link Set} must contain more than one element.
     *
     * @param groupedFiles a {@link Set} of {@link Set} of {@link File} instances,
     *                     where each inner set represents a group of files.
     *                     The input must not be null, and each group should contain at least two files.
     * @return a new {@link Map} where:
     *         - The key is a {@link File} representing the original file in one group.
     *         - The value is a {@link Set} of {@link File} instances representing duplicates or similar files.
     *         The returned map is guaranteed not to contain null keys or values.
     * @throws NullPointerException if {@code groupedFiles} is null, or when it contains null.
     */
    @NotNull
    @Contract("_ -> new")
    private Map<File, Set<File>> originalDistinction(@NotNull Set<Set<File>> groupedFiles) {
        Objects.requireNonNull(groupedFiles, "groupedFiles is null.");
        if (groupedFiles.contains(null)) throw new NullPointerException("groupedFiles contains null.");

        logger.info("Identifying originals...");
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
    @NotNull
    @Contract("_,_ -> new")
    private Set<File> consolidate(@NotNull Set<File> s1, @NotNull Set<File> s2) {
        Set<File> out = new HashSet<>(s1);
        out.addAll(s2);
        return out;
    }
}
