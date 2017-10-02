/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.jeff;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.ParameterBox;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class StatisticJeff {

    private ParameterBox fixedParams = null;
    private double tpmax = 0f;
    private final IHMProgressMonitor pm;

    /**
     * @param fixedParameters
     * @param tp_max
     * @param pm
     */
    public StatisticJeff( ParameterBox fixedParameters, double tp_max, IHMProgressMonitor pm ) {
        fixedParams = fixedParameters;
        tpmax = tp_max;
        this.pm = pm;
    }

    public double[][] calculateJeff() {
        pm.message("Calculating Jeff...");
        double n_idf = fixedParams.getN_idf();
        double a_idf = fixedParams.getA_idf();

        /*
         * multiplied by 1/3600 1/(1000*3600) gives us Jeff in m/s
         */
        double J = a_idf * Math.pow(tpmax / 3600.0, n_idf - 1) / (1000.0 * 3600.0);
        double h = a_idf * Math.pow(tpmax / 3600.0, n_idf) / 1000.0;
        double[][] result = new double[1][2];
        result[0][0] = J;
        result[0][1] = h;

        return result;
    }
}
