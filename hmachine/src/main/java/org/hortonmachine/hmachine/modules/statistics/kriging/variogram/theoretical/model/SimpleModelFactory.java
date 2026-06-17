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

import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.VariogramParameters;

public class SimpleModelFactory {

	public static Model createModel(String type, double dist, double sill, double range, double nug) {
		Model model = null;
		if ("exponential".equals(type)) {
			model = new Exponential(dist, sill, range, nug);
		}
		if ("gaussian".equals(type)) {
			model = new Gaussian(dist, sill, range, nug);
		}
		if ("spherical".equals(type)) {
			model = new Spherical(dist, sill, range, nug);
		}
		if ("pentaspherical".equals(type)) {
			model = new Pentaspherical(dist, sill, range, nug);
		}
		if ("linear".equals(type)) {
			model = new Linear(dist, sill, range, nug);
		}
		if ("circular".equals(type)) {
			model = new Circular(dist, sill, range, nug);
		}
		if ("bessel".equals(type)) {
			model = new Bessel(dist, sill, range, nug);
		}
		if ("periodic".equals(type)) {
			model = new Periodic(dist, sill, range, nug);
		}
		if ("hole".equals(type)) {
			model = new Hole(dist, sill, range, nug);
		}
		if ("logarithmic".equals(type)) {
			model = new Logarithmic(dist, sill, range, nug);
		}
		if ("power".equals(type)) {
			model = new Power(dist, sill, range, nug);
		}
		if ("spline".equals(type)) {
			model = new Spline(dist, sill, range, nug);
		}

		return model;
	}

	public static Model createModel(VariogramParameters vp, double dist) {
		return createModel(vp.getModelName(), dist, vp.getSill(), vp.getRange(), vp.getNugget());
	}
}
