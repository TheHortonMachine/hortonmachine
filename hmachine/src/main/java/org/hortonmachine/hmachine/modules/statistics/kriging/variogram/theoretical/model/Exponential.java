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

public class Exponential implements Model {

	double dist;
	double sill;
	double range;
	double nug;
	boolean isOk = false;

	public Exponential(double dist, double sill, double range, double nug) {
		this.dist = dist;
		this.sill = sill;
		this.range = range;
		this.nug = nug;
		this.isOk = nug >= 0 && sill >= 0 && range >= 0;
	}

	@Override
	public double computeSemivariance() {

		double result = Double.NaN;

		if (isOk) {
			result = nug + sill * (1 - (Math.exp(-dist / range)));
		}
		// System.out.println(func[i]);

		return result;
	}

	@Override
	public double[] computeGradient() {
		// TODO Auto-generated method stub
		double[] gradient = new double[] { Double.NaN, Double.NaN, Double.NaN };

		if (isOk) {
			gradient = new double[] { 1 - Math.exp(-(dist / range)),
					-sill * Math.exp(-(dist / range)) * (dist / (range * range)), 1.0 };
		}
		return gradient;

	}

}
