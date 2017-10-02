package org.hortonmachine.gears.utils.math.matrixes;


/**
 * From: Java Number Cruncher
 * The Java Programmer's Guide to Numerical Computation
 * by Ronald Mak 
 *
 * A matrix that can be inverted.  Also, compute its determinant,
 * norm, and condition number.
 */
public class InvertibleMatrix extends LinearSystem
{
    /**
     * Constructor.
     * @param n the number of rows = the number of columns
     */
    public InvertibleMatrix(int n) { super(n); }

    /**
     * Constructor.
     * @param values the array of values
     */
    public InvertibleMatrix(double values[][]) { super(values); }

    /**
     * Compute the inverse of this matrix.
     * @return the inverse matrix
     * @throws matrix.MatrixException if an error occurred
     */
    public InvertibleMatrix inverse() throws MatrixException
    {
        InvertibleMatrix inverse  = new InvertibleMatrix(nRows);
        IdentityMatrix   identity = new IdentityMatrix(nRows);

        // Compute each column of the inverse matrix
        // using columns of the identity matrix.
        for (int c = 0; c < nCols; ++c) {
            ColumnVector col = solve(identity.getColumn(c), true);
            inverse.setColumn(col, c);
        }

        return inverse;
    }

    /**
     * Compute the determinant.
     * @return the determinant
     * @throws matrix.MatrixException if an error occurred
     */
    public double determinant() throws MatrixException
    {
        decompose();

        // Each row exchange during forward elimination flips the sign
        // of the determinant, so check for an odd number of exchanges.
        double determinant = ((exchangeCount & 1) == 0) ? 1 : -1;

        // Form the product of the diagonal elements of matrix U.
        for (int i = 0; i < nRows; ++i) {
            int pi = permutation[i];        // permuted index
            determinant *= LU.at(pi, i);
        }

        return determinant;
    }

    /**
     * Compute the Euclidean norm of this matrix.
     * @return the norm
     */
    public double norm()
    {
        double sum = 0;

        for (int r = 0; r < nRows; ++r) {
            for (int c = 0; c < nCols; ++c) {
                double v = values[r][c];
                sum += v*v;
            }
        }

        return (double) Math.sqrt(sum);
    }

    /**
     * Compute the condition number based on the Euclidean norm.
     * @return the condition number
     */
    public double condition() throws MatrixException
    {
        return norm() * inverse().norm();
    }
}