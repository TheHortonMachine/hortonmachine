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
package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.VariogramFunction;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.VariogramFunctionFitter;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.ITheoreticalVariogram;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Teorethical semivariogram models.")
@Documentation("vgm.html")
@Author(name = "Giuseppe Formetta, Adami Francesco, Marialaura Bancheri", contact = " http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Kriging, Hydrology")
@Label(HMConstants.STATISTICS)
@Name("kriging")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class OmsTheoreticalVariogram extends HMModel {

    @Description("Experimental Variogram")
    @In
    public HashMap<Integer, double[]> inExperimentalVariogramMap;

    @Description("Theoretical Variogram type.")
    @UI("combo:" + ITheoreticalVariogram.TYPES)
    @In
    public String pTheoreticalVariogramType = ITheoreticalVariogram.EXPONENTIAL;

    @Description("The Sill value fitted for the selected model.")
    @Out
    public double outSill;

    @Description("The Range value fitted for the selected model.")
    @Out
    public double outRange;

    @Description("The Nugget value fitted for the selected model")
    @Out
    public double outNugget;

    @Description("The Theoretical Variogram. The double array is of the form [distance, variance]")
    @Out
    public HashMap<Integer, double[]> outTheoreticalVariogram = new HashMap<Integer, double[]>();

    @Execute
    public void process() throws Exception {
        // set some initial values
        Collection<double[]> allValues = inExperimentalVariogramMap.values();
        double maxVariance = Double.NEGATIVE_INFINITY;
        double minDistance = Double.POSITIVE_INFINITY;
        for( double[] ds : allValues ) {
            maxVariance = Math.max(ds[1], maxVariance);
            minDistance = Math.min(ds[0], minDistance);
        }
        double initSill = 0.8 * maxVariance;
        double initRange = 1.2 * minDistance;
        double initNugget = 0.0;

        VariogramFunction variogramFunction = new VariogramFunction(pTheoreticalVariogramType);
        VariogramFunctionFitter fitter = new VariogramFunctionFitter(variogramFunction, initSill, initRange, initNugget);
        double[] sillRangeNugget = fitter.fit(allValues);

        for( Entry<Integer, double[]> entry : inExperimentalVariogramMap.entrySet() ) {
            Integer id = entry.getKey();
            double[] values = entry.getValue();
            double distance = values[0];

            ITheoreticalVariogram modelVGM = ITheoreticalVariogram.create(pTheoreticalVariogramType);
            modelVGM.init(distance, sillRangeNugget[0], sillRangeNugget[1], sillRangeNugget[2]);
            double variance = modelVGM.computeSemivariance();

            outTheoreticalVariogram.put(id, new double[]{distance, variance});
        }

    }

}
