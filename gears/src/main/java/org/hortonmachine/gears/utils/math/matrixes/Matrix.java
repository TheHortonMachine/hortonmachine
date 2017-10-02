package org.hortonmachine.gears.utils.math.matrixes;

/**
 * From: Java Number Cruncher
 * The Java Programmer's Guide to Numerical Computation
 * by Ronald Mak 
 *
 *
 * The matrix class.
 */
public class Matrix
{
    /** number of rows */         protected int   nRows;
    /** number of columns */      protected int   nCols;
    /** 2-d array of  values */   protected double values[][];

    //--------------//
    // Constructors //
    //--------------//

    /**
     * Default constructor.
     */
    protected Matrix() {}

    /**
     * Constructor.
     * @param rowCount the number of rows
     * @param colCount the number of columns
     */
    public Matrix(int rowCount, int colCount)
    {
        nRows  = (rowCount > 0) ? rowCount : 1;
        nCols  = (colCount > 0) ? colCount : 1;
        values = new double[nRows][nCols];
    }

    /**
     * Constructor.
     * @param values the 2-d array of values
     */
    public Matrix(double values[][]) { set(values); }

    //---------//
    // Getters //
    //---------//

    /**
     * Get the row count.
     * @return the row count
     */
    public int rowCount() { return nRows; }

    /**
     * Get the column count.
     * @return the column count
     */
    public int columnCount() { return nCols; }

    /**
     * Get the value of element [r,c] in the matrix.
     * @param r the row index
     * @param c the column index
     * @return the value
     * @throws numbercruncher.MatrixException for an invalid index
     */
    public double at(int r, int c) throws MatrixException
    {
        if ((r < 0) || (r >= nRows) || (c < 0) || (c >= nCols)) {
            throw new MatrixException(MatrixException.INVALID_INDEX);
        }

        return values[r][c];
    }

    /**
     * Get a row of this matrix.
     * @param r the row index
     * @return the row as a row vector
     * @throws numbercruncher.MatrixException for an invalid index
     */
    public RowVector getRow(int r) throws MatrixException
    {
        if ((r < 0) || (r >= nRows)) {
            throw new MatrixException(MatrixException.INVALID_INDEX);
        }

        RowVector rv = new RowVector(nCols);
        for (int c = 0; c < nCols; ++c) {
            rv.values[0][c] = this.values[r][c];
        }

        return rv;
    }

    /**
     * Get a column of this matrix.
     * @param c the column index
     * @return the column as a column vector
     * @throws numbercruncher.MatrixException for an invalid index
     */
    public ColumnVector getColumn(int c) throws MatrixException
    {
        if ((c < 0) || (c >= nCols)) {
            throw new MatrixException(MatrixException.INVALID_INDEX);
        }

        ColumnVector cv = new ColumnVector(nRows);
        for (int r = 0; r < nRows; ++r) {
            cv.values[r][0] = this.values[r][c];
        }

        return cv;
    }

    /**
     * Copy the values of this matrix.
     * @return the values
     */
    public double[][] values() { return values; }

    /**
     * Copy the values of this matrix.
     * @return the copied values
     */
    public double[][] copyValues2D()
    {
        double v[][] = new double[nRows][nCols];

        for (int r = 0; r < nRows; ++r) {
            for (int c = 0; c < nCols; ++c) {
                v[r][c] = values[r][c];
            }
        }

        return v;
    }

    //---------//
    // Setters //
    //---------//

    /**
     * Set the value of element [r,c].
     * @param r the row index
     * @param c the column index
     * @param value the value
     * @throws numbercruncher.MatrixException for an invalid index
     */
    public void set(int r, int c, double value) throws MatrixException
    {
        if ((r < 0) || (r >= nRows) || (c < 0) || (c >= nCols)) {
            throw new MatrixException(MatrixException.INVALID_INDEX);
        }

        values[r][c] = value;
    }

    /**
     * Set this matrix from a 2-d array of values.
     * If the rows do not have the same length, then the matrix
     * column count is the length of the shortest row.
     * @param values the 2-d array of values
     */
    protected void set(double values[][])
    {
        this.nRows  = values.length;
        this.nCols  = values[0].length;
        this.values = values;

        for (int r = 1; r < nRows; ++r) {
            nCols = Math.min(nCols, values[r].length);
        }
    }

    /**
     * Set a row of this matrix from a row vector.
     * @param rv the row vector
     * @param r the row index
     * @throws numbercruncher.MatrixException for an invalid index or
     *                                        an invalid vector size
     */
    public void setRow(RowVector rv, int r) throws MatrixException
    {
        if ((r < 0) || (r >= nRows)) {
            throw new MatrixException(MatrixException.INVALID_INDEX);
        }
        if (nCols != rv.nCols) {
            throw new MatrixException(
                                MatrixException.INVALID_DIMENSIONS);
        }

        for (int c = 0; c < nCols; ++c) {
            this.values[r][c] = rv.values[0][c];
        }
    }

