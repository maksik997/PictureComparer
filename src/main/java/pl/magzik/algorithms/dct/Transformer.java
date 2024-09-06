package pl.magzik.algorithms.dct;

import org.jtransforms.dct.DoubleDCT_1D;

/**
 * Provides methods to perform Discrete Cosine Transform (DCT) on matrices and vectors.
 * <p>
 * This class uses the JTransforms library for efficient 1D DCT operations.
 * </p>
 */
public class Transformer {

    /**
     * Transforms the given matrix using 2D Discrete Cosine Transform (DCT).
     *
     * @param matrix the matrix to transform
     * @return the transformed matrix
     */
    public double[][] transform(double[][] matrix) {
        int m = matrix.length, n = matrix[0].length;
        double[][] transformed = new double[m][n];

        for (int i = 0; i < m; i++)
            transformed[i] = transform(matrix[i]);

        for (int j = 0; j < n; j++) {
            double[] col = new double[m];
            for (int i = 0; i < m; i++)
                col[i] = transformed[i][j];
            col = transform(col);
            for (int i = 0; i < m; i++)
                transformed[i][j] = col[i];
        }

        return transformed;
    }

    /**
     * Transforms the given 1D vector using Discrete Cosine Transform (DCT).
     *
     * @param vector the vector to transform
     * @return the transformed vector
     */
    public double[] transform(double[] vector) {
        DoubleDCT_1D dct = new DoubleDCT_1D(vector.length);
        double[] copy = vector.clone();
        dct.forward(copy, true);
        return copy;
    }

}
