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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.geotools.feature.SchemaException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

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

@Description("Teorethical semivariogram models.")
@Documentation("vgm.html")
@Author(name = "Giuseppe Formetta, Adami Francesco, Marialaura Bancheri", contact = " http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Kriging, Hydrology")
@Label(HMConstants.STATISTICS)
@Name("kriging")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class TheoreticalVariogram extends HMModel {

    @Description("Distances input Hashmap")
    @In
    public HashMap<Integer, double[]> inDistanceValues;

    @Description("Experimental Variogram input Hashmap")
    @In
    public HashMap<Integer, double[]> inExperimentalVariogramValues;

    @Description("Distances value.")
    @In
    public boolean doCalibrate;

    @Description("Distances value.")
    @In
    public double distance;

    @Description("Sill value.")
    @In
    @Out
    public double sill;

    @Description("Range value.")
    @In
    @Out
    public double range;

    @Description("Nugget value.")
    @In
    @Out
    public double nugget;

    @Description("Model name")
    @In
    @Out
    public String modelName;

    @Description("the output hashmap withe the semivariance")
    @Out
    public double[] result;

    @Description("the output hashmap withe the semivariance")
    @Out
    public double[] observation;

    @Description("the output hashmap with the semivariance")
    @Out
    public HashMap<Integer, double[]> outHMtheoreticalVariogram = new HashMap<Integer, double[]>();;

    ITheoreticalVariogram modelVGM;

    @Execute
    public void process() throws Exception {

        // reading the ID of all the stations
        Set<Entry<Integer, double[]>> entrySet = inDistanceValues.entrySet();
        result = new double[inDistanceValues.size()];
        observation = new double[inDistanceValues.size()];

        for( Entry<Integer, double[]> entry : entrySet ) {
            Integer ID = entry.getKey();

            distance = inDistanceValues.get(ID)[0];

            result[ID] = calculateVGM(modelName, distance, sill, range, nugget);
            observation[ID] = inExperimentalVariogramValues.get(ID)[0];

            storeVariogramResults(ID, result[ID]);
        }

    }

    public double calculateVGM( String model, double distance, double sill, double range, double nug ) {

        modelVGM = ITheoreticalVariogram.create(model);
        modelVGM.init(distance, sill, range, nug);
        double result = modelVGM.computeSemivariance();

        return result;
    }

    private void storeVariogramResults( int ID, double result ) throws SchemaException {

        outHMtheoreticalVariogram.put(ID, new double[]{result});

    }
}
