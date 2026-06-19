package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.curvefitter;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.EvaluationRmsChecker;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealVector;

public class VariogramFitter extends AbstractCurveFitter {
	private ParametricUnivariateFunction myFunction = null;
	private LeastSquaresProblem problem = null;
	private int nEvaluation = 0;
	private int nIteration = 0;
	private double rms = 0.0;
	private RealVector residuals;
	double tol = Math.pow(0.1, 9);
	double sillMin = 1e-6;
	double rangeMin = 1e-6;
	double nuggetMin = 0.0;

	private VariogramFitter() {
		// make constructor private to prevento inizializzation without the parametric
		// function.
	}

	public VariogramFitter(ParametricUnivariateFunction myFun) {
		this.myFunction = myFun;
	}

	@Override
	protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {
		final int len = points.size();
		final double[] target = new double[len];
		final double[] x = new double[len];
		final double[] weights = new double[len];

		/*
		 * TODO: quali valori iniziali? da dove posso ricavarli?
		 *
		 * sill = 0.9 * max(semivarianze) nugget = 0 range = vettore t.c. la differenza
		 * fra semivarianza e sill, il minimo di queste differenze => range cheint
		 * corrisponde a questa differenza
		 */
		double maxRange = 0;
		int i = 0;
		for (WeightedObservedPoint point : points) {
			target[i] = point.getY();
			x[i] = point.getX();
			if (x[i] > maxRange) {
				maxRange = x[i];
			}
			weights[i] = point.getWeight();
			i += 1;
		}

		double initialSill = 0.9 * Arrays.stream(target).max().getAsDouble();

		double initialNugget = 0.0;

		double[] semivarianceMinusSill = Arrays.stream(target).map(v -> Math.abs(v - initialSill)).toArray();
		int index = 0;
		double min = semivarianceMinusSill[index];

		for (int j = 1; j < semivarianceMinusSill.length; j++) {
			if (semivarianceMinusSill[j] <= min) {
				min = semivarianceMinusSill[j];
				index = j;
			}
		}
		double initialRange = x[index];
		final double[] initialGuess = { initialSill, initialRange, initialNugget };
		// final double[] initialGuess = { 1000.0, 10.0, 0.0000001 };
		final AbstractCurveFitter.TheoreticalValuesFunction model = new AbstractCurveFitter.TheoreticalValuesFunction(
				myFunction, points);
		problem = new LeastSquaresBuilder().maxEvaluations(1000).maxIterations(1000).start(initialGuess).target(target)
				.model(model.getModelFunction(), model.getModelFunctionJacobian()).weight(new DiagonalMatrix(weights))
				.checker(new EvaluationRmsChecker(tol, tol))
				.parameterValidator(new KrigingParamValidator(new double[] { sillMin, rangeMin, nuggetMin },
						new double[] { Double.POSITIVE_INFINITY, maxRange, Double.POSITIVE_INFINITY }))
				.lazyEvaluation(false).build();

		return problem;

	}

	@Override
	public double[] fit(Collection<WeightedObservedPoint> points) {
		double[] optimalValues = null;
		LevenbergMarquardtOptimizer lmo = new LevenbergMarquardtOptimizer();

		LeastSquaresOptimizer.Optimum lsoo = lmo.withCostRelativeTolerance(1.0e-12)
				.withParameterRelativeTolerance(1.0e-12).optimize(getProblem(points));
		optimalValues = lsoo.getPoint().toArray();
		nIteration = lsoo.getIterations();
		nEvaluation = lsoo.getEvaluations();
		rms = lsoo.getRMS();
		residuals = lsoo.getResiduals();

//		LeastSquaresOptimizer optimizer = new GaussNewtonOptimizer()
//				.withDecomposition(GaussNewtonOptimizer.Decomposition.QR);
//		Optimum mm = optimizer.optimize(problem);
		// return getOptimizer().optimize(getProblem(points)).getPoint().toArray();
		return optimalValues;
	}

	public int getNumberOfIteration() {
		return nIteration;
	}

	public int getNumberOfEvaluation() {
		return nEvaluation;
	}

	public double getRMS() {
		return rms;
	}

	public RealVector getResiduals() {
		return residuals;
	}

}
