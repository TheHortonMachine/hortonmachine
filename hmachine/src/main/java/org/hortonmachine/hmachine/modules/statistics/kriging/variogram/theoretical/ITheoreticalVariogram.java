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

/**
 * The  variogram interface.
 */
public interface ITheoreticalVariogram {

    public static final String POWER = "power";
    public static final String LOGARITHMIC = "logarithmic";
    public static final String LINEAR = "linear";
    public static final String EXPONENTIAL = "exponential";
    public static final String GAUSSIAN = "gaussian";
    public static final String TYPES = LINEAR + "," + EXPONENTIAL + "," + LOGARITHMIC + "," + POWER + "," + GAUSSIAN;

    void init(double dist, double sill, double range, double nug);
    
    double computeSemivariance();

    double[] computeJacobian();

    public static ITheoreticalVariogram create( String type) {
        ITheoreticalVariogram model = null;

        if (EXPONENTIAL.equals(type)) {
            model = new Exponential();
        } else if (LINEAR.equals(type)) {
            model = new Linear();
        } else if (LOGARITHMIC.equals(type)) {
            model = new Logarithmic();
        } else if (POWER.equals(type)) {
            model = new Power();
        } else if (GAUSSIAN.equals(type)) {
            model = new Gaussian();
        } else {
            throw new ModelsIllegalargumentException("Not a valid type: " + type, model);
        }

        return model;

    }
}