/* This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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

import static org.hortonmachine.gears.libs.modules.Variables.*;
import static org.hortonmachine.gears.libs.modules.Variables.COSINE;
import static org.hortonmachine.gears.libs.modules.Variables.DISTANCE;
import static org.hortonmachine.gears.libs.modules.Variables.EPANECHNIKOV;
import static org.hortonmachine.gears.libs.modules.Variables.GAUSSIAN;
import static org.hortonmachine.gears.libs.modules.Variables.INVERSE_DISTANCE;
import static org.hortonmachine.gears.libs.modules.Variables.QUARTIC;
import static org.hortonmachine.gears.libs.modules.Variables.TRIANGULAR;
import static org.hortonmachine.gears.libs.modules.Variables.TRIWEIGHT;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.ITheoreticalVariogram;
import org.locationtech.jts.geom.Coordinate;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Measurements data coach.")
//@Documentation("Kriging.html")
@Author(name = "Silvia Franceschi, Andrea Antonello")
@Keywords("Kriging, Variogram, Interpolation")
@Label("")
@Name("stationdatacoach")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class OmsKrigingInterpolator extends HMModel {

    @Description("Static target points ids to coordinates map for current timestep.")
    @In
    public HashMap<Integer, Coordinate> inTargetPointsIds2CoordinateMap;

    @Description("Valid station ids to coordinates map for current timestep.")
    @In
    public HashMap<Integer, Coordinate> inStationIds2CoordinateMap;

    @Description("Measurement data for current timestep and station ids.")
    @In
    public HashMap<Integer, double[]> inStationIds2ValueMap;

    @Description("Association of target points with their valid stations and distance.")
    @In
    public HashMap<Integer, TargetPointAssociation> inTargetPointId2AssociationMap;

    @Description("Interpolated data for current timestep and target point ids.")
    @Out
    public HashMap<Integer, double[]> outTargetIds2ValueMap;

    @Description("Variogram mode.")
    @UI("combo:" + KRIGING_EXPERIMENTAL_VARIOGRAM + "," + KRIGING_DEFAULT_VARIOGRAM)
    @In
    public String pMode = KRIGING_DEFAULT_VARIOGRAM;
    
    @Description("Theoretical Variogram type.")
    @UI("combo:" + ITheoreticalVariogram.TYPES)
    @In
    public String pTheoreticalVariogramType = ITheoreticalVariogram.EXPONENTIAL;
    
    @Description("Specified cutoff for experimental variogram.")
    @In
    public double pCutoff;

    @Description("Number of bins to consider in the anlysis for the experimental variogram.")
    @In
    public int pBins;
    
    
//    public static final String OMSKRIGING_pIntegralscale_DESCRIPTION = "The integral scale.";
//    public static final String OMSKRIGING_pVariance_DESCRIPTION = "The variance.";
//    public static final String OMSKRIGING_doLogarithmic_DESCRIPTION = "Switch for logaritmic run selection.";
//    public static final String OMSKRIGING_inInterpolationGrid_DESCRIPTION = "The collection of the points in which the data needs to be interpolated.";
//    public static final String OMSKRIGING_pSemivariogramType_DESCRIPTION = "The type of theoretical semivariogram: 0 = Gaussian; 1 = Exponential.";

    @Execute
    public void process() throws Exception {
        outTargetIds2ValueMap = new HashMap<>();
        for( Entry<Integer, TargetPointAssociation> targetId2AssociationEntry : inTargetPointId2AssociationMap.entrySet() ) {
            Integer targetId = targetId2AssociationEntry.getKey();
            TargetPointAssociation association = targetId2AssociationEntry.getValue();

            InterpolationType interpolationType = association.interpolationType;
            switch( interpolationType ) {
            case NOINTERPOLATION_USE_RAW_DATA:
                // we can take the first valid value
                Integer stationId = association.stationIds.get(0);
                double value = inStationIds2ValueMap.get(stationId)[0];
                outTargetIds2ValueMap.put(targetId, new double[]{value});
                break;
            case INTERPOLATION_IDW:
                double idwValue = getIdwInterpolatedValue(association, inStationIds2ValueMap);
                outTargetIds2ValueMap.put(targetId, new double[]{idwValue});

                break;
            case INTERPOLATION_KRIGING:
                if(pMode.equals(KRIGING_EXPERIMENTAL_VARIOGRAM)) {
                    OmsExperimentalVariogram expVariogram = new OmsExperimentalVariogram();
                    expVariogram.inStationIds2CoordinateMap = inStationIds2CoordinateMap;
                    expVariogram.inStationIds2ValueMap = inStationIds2ValueMap;
                    expVariogram.pCutoff = pCutoff;
                    expVariogram.pBins = pBins;
                    expVariogram.process();
                    HashMap<Integer, double[]> outExperimentalVariogram = expVariogram.outExperimentalVariogram;
                    
                    OmsTheoreticalVariogram theoVariogram = new OmsTheoreticalVariogram();
                    theoVariogram.inExperimentalVariogramMap = outExperimentalVariogram;
                    theoVariogram.pTheoreticalVariogramType = pTheoreticalVariogramType;
                    HashMap<Integer, double[]> outTheoreticalVariogram = theoVariogram.outTheoreticalVariogram;
                    double outSill = theoVariogram.outSill;
                    double outRange = theoVariogram.outRange;
                    double outNugget = theoVariogram.outNugget;
                    
                    
                    
                }
                
                
                throw new RuntimeException("Not implemented yet");
//                break;
            case NODATA:
            default:
                // do nothing, the target point will be ignored as it has no data
                break;
            }
        }

    }

    private double getIdwInterpolatedValue( TargetPointAssociation association,
            HashMap<Integer, double[]> inStationIds2ValueMap ) {
        double sumdValue = 0;
        double sumweight = 0;

        List<Double> distances = association.stationDistances;
        List<Integer> stationIds = association.stationIds;

        for( int i = 0; i < stationIds.size(); i++ ) {
            Integer stationId = stationIds.get(i);
            double distance = distances.get(i);
            double value = inStationIds2ValueMap.get(stationId)[0];

            if (distance < 0.00001) {
                distance = 0.00001;
            }
            double weight = (1 / Math.pow(distance, 2));
            sumdValue = sumdValue + value * weight;
            sumweight = sumweight + weight;
        }

        double interpolatedValue = sumdValue / sumweight;
        return interpolatedValue;
    }
}
