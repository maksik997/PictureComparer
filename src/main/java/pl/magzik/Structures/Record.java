package pl.magzik.Structures;

import java.io.File;
import java.util.Objects;
import java.util.zip.CRC32;

public abstract class Record<T>{

    // Algorithm to calculate checksum
    protected final static CRC32 algorithm = new CRC32();

    // File Reference
    private final File file;

    // Checksum of the content
    private long checksum;


    public Record(File file) {
        this.file = file;
        this.checksum = this.hashCode();
    }
    public Record(Record<T> r){
        this(r.file);
    }

    public File getFile() {
        return file;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    protected abstract void calculateAndSetChecksum(T e);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Record<?> record)) return false;
        return checksum == record.checksum && Objects.equals(file, record.file);
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
