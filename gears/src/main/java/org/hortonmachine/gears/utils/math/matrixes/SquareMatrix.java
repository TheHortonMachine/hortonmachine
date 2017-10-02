package org.hortonmachine.gears.utils.math.matrixes;

/**
 * From: Java Number Cruncher
 * The Java Programmer's Guide to Numerical Computation
 * by Ronald Mak 
 *
 *
 * A square matrix.
 */
public class SquareMatrix extends Matrix
{
    //--------------//
    // Constructors //
    //--------------//

    /**
     * Constructor.
     * @param n the number of rows == the number of columns
     */
    public SquareMatrix(int n) { super(n, n); }

    /**
     * Constructor.
     * @param m the matrix (only the upper left square used)
     */
    private SquareMatrix(Matrix m) { set(m); }

    /**
     * Constructor.
     * @param values the array of values
     */
    public SquareMatrix(double values[][]) { set(values); }

    //---------//
    // Setters //
    //---------//

    /**
     * Set this square matrix from another matrix.  Note that this
     * matrix will reference the values of the argument matrix.  If
     * the values are not square, only the upper left square is used.
     * @param values the 2-d array of values
     */
    private void set(Matrix m)
    {
        this.nRows  = this.nCols = Math.min(m.nRows, m.nCols);
        this.values = m.values;
    }

    /**
     * Set this square matrix from a 2-d array of values.  If the
     * values are not square, only the upper left square is used.
     * @param values the 2-d array of values
     */
    protected void set(double values[][])
    {
        super.set(values);
        nRows = nCols = Math.min(nRows, nCols);
    }

    //-------------------//
    // Matrix operations //
    //-------------------//

    /**
     * Add another square matrix to this matrix.
     * @param sm the square matrix addend
     * @return the sum matrix
     * @throws numbercruncher.MatrixException for invalid size
     */
    public SquareMatrix add(SquareMatrix sm) throws MatrixException
    {
        return new SquareMatrix(super.add(sm));
    }

    /**
     * Subtract another square matrix from this matrix.
     * @param sm the square matrix subrrahend
     * @return the difference matrix
     * @throws numbercruncher.MatrixException for invalid size
     */
    public SquareMatrix subtract(SquareMatrix sm)
        throws MatrixException
    {
        return new SquareMatrix(super.subtract(sm));
    }

    /**
     * Multiply this square matrix by another square matrix.
     * @param sm the square matrix multiplier
     * @return the product matrix
     * @throws numbercruncher.MatrixException for invalid size
     */
    public SquareMatrix multiply(SquareMatrix sm)
        throws MatrixException
    {
        return new SquareMatrix(super.multiply(sm));
    }
}