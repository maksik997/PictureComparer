package pl.magzik.algorithms;

import pl.magzik.algorithms.dct.Quantifier;
import pl.magzik.algorithms.dct.Transformer;

/**
 * Provides a pipeline for applying Discrete Cosine Transform (DCT) and quantization
 * to a matrix of image coefficients.
 * <p>
 * This class first performs DCT on the input matrix and then applies quantization
 * using a quantization matrix.
 * </p>
 */
public class DCT {

    private final Quantifier quantifier;
    private final Transformer transformer;

    private DCT(Quantifier quantifier, Transformer transformer) {
        this.quantifier = quantifier;
        this.transformer = transformer;
    }

    /**
     * Applies DCT and quantization to the given matrix.
     * <p>
     * This method creates a new instance of {@link Quantifier} and {@link Transformer}
     * and applies them sequentially to the input matrix.
     * </p>
     *
     * @param matrix the matrix of image coefficients to process
     * @return the quantized matrix after applying DCT
     */
    public static double[][] apply(double[][] matrix) {
        Quantifier quantifier = new Quantifier();
        Transformer transformer = new Transformer();
        return new DCT(quantifier, transformer).applyInternal(matrix);
    }

    private double[][] applyInternal(double[][] matrix) {
        double[][] coeffs = transformer.transform(matrix);
        return quantifier.quantize(coeffs);
    }
}
