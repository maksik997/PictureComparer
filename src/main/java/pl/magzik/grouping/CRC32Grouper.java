package pl.magzik.grouping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

public class CRC32Grouper implements Grouper {

    private static final Logger logger = LoggerFactory.getLogger(CRC32Grouper.class);

    @Override
    public Set<Set<File>> divide(Collection<File> col) {
        Set<Set<File>> groupedFiles = new HashSet<>();
        Map<Long, Set<File>> checksumMap = col.parallelStream()
            .map(f -> {
                try {
                    long checksum = calculateChecksum(f);
                    if (checksum != 0L)
                        return new AbstractMap.SimpleEntry<>(checksum, f);
                } catch (IOException e) {
                    logger.error("Error while processing a file: {}", f.getName(), e);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toSet())
            ));

        checksumMap.values().stream()
                .filter(g -> g.size() > 1)
                .forEach(groupedFiles::add);

        return groupedFiles;
    }

    /**
     * Creates a checksum for a given file using the CRC32 algorithm.
     * This method reads the file in chunks and updates the CRC32 checksum as it processes the file's content.
     * The checksum is calculated by reading the file's bytes and applying the CRC32 hashing algorithm.
     *
     * @param f The file for which the checksum is to be generated
     * @return The checksum value of the file as a long
     * @throws IOException If an I/O error occurs while reading the file
     */
    private long calculateChecksum(File f) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(f))) {
            CRC32 crc32 = new CRC32();
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                crc32.update(buffer, 0, bytesRead);
            }
            return crc32.getValue();
        }
    }
}
