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

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;

public class SimpleModelFactory {

    public static Model createModel( String type, double dist, double sill, double range, double nug ) {
        Model model = null;

        if ("exponential".equals(type)) {
            model = new Exponential(dist, sill, range, nug);
        } else if ("gaussian".equals(type)) {
            model = new Gaussian(dist, sill, range, nug);
        } else if ("spherical".equals(type)) {
            model = new Spherical(dist, sill, range, nug);
        } else if ("pentaspherical".equals(type)) {
            model = new Pentaspherical(dist, sill, range, nug);
        } else if ("linear".equals(type)) {
            model = new Linear(dist, sill, range, nug);
        } else if ("circular".equals(type)) {
            model = new Circular(dist, sill, range, nug);
        } else if ("bessel".equals(type)) {
            model = new Bessel(dist, sill, range, nug);
        } else if ("periodic".equals(type)) {
            model = new Periodic(dist, sill, range, nug);
        } else if ("hole".equals(type)) {
            model = new Hole(dist, sill, range, nug);
        } else if ("logarithmic".equals(type)) {
            model = new Logarithmic(dist, sill, range, nug);
        } else if ("power".equals(type)) {
            model = new Power(dist, sill, range, nug);
        } else if ("spline".equals(type)) {
            model = new Spline(dist, sill, range, nug);
        } else {
            throw new ModelsIllegalargumentException("Not a valid type: " + type, model);
        }

        return model;

    }

}
