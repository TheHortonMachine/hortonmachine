/*
 * GNU GPL v3 License
 *
 * Copyright 2016 Marialaura Bancheri
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model;

public class Power implements Model {

	double dist;
	double sill;
	double range;
	double nug;
	boolean isOk = false;

	public Power(double dist, double sill, double range, double nug) {
		this.dist = dist;
		this.sill = sill;
		this.range = range;
		this.nug = nug;
		this.isOk = nug >= 0 && sill >= 0 && range >= 0;

	}

	@Override
	public double computeSemivariance() {
		double result = Double.MAX_VALUE;
		if (isOk) {
			result = nug + sill * (Math.pow(dist, 2));
		}
		if(Double.isInfinite(result)) {
			return Double.MAX_VALUE;
		}
		return result;
	}

	@Override
	public double[] computeGradient() {
		// TODO Auto-generated method stub
		double[] gradient = new double[] { Double.NaN, Double.NaN, Double.NaN };

		if (isOk && !Double.isInfinite(Math.pow(dist, 2))) {
			gradient = new double[] { Math.pow(dist, range), sill * Math.pow(dist, 2) * Math.log(dist), 1.0 };
		}
		return gradient;
	}

}
