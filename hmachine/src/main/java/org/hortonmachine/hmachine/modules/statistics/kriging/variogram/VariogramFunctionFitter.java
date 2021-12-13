/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.hmachine.modules.statistics.kriging.variogram;

import java.util.Collection;

import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

public class VariogramFunctionFitter {

    private VariogramFunction function;
    private double initSill;
    private double initRange;
    private double initNugget;

    public VariogramFunctionFitter( VariogramFunction function, double initSill, double initRange, double initNugget ) {
        this.function = function;
        this.initSill = initSill;
        this.initRange = initRange;
        this.initNugget = initNugget;
    }

    public double[] fit( Collection<double[]> allValues ) {
        final WeightedObservedPoints obs = new WeightedObservedPoints();
        for( double[] ds : allValues ) {
            obs.add(ds[0], ds[1]);
        }

        SimpleCurveFitter curveFitter = SimpleCurveFitter.create(function, new double[]{initSill, initRange, initNugget});
        double[] fit = curveFitter.fit(obs.toList());
        return fit;
    }

}
