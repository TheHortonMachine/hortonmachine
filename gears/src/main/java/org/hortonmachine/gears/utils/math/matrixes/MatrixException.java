package org.hortonmachine.gears.utils.math.matrixes;

/**
 * From: Java Number Cruncher
 * The Java Programmer's Guide to Numerical Computation
 * by Ronald Mak 
 */
public class MatrixException extends Exception
{
    public static final String INVALID_INDEX =
                                        "Invalid index.";
    public static final String INVALID_DIMENSIONS =
                                        "Invalid matrix dimensions.";
    public static final String ZERO_ROW =
                                        "Matrix has a zero row.";
    public static final String SINGULAR =
                                        "Matrix is singular.";
    public static final String NO_CONVERGENCE =
                                        "Solution did not converge.";

    /**
     * Constructor.
     * @param msg the error message
     */
    public MatrixException(String msg) { super(msg); }
}
