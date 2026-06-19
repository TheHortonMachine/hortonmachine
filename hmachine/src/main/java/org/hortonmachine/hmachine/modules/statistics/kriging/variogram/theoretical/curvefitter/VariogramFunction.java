package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.curvefitter;

import java.util.ArrayList;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model.Model;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model.SimpleModelFactory;

public class VariogramFunction implements ParametricUnivariateFunction {

	String type;

	public VariogramFunction(String type) {
		this.type = type;
	}

	@Override
	public double value(double x, double... parameters) {
		// TODO: check if there is 3 parameters.
		Model variogram = SimpleModelFactory.createModel(type, x, parameters[0], parameters[1], parameters[2]);
		return variogram.computeSemivariance();
	}

	@Override
	public double[] gradient(double x, double... parameters) {
		Model variogram = SimpleModelFactory.createModel(type, x, parameters[0], parameters[1], parameters[2]);
		return variogram.computeGradient();
	}

	public ArrayList<WeightedObservedPoint> filterPoint(ArrayList<WeightedObservedPoint> points) {
		ArrayList<WeightedObservedPoint> newPoint = new ArrayList<>();
		if (type == "logarithmic" || type == "power") {
			for (WeightedObservedPoint point : points) {
				if (point.getX() != 0) {
					newPoint.add(point);
				}
			}
			return newPoint;
		}
		return points;
	}

}
