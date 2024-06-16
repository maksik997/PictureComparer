package pl.magzik.Algorithms;

import java.util.Arrays;
import java.util.Objects;

public class DCT {

    private static final int[][] JPEG_QUANTIZATION_TABLE = {
        {16, 11, 10, 16, 24, 40, 51, 61},
        {12, 12, 14, 19, 26, 58, 60, 55},
        {14, 13, 16, 24, 40, 57, 69, 56},
        {14, 17, 22, 29, 51, 87, 80, 62},
        {18, 22, 37, 56, 68, 109, 103, 77},
        {24, 35, 55, 64, 81, 104, 113, 92},
        {49, 64, 78, 87, 103, 121, 120, 101},
        {72, 92, 95, 98, 112, 100, 103, 99}
    };

    public static double[][] quantization(double[][] coeffs) {
        int m = coeffs.length, n = coeffs[0].length;

        double[][] result = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = Math.round(coeffs[i][j] / JPEG_QUANTIZATION_TABLE[i % 8][j % 8]);
            }
        }

        return result;
    }

    public static double[][] transform(double[][] matrix) {
        Objects.requireNonNull(matrix);
        int m = matrix.length, n = matrix[0].length;
        double[][] result = new double[m][n];

        for (int i = 0; i < m; i++) {
            double[] row = matrix[i];
            result[i] = transform(row);
        }

        for (int j = 0; j < n; j++) {
            double[] col = new double[m];
            for (int i = 0; i < m; i++) {
                col[j] = result[i][j];
            }
            double[] transCol = transform(col);
            for (int i = 0; i < m; i++) {
                result[i][j] = transCol[i];
            }
        }

        return result;
    }

    public static double[] transform(double[] vector) {
        Objects.requireNonNull(vector);

        int n = vector.length;
        double[] result = new double[n];
        double[] temp = new double[n];

        if (n == 1) {
            result[0] = vector[0];
            return result;
        }

        int half = n / 2;

        for (int i = 0; i < half; i++) {
            double x = vector[i];
            double y = vector[n - i - 1];
            temp[i] = x + y;
            temp[i + half] = (x - y) / (2 * Math.cos((i + 0.5) * Math.PI / n));
        }

        double[] even = transform(Arrays.copyOfRange(temp, 0, half));
        double[] odd = transform(Arrays.copyOfRange(temp, half, n));

        for (int i = 0; i < half; i++) {
            result[2 * i] = even[i];
            result[2 * i + 1] = odd[i];
        }

        return result;
    }
}
