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

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.ITheoreticalVariogram;

public class VariogramFunction implements ParametricUnivariateFunction {

    private String variogramType;

    public VariogramFunction( String variogramType ) {
        this.variogramType = variogramType;
    }

    @Override
    public double value( double x, double... parameters ) {
        // the order has to be sill, range, nugget
        ITheoreticalVariogram variogram = ITheoreticalVariogram.create(variogramType);
        variogram.init(x, parameters[0], parameters[1], parameters[2]);
        return variogram.computeSemivariance();
    }

    @Override
    public double[] gradient( double x, double... parameters ) {
        // the order has to be sill, range, nugget
        ITheoreticalVariogram variogram = ITheoreticalVariogram.create(variogramType);
        variogram.init(x, parameters[0], parameters[1], parameters[2]);
        return variogram.computeJacobian();
    }

}
