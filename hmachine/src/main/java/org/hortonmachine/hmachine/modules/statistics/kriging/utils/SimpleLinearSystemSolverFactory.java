package org.hortonmachine.hmachine.modules.statistics.kriging.utils;

import org.hortonmachine.gears.utils.math.matrixes.ColumnVector;
import org.hortonmachine.gears.utils.math.matrixes.LinearSystem;
import org.hortonmachine.gears.utils.math.matrixes.MatrixException;

public class SimpleLinearSystemSolverFactory {

    public static ColumnVector solve(double[] knownTerm, double[][] covarianceMatrix, String type) throws MatrixException {

        if (type.equals("default")) {
            ColumnVector knownTermColumn = new ColumnVector(knownTerm);
            return new LinearSystem(covarianceMatrix).solve(knownTermColumn, true);
        }

        return null;
    }
}