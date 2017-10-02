package org.hortonmachine.gears.utils.math.matrixes;

/**
 * From: Java Number Cruncher
 * The Java Programmer's Guide to Numerical Computation
 * by Ronald Mak 
 */
public class IdentityMatrix extends SquareMatrix
{
    /**
     * Constructor.
     * @param n the number of rows == the number of columns
     */
    public IdentityMatrix(int n)
    {
        super(n);
        for (int i = 0; i < n; ++i) values[i][i] = 1;
    }

    /**
     * Convert a square matrix into an identity matrix.
     * @param sm the square matrix to convert
     */
    public static void convert(SquareMatrix sm)
    {
        for (int r = 0; r < sm.nRows; ++r) {
            for (int c = 0; c < sm.nCols; ++c) {
                sm.values[r][c] = (r == c) ? 1 : 0;
            }
        }
    }
}