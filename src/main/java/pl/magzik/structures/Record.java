package pl.magzik.structures;

import pl.magzik.utils.LoggingInterface;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Abstract class representing a record of a file with a checksum.
 *
 * @param <T> the type of the record
 */
@Deprecated
public abstract class Record<T> implements LoggingInterface {

    protected final File file;
    private final long checksum;
    private final String extension;

    /**
     * Constructs a Record with the given file.
     *
     * @param file the file associated with the record
     * @throws IOException if an I/O error occurs while creating the checksum
     */
    public Record(File file) throws IOException {
        Objects.requireNonNull(file, "File cannot be null");
        this.file = file;
        this.extension = getExtension(file);
        this.checksum = createChecksum(file);
    }

    /**
     * Copy constructor for creating a new record based on an existing one.
     *
     * @param r the record to copy
     * @throws IOException if an I/O error occurs while creating the checksum
     */
    public Record(Record<T> r) throws IOException {
        this(r.file);
    }

    /**
     * Returns the file associated with this record.
     *
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the checksum of this record.
     *
     * @return the checksum
     */
    public long getChecksum() {
        return checksum;
    }

    /**
     * Creates a checksum for the given file.
     *
     * @param e the file to create a checksum for
     * @return the checksum of the file
     * @throws IOException if an I/O error occurs
     */
    protected abstract long createChecksum(File e) throws IOException;

    /**
     * Retrieves the file extension based on the file's name.
     *
     * @param f the file from which to retrieve the extension
     * @return the file extension (without the dot) or an empty string if the extension is not found
     */
    protected String getExtension(File f) {
        int idx = f.getName().lastIndexOf('.');
        return idx == -1 ? "" : f.getName().substring(idx + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Record<?> record)) return false;
        return checksum == record.checksum && Objects.equals(extension, record.extension) && Objects.equals(file, record.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, checksum);
    }

    @Override
    public String toString() {
        return "Record{" +
                "file=" + file +
                ", checksum=" + checksum +
                '}';
    }
}
