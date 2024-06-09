package pl.magzik.Comparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class ImageFilePredicate implements FilePredicate {

    private final static byte[][] formatNumbers = {
        {(byte)0xff, (byte)0xd8, (byte)0xff,},
        {(byte)0x89, (byte)0x50, (byte)0x4e, (byte)0x47, (byte)0x0d, (byte)0x0a, (byte)0x1a, (byte)0x0a,},
        {(byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x37, (byte)0x61,},
        {(byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x39, (byte)0x61,},
        {(byte)0x42, (byte)0x4d,},
        {(byte)0x49, (byte)0x49, (byte)0x2a, (byte)0x00,},
        {(byte)0x4d, (byte)0x4d, (byte)0x00, (byte)0x2a,},
        {(byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00,},
        {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0c, (byte)0x6a, (byte)0x50, (byte)0x20, (byte)0x20, (byte)0x0d, (byte)0x0a, (byte)0x87, (byte)0x0a},
        {(byte)0xff, (byte)0x4f, (byte)0xff, (byte)0x51,},
    };

    private final static int longestHeader = Arrays.stream(formatNumbers)
            .map(b -> b.length)
            .max(Integer::compareTo).orElse(0);

    @Override
    public boolean test(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[longestHeader];
            if (fis.read(header) == -1)
                return false;

            for (byte[] formatBytes : formatNumbers) {
                int i = 0;
                boolean valid = true;

                for (byte b : formatBytes) {
                    if (b != header[i++]) {
                        valid = false;
                        break;
                    }
                }
                if (valid) return true;
            }
        }

        return false;
    }
}
