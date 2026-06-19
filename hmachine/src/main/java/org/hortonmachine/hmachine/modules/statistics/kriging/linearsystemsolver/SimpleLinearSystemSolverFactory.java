package org.hortonmachine.hmachine.modules.statistics.kriging.linearsystemsolver;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.hortonmachine.gears.utils.math.matrixes.ColumnVector;
import org.hortonmachine.gears.utils.math.matrixes.LinearSystem;
import org.hortonmachine.gears.utils.math.matrixes.MatrixException;
public class SimpleLinearSystemSolverFactory {

    public static ColumnVector solve(double[] knownTerm, double[][] covarianceMatrix, String type) throws MatrixException {

        if (type.equals("default")) {
            ColumnVector knownTermColumn = new ColumnVector(knownTerm);
            return new LinearSystem(covarianceMatrix).solve(knownTermColumn, true);
        }else if (type.equals("math3")) {
            RealMatrix A = new Array2DRowRealMatrix(covarianceMatrix);
            RealVector b = new ArrayRealVector(knownTerm);

            // Solve using QR Decomposition
            DecompositionSolver solver = new QRDecomposition(A).getSolver();
            RealVector solution = solver.solve(b);
            return  new ColumnVector(solution.toArray());
        }else if(type.equals("prec")) {
            RealMatrix A = new Array2DRowRealMatrix(covarianceMatrix);
            RealVector b = new ArrayRealVector(knownTerm);
            SingularValueDecomposition svd = new SingularValueDecomposition(A);
            double rcond = 0.99;
            double[] singularValues = svd.getSingularValues();
            double threshold = rcond * singularValues[0]; // Assuming singular values are sorted in descending order

            for (int i = 0; i < singularValues.length; i++) {
                if (singularValues[i] < threshold) {
                    singularValues[i] = 0;
                }
            }
            RealMatrix S = svd.getS();
            for (int i = 0; i < S.getRowDimension(); i++) {
                if (S.getEntry(i, i) != 0.0) {
                    S.setEntry(i, i, 1.0 / S.getEntry(i, i));
                }
            }
            RealMatrix pseudoInverse = svd.getVT().multiply(S.transpose()).multiply(svd.getU().transpose());
            RealVector solution = pseudoInverse.operate(b);
            return  new ColumnVector(solution.toArray());
        }else if(type.equals("Cholesky")) {
        	RealMatrix A = new Array2DRowRealMatrix(covarianceMatrix);
        	CholeskyDecomposition cholesky = new CholeskyDecomposition(A);
        	RealVector solution = cholesky.getSolver().solve(new ArrayRealVector(knownTerm));
        	return new ColumnVector(solution.toArray());
        }
        return null;
    }
}