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
package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical;

public class Logarithmic implements ITheoreticalVariogram {

    double dist;
    double sill;
    double range;
    double nug;

    public void init( double dist, double sill, double range, double nug ) {
        this.dist = dist;
        this.sill = sill;
        this.range = range;
        this.nug = nug;
    }

    @Override
    public double computeSemivariance() {
        double result = 0;
        if (dist != 0.0) {
            result = nug + sill * (Math.log(dist / range));
        }
        return result;
    }

    @Override
    public double[] computeJacobian() {
        if (dist != 0.0) {
            return new double[]{//
                    Math.log(dist / range), // dSill
                    sill * (range / dist) * (-dist / (range * range)), // dRange
                    1.0 // dNug
            };
        }
        return new double[]{0.0, 0.0, 0.0};
    }

}
