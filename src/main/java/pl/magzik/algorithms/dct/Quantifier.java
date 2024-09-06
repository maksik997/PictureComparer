package pl.magzik.algorithms.dct;

/**
 * Represents a quantization process used in image processing, typically for JPEG compression.
 * <p>
 * This class provides methods to quantize a matrix of DCT coefficients using a specified
 * quantization matrix.
 * </p>
 */
public class Quantifier {

    private final int[][] quantizationMatrix;

    /**
     * Creates a Quantifier instance with the specified quantization matrix.
     *
     * @param quantizationMatrix the quantization matrix to use for quantization
     */
    public Quantifier(int[][] quantizationMatrix) {
        this.quantizationMatrix = quantizationMatrix;
    }

    /**
     * Creates a Quantifier instance using the default JPEG quantization matrix.
     */
    public Quantifier() {
        this(new int[][]{
            {16, 11, 10, 16, 24, 40, 51, 61},
            {12, 12, 14, 19, 26, 58, 60, 55},
            {14, 13, 16, 24, 40, 57, 69, 56},
            {14, 17, 22, 29, 51, 87, 80, 62},
            {18, 22, 37, 56, 68, 109, 103, 77},
            {24, 35, 55, 64, 81, 104, 113, 92},
            {49, 64, 78, 87, 103, 121, 120, 101},
            {72, 92, 95, 98, 112, 100, 103, 99}
        });
    }

    /**
     * Quantizes the given matrix of DCT coefficients using the quantization matrix.
     *
     * @param coeffs the matrix of DCT coefficients to quantize
     * @return the quantized matrix
     * @throws IllegalArgumentException if the dimensions of the coefficient matrix and
     *                                  quantization matrix do not match
     */
    public double[][] quantize(double[][] coeffs) {
        int m = coeffs.length, n = coeffs[0].length;
        if (m != quantizationMatrix.length || n != quantizationMatrix[0].length) {
            throw new IllegalArgumentException("Coefficient matrix and quantization matrix dimensions must match");
        }

        double[][] result = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = Math.round(coeffs[i][j] / quantizationMatrix[i % quantizationMatrix.length][j % quantizationMatrix[0].length]);
            }
        }

        return result;
    }
}