    /**
     * Set a column of this matrix from a column vector.
     * @param cv the column vector
     * @param c the column index
     * @throws numbercruncher.MatrixException for an invalid index or
     *                                        an invalid vector size
     */
    public void setColumn(ColumnVector cv, int c)
        throws MatrixException
    {
        if ((c < 0) || (c >= nCols)) {
            throw new MatrixException(MatrixException.INVALID_INDEX);
        }
        if (nRows != cv.nRows) {
            throw new MatrixException(
                                MatrixException.INVALID_DIMENSIONS);
        }

        for (int r = 0; r < nRows; ++r) {
            this.values[r][c] = cv.values[r][0];
        }
    }

    //-------------------//
    // Matrix operations //
    //-------------------//

    /**
     * Return the transpose of this matrix.
     * @return the transposed matrix
     */
    public Matrix transpose()
    {
        double tv[][] = new double[nCols][nRows];  // transposed values

        // Set the values of the transpose.
        for (int r = 0; r < nRows; ++r) {
            for (int c = 0; c < nCols; ++c) {
                tv[c][r] = values[r][c];
            }
        }

        return new Matrix(tv);
    }

    /**
     * Add another matrix to this matrix.
     * @param m the matrix addend
     * @return the sum matrix
     * @throws numbercruncher.MatrixException for invalid size
     */
    public Matrix add(Matrix m) throws MatrixException
    {
        // Validate m's size.
        if ((nRows != m.nRows) && (nCols != m.nCols)) {
            throw new MatrixException(
                                MatrixException.INVALID_DIMENSIONS);
        }

        double sv[][] = new double[nRows][nCols]; // sum values

        // Compute values of the sum.
        for (int r = 0; r < nRows; ++r) {
            for (int c = 0; c < nCols; ++c) {
                sv[r][c] = values[r][c] + m.values[r][c];
            }
        }

        return new Matrix(sv);
    }

    /**
    * Subtract another matrix from this matrix.
    * @param m the matrix subrrahend
    * @return the difference matrix
    * @throws numbercruncher.MatrixException for invalid size
    */
    public Matrix subtract(Matrix m) throws MatrixException
    {
        // Validate m's size.
        if ((nRows != m.nRows) && (nCols != m.nCols)) {
            throw new MatrixException(
                                MatrixException.INVALID_DIMENSIONS);
        }

        double dv[][] = new double[nRows][nCols]; // difference values

        // Compute values of the difference.
        for (int r = 0; r < nRows; ++r) {
            for (int c = 0; c < nCols; ++c) {
                dv[r][c] = values[r][c] - m.values[r][c];
            }
        }

        return new Matrix(dv);
    }

    /**
     * Multiply this matrix by a constant.
     * @param k the constant
     * @return the product matrix
     */
    public Matrix multiply(double k)
    {
        double pv[][] = new double[nRows][nCols]; // product values

        // Compute values of the product.
        for (int r = 0; r < nRows; ++r) {
            for (int c = 0; c < nCols; ++c) {
                pv[r][c] = k*values[r][c];
            }
        }

        return new Matrix(pv);
    }

    /**
     * Multiply this matrix by another matrix.
     * @param m the matrix multiplier
     * @return the product matrix
     * @throws numbercruncher.MatrixException for invalid size
     */
    public Matrix multiply(Matrix m) throws MatrixException
    {
        // Validate m's dimensions.
        if (nCols != m.nRows) {
            throw new MatrixException(
                                MatrixException.INVALID_DIMENSIONS);
        }

        double pv[][] = new double[nRows][m.nCols];  // product values

        // Compute values of the product.
        for (int r = 0; r < nRows; ++r) {
            for (int c = 0; c < m.nCols; ++c) {
                double dot = 0;
                for (int k = 0; k < nCols; ++k) {
                    dot += values[r][k] * m.values[k][c];
                }
                pv[r][c] = dot;
            }
        }

        return new Matrix(pv);
    }

    /**
     * Multiply this matrix by a column vector: this*cv
     * @param cv the column vector
     * @return the product column vector
     * @throws numbercruncher.MatrixException for invalid size
     */
    public ColumnVector multiply(ColumnVector cv)
        throws MatrixException
    {
        // Validate cv's size.
        if (nRows != cv.nRows) {
            throw new MatrixException(
                                MatrixException.INVALID_DIMENSIONS);
        }

        double pv[] = new double[nRows];   // product values

        // Compute the values of the product.
        for (int r = 0; r < nRows; ++r) {
            double dot = 0;
            for (int c = 0; c < nCols; ++c) {
                dot += values[r][c] * cv.values[c][0];
            }
            pv[r] = dot;
        }

        return new ColumnVector(pv);
    }

    /**
     * Multiply a row vector by this matrix: rv*this
     * @param rv the row vector
     * @return the product row vector
     * @throws numbercruncher.MatrixException for invalid size
     */
    public RowVector multiply(RowVector rv) throws MatrixException
    {
        // Validate rv's size.
        if (nCols != rv.nCols) {
            throw new MatrixException(
                                MatrixException.INVALID_DIMENSIONS);
        }

        double pv[] = new double[nRows];  // product values

        // Compute the values of the product.
        for (int c = 0; c < nCols; ++c) {
            double dot = 0;
            for (int r = 0; r < nRows; ++r) {
                dot += rv.values[0][r] * values[r][c];
            }
            pv[c] = dot;
        }

        return new RowVector(pv);
    }
}