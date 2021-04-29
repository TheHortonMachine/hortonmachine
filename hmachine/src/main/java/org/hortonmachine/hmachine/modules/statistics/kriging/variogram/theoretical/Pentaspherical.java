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

public class Pentaspherical implements Model {

    double dist;
    double sill;
    double range;
    double nug;

    public Pentaspherical( double dist, double sill, double range, double nug ) {
        this.dist = dist;
        this.sill = sill;
        this.range = range;
        this.nug = nug;
    }

    @Override
    public double computeSemivariance() {

        double result = 0;
        double hr = 0;
        hr = dist / (range);
        if (dist != 0.0) {
            result = nug + sill * (hr * ((15.0 / 8.0) + Math.pow(hr, 3) * ((-5.0 / 4.0) + Math.pow(hr, 5) * (3.0 / 8.0))));
        }
        if (dist >= range) {
            result = sill + nug;
        }
        // System.out.println(func[i]);

        return result;
    }

}
