package pl.magzik.Structures;

import pl.magzik.Algorithms.DCT;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.List;
import java.util.stream.Collectors;

public class ImageRecord extends Record<BufferedImage> {

    public final static Function<List<? extends Record<BufferedImage>>, Map<? ,List<? extends Record<BufferedImage>>>> pHashFunction = list -> {
        //boolean[] ifTake = new boolean[list.size()];
        String[] hashes = new String[list.size()];
        int p = 0;
        int w = 64, h = 64;

        for (Record<? extends BufferedImage> imageRecord : list) {
            // Resizing
            BufferedImage orgImage;
            try {
                orgImage = ImageIO.read(imageRecord.getFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (orgImage == null) throw new RuntimeException("Image not found");
            BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(orgImage, 0, 0, w, h, null);
            g.dispose();

            // Fast DCT Lee
            double[][] samples = new double[w][h];
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    samples[x][y] = resizedImage.getRaster().getSample(x, y, 0);
                }
            }

            double[][] quantDct = DCT.quantization(DCT.transform(samples));

            double avg = Arrays.stream(quantDct).mapToDouble(a -> Arrays.stream(a).sum()).sum() / (quantDct.length + quantDct[0].length);


            /*// DCT
            double[][] vals = new double[w][h];
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    vals[x][y] = resizedImage.getRaster().getSample(x, y, 0);
                }
            }

            double[][] dct = new double[w][h];
            for (int u = 0; u < w; u++) {
                for (int v = 0; v < h; v++) {
                    double sum = 0;
                    for (int x = 0; x < w; x++) {
                        for (int y = 0; y < h; y++) {
                            sum += vals[x][y] *
                                    Math.cos((2 * x + 1) * u * Math.PI /(2.0 * w)) *
                                    Math.cos((2 * y + 1) * v * Math.PI /(2.0 * h));
                        }
                    }
                    double cu = (u == 0) ? 1 / Math.sqrt(2) : 1;
                    double cv = (v == 0) ? 1 / Math.sqrt(2) : 1;
                    dct[u][v] = cu * cv * sum;
                }
            }

            int sw = 8, sh = 8;

            // Convert DCT
            double[][] dctLowFreq = new double[sw][sh];
            for (int x = 0; x < sw; x++) {
                for (int y = 0; y < sh; y++) {
                    dctLowFreq[x][y] = dct[x+1][y+1];
                }
            }

            double total = Arrays.stream(dctLowFreq)
                    .mapToDouble(arr -> Arrays.stream(arr).sum())
                    .sum();
            double avg = total / (sw*sh);*/

            // Hash creation
            StringBuilder hash = new StringBuilder();
            for (double[] arr : quantDct) {
                for (double vals : arr) {
                    if (vals > avg) hash.append("1");
                    else hash.append("0");
                }
            }
            hashes[p++] = hash.toString();

            /*if (Arrays.stream(hashes).noneMatch(Objects::isNull)) {
                if (p == 0) ifTake[p] = true;
                else {
                    boolean failed = false;
                    for (int i = 0; i < p; i++) {
                        String hash2 = hashes[i];
                        int distance = 0;
                        for (int j = 0; j < hash2.length(); j++) {
                            if (hash2.charAt(j) != hash.charAt(j)) distance++;
                        }
                        if (distance > 15) {
                            ifTake[p] = false;
                            failed = true;
                            break;
                        }
                    }
                    if (!failed) ifTake[p] = true;
                }
                p++;
            }*/

            /*if (Arrays.stream(hashes).noneMatch(Objects::isNull)) {
                System.out.println(Objects.equals(hashes[0], hashes[1]));

                AtomicInteger i1 = new AtomicInteger(0);
                list.stream()
                        .collect(Collectors.groupingBy(l -> i1.getAndIncrement()))
                        .values()
                        .stream().filter(l -> l.size() > 1)
                        .forEach(System.out::println);

                if (p == 0) ifTake[p] = true;
                else {
                    boolean failed = false;
                    for (int i = 0; i < p; i++) {
                        String hash2 = hashes[i];
                        int distance = 0;
                        for (int j = 0; j < hash2.length(); j++) {
                            if (hash2.charAt(j) != hash.charAt(j)) distance++;
                        }
                        if (distance > 15) {
                            ifTake[p] = false;
                            failed = true;
                            break;
                        }
                    }
                    if (!failed) ifTake[p] = true;
                }
                p++;
            }*/
        }

        if (Arrays.stream(hashes).anyMatch(Objects::isNull)) {
//            System.out.println(list);
//            System.out.println(Arrays.toString(hashes));
            throw new NullPointerException("Some hashes are null.");
        }


        //System.out.println(Objects.equals(hashes[0], hashes[1]));

        // todo fix this sh*t
        AtomicInteger i1 = new AtomicInteger(0),
                      i2 = new AtomicInteger(0);
        return list.stream()
                .collect(Collectors.groupingBy(l -> hashes[i1.getAndIncrement()]))
                .values()
                .stream().filter(l -> l.size() > 1)
                .map(l -> l.subList(1, l.size()))
                .collect(Collectors.toMap(l -> hashes[i2.getAndIncrement()], l -> l));



        // We take only duplicates, rest is good, I consider it todo for now
//        AtomicInteger itr = new AtomicInteger(0);
//        return list.stream().filter(e -> !ifTake[itr.getAndIncrement()]).toList();
    };

    public ImageRecord(File file) throws IOException {
        super(file);
//        createChecksum(ImageIO.read(file));
    }

    public ImageRecord(ImageRecord r) throws IOException {
        this(r.getFile());
    }

    @Override
    public String toString() {
        return "ImageRecord{" +
                super.toString() +
                "}";
    }

    @Override
    protected long createChecksum(File f) throws IOException {
        BufferedImage img = ImageIO.read(f);

        int idx = f.getName().lastIndexOf('.');
        String extension = f.getName().substring(idx+1);

//        String[] fileNameArr = super.getFile().getName().split("\\.");
//        String extension = fileNameArr[fileNameArr.length-1];

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            ImageIO.write(img, extension, byteStream);
            Record.algorithm.update(byteStream.toByteArray());
        } catch (IOException ex) {
            // skip the file and set checksum = 0
            log(ex, "Couldn't create checksum.");
//            System.out.println(ex.getMessage());
//            setChecksum(0L);
            return 0L;
        }

//        super.setChecksum(Record.algorithm.getValue());
        long v = Record.algorithm.getValue();
        Record.algorithm.reset();
        return v;
    }


}
