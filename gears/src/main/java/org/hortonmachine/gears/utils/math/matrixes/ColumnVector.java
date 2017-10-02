package org.hortonmachine.gears.utils.math.matrixes;

/**
 * From: Java Number Cruncher
 * The Java Programmer's Guide to Numerical Computation
 * by Ronald Mak 
 * 
 * A column vector.
 */
public class ColumnVector extends Matrix
{
    //--------------//
    // Constructors //
    //--------------//

    /**
     * Constructor.
     * @param n the number of elements
     */
    public ColumnVector(int n) { super(n, 1); }

    /**
     * Constructor.
     * @param values the array of values
     */
    public ColumnVector(double values[]) { set(values); }

    /**
     * Constructor.
     * @param m the matrix (only the first column used)
     */
    private ColumnVector(Matrix m) { set(m); }

    //---------//
    // Getters //
    //---------//

    /**
     * Return this column vector's size.
     */
    public int size() { return nRows; }

    /**
     * Return the i'th value of the vector.
     * @param i the index
     * @return the value
     */
    public double at(int i) { return values[i][0]; }

    /**
     * Copy the values of this matrix.
     * @return the copied values
     */
    public double[] copyValues1D()
    {
        double v[] = new double[nRows];

        for (int r = 0; r < nRows; ++r) {
            v[r] = values[r][0];
        }

        return v;
    }

    //---------//
    // Setters //
    //---------//

    /**
     * Set this column vector from a matrix.
     * Only the first column is used.
     * @param m the matrix
     */
    private void set(Matrix m)
    {
        this.nRows  = m.nRows;
        this.nCols  = 1;
        this.values = m.values;
    }

    /**
     * Set this column vector from an array of values.
     * @param values the array of values
     */
    protected void set(double values[])
    {
        this.nRows  = values.length;
        this.nCols  = 1;
        this.values = new double[nRows][1];

        for (int r = 0; r < nRows; ++r) {
            this.values[r][0] = values[r];
        }
    }

    /**
     * Set the value of the i'th element.
     * @param i the index
     * @param value the value
     */
    public void set(int i, double value) { values[i][0] = value; }

    //-------------------//
    // Vector operations //
    //-------------------//

    /**
     * Add another column vector to this column vector.
     * @param cv the other column vector
     * @return the sum column vector
     * @throws numbercruncher.MatrixException for invalid size
     */
    public ColumnVector add(ColumnVector cv) throws MatrixException
    {
        return new ColumnVector(super.add(cv));
    }

    /**
     * Subtract another column vector from this column vector.
     * @param cv the other column vector
     * @return the sum column vector
     * @throws numbercruncher.MatrixException for invalid size
     */
    public ColumnVector subtract(ColumnVector cv)
        throws MatrixException
    {
        return new ColumnVector(super.subtract(cv));
    }

    /**
     * Compute the Euclidean norm.
     * @return the norm
     */
    public double norm()
    {
        double t = 0;

        for (int r = 0; r < nRows; ++r) {
            double v = values[r][0];
            t += v*v;
        }

        return (double) Math.sqrt(t);
    }

    /**
     * Print the vector values.
     */
    public void print()
    {
        for (int r = 0; r < nRows; ++r) {
            System.out.print("  " + values[r][0]);
        }
        System.out.println();
    }
}