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

import static org.hortonmachine.gears.libs.modules.Variables.KRIGING_DEFAULT_VARIOGRAM;
import static org.hortonmachine.gears.libs.modules.Variables.KRIGING_EXPERIMENTAL_VARIOGRAM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.math.interpolation.LinearArrayInterpolator;
import org.hortonmachine.gears.utils.math.interpolation.LinearListInterpolator;
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

    @Description("Measurement data of the previous timesteps.")
    @Out
    public LinkedList<HashMap<Integer, double[]>> inPreviousStationIds2ValueMaps;

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
                if (pMode.equals(KRIGING_EXPERIMENTAL_VARIOGRAM)) {
                    HashMap<Integer, Coordinate> validStationIds2CoordinateMap = new HashMap<>();
                    HashMap<Integer, double[]> validStationIds2ValueMap = new HashMap<>();
                    for( Integer tmpStationId : association.stationIds ) {
                        validStationIds2CoordinateMap.put(tmpStationId, inStationIds2CoordinateMap.get(tmpStationId));

                        double[] finalValues;
                        double[] valueArray = inStationIds2ValueMap.get(tmpStationId);
                        if (inPreviousStationIds2ValueMaps != null) {
                            List<Double> allValues = new ArrayList<>();
                            allValues.add(valueArray[0]);
                            for( int i = 0; i < inPreviousStationIds2ValueMaps.size(); i++ ) {
                                if (i > 0) { // because in 0 the current timestep values are kept
                                    double[] tmpValue = inPreviousStationIds2ValueMaps.get(i).get(tmpStationId);
                                    if (tmpValue != null) {
                                        allValues.add(tmpValue[0]);
                                    }
                                }
                            }
                            finalValues = new double[allValues.size()];
                            for( int i = 0; i < finalValues.length; i++ ) {
                                finalValues[i] = allValues.get(i);
                            }
                        } else {
                            finalValues = valueArray;
                        }
                        validStationIds2ValueMap.put(tmpStationId, finalValues);
                    }

                    NormalizationStore store = normalizeData(validStationIds2ValueMap);

                    OmsExperimentalVariogram expVariogram = new OmsExperimentalVariogram();
                    expVariogram.inStationIds2CoordinateMap = validStationIds2CoordinateMap;
                    expVariogram.inStationIds2ValueMap = validStationIds2ValueMap;
                    expVariogram.pBins = pBins;
                    expVariogram.process();
                    HashMap<Integer, double[]> outExperimentalVariogram = expVariogram.outExperimentalVariogram;

                    OmsTheoreticalVariogram theoVariogram = new OmsTheoreticalVariogram();
                    theoVariogram.inExperimentalVariogramMap = outExperimentalVariogram;
                    theoVariogram.pTheoreticalVariogramType = pTheoreticalVariogramType;
                    theoVariogram.process();
                    HashMap<Integer, double[]> outTheoreticalVariogram = theoVariogram.outTheoreticalVariogram;
                    double outSill = theoVariogram.outSill;
                    double outRange = theoVariogram.outRange;
                    double outNugget = theoVariogram.outNugget;

                    // TODO after Kriging output data need to be converted back (see normalization
                    // above)
                    HashMap<Integer, double[]> krigingOutData = null;
                    // Does this contain just one id and one value? To be checked after Kriging interpolation.
                    inverseNormalizeData(store, krigingOutData);
                    

                } else {
                    throw new RuntimeException("Not implemented yet");
                }
                break;
            case NODATA:
            default:
                // do nothing, the target point will be ignored as it has no data
                break;
            }
        }

    }

    private void inverseNormalizeData( NormalizationStore store, HashMap<Integer, double[]> targetIds2ValueMap ) {
        Set<Integer> targetIdsSet = targetIds2ValueMap.keySet();
        List<Double> orderedInterpolatedValues = new ArrayList<Double>();
        for( Integer id : targetIdsSet ) {
            double[] sValues = targetIds2ValueMap.get(id);
            for( double sv : sValues ) {
                orderedInterpolatedValues.add(sv);
            }
        }
        Collections.sort(orderedInterpolatedValues);
        // calculate ranking
        double[] orderedInterpolatedValuesArray = new double[orderedInterpolatedValues.size()];
        for( int i = 0; i < orderedInterpolatedValuesArray.length; i++ ) {
            orderedInterpolatedValuesArray[i] = orderedInterpolatedValues.get(i);
        }
//        NaturalRanking nr = new NaturalRanking(TiesStrategy.AVERAGE);
//        double[] rank = nr.rank(orderedInterpolatedValuesArray);
//        List<Double> rankInterpolatedValues = new ArrayList<>();
//        for( int i = 0; i < rank.length; i++ ) {
//            rankInterpolatedValues.add(rank[i]);
//        }
//        List<Double> uniqueRankInterpolatedValues = rankInterpolatedValues.stream().distinct().collect(Collectors.toList());
        // z_star
        List<Double> uniqueOrderedInterpolatedValues = orderedInterpolatedValues.stream().distinct().collect(Collectors.toList());
        
        // funzione densita' di prob cumulata dei valori interpolati -> 
        // sono distribuiti come una Normale con media 0 e varianza 1
        List<Double> ppfNormalG = new ArrayList<Double>();
        NormalDistribution nd = new NormalDistribution(0, 1);
        for( int i = 0; i < uniqueOrderedInterpolatedValues.size(); i++ ) {
            double v = nd.cumulativeProbability(uniqueOrderedInterpolatedValues.get(i));
            ppfNormalG.add(v);
        }
        
        // la F(z) dei dati originali nota solo in forma discreta, il calcolo dell'inversa F-1(z)
        // deve essere fatto tramite interpolazione. La tecnica di interpolazione dipende dalla
        // posizione del valore della G rispetto alla F originale: 
        // lower tail: F                   -> z_star[G<min(F)] ------- G[G<min(F)]
        // parte centrale (interpolazione) -> z_star[(G>=min(F))*(G<=max(F))] ------- G[(G>=min(F))*(G<=max(F))]
        // upper tail                      -> z_star[G>max(F)] ------- G[G>max(F)]

        // Inserisco la F(z) manualmente per questo tentativo...
        List<Double> cdfOriginalValuesF = store.cdfWeibullValues;
        List<Double> originalMeasuredValuesZ = store.uniqueOrderedValues;
        
        Double maxCdfOriginalValuesF = Collections.max(cdfOriginalValuesF);
        Double minCdfOriginalValuesF = Collections.min(cdfOriginalValuesF);
        Double maxCdfOriginalMeasuredValues = Collections.max(originalMeasuredValuesZ);
        Double minCdfOriginalMeasuredValues = Collections.min(originalMeasuredValuesZ);
        
        // create tails for the original values CDF function -> F(z)
        List<Double> lowerTailInterpolatedValues = new ArrayList<Double>();
        List<Double> upperTailInterpolatedValues = new ArrayList<Double>();
        List<Double> middleTailInterpolatedValues = new ArrayList<Double>();
        double currentInterpolatedValue = Double.NaN;
        
        for (int j = 0; j < ppfNormalG.size()-1; j++) {
            currentInterpolatedValue = uniqueOrderedInterpolatedValues.get(j);
            if (ppfNormalG.get(j) < minCdfOriginalValuesF) {
                lowerTailInterpolatedValues.add(currentInterpolatedValue);
            } else if (ppfNormalG.get(j) > maxCdfOriginalValuesF) {
                upperTailInterpolatedValues.add(currentInterpolatedValue);
            } else {
                middleTailInterpolatedValues.add(currentInterpolatedValue);
            }
        }
        
        // create tails for the interpolated values CDF function -> G(z)
        List<Double> lowerTailPpfG = new ArrayList<Double>();
        List<Double> upperTailPpfG = new ArrayList<Double>();
        List<Double> middleTailPpfG = new ArrayList<Double>();
        double currentppfValueG = Double.NaN;
        
        for (int j = 0; j < ppfNormalG.size(); j++) {
            currentppfValueG = ppfNormalG.get(j);
            if (ppfNormalG.get(j) < minCdfOriginalValuesF) {
                lowerTailPpfG.add(currentppfValueG);
            } else if (ppfNormalG.get(j) > maxCdfOriginalValuesF) {
                upperTailPpfG.add(currentppfValueG);
            } else {
                middleTailPpfG.add(currentppfValueG);
            }
        }
        
        // Calcolo ora la F-1(z) partendo da G nelle tre fasce
        
        
        
        List<Double> finalInterpolatedValues = new ArrayList<>();
        
        // lower tail -> use the expression G(z) = F(z1) [(z-z_min)/ (z1-zmin)]^(omega>1)
        double omega = 5.0;
        double z_min = 0.0;
        for (int i = 0; i < lowerTailPpfG.size(); i++) {
            double value = lowerTailPpfG.get(i);
            double inverseInterpolatedLowerG = Math.pow((value/minCdfOriginalValuesF),(1/omega))*(minCdfOriginalMeasuredValues-z_min) + z_min;
            finalInterpolatedValues.add(inverseInterpolatedLowerG);
        }
        // middle tail -> interpolation
        LinearListInterpolator llInt = new LinearListInterpolator(cdfOriginalValuesF, originalMeasuredValuesZ);
        for (int i = 0; i < middleTailPpfG.size(); i++) {
            double value = middleTailPpfG.get(i);
            double inverseInterpolatedMiddleG = llInt.getInterpolated(value);
            finalInterpolatedValues.add(inverseInterpolatedMiddleG);
        }
        
        // upper tail -> use the expression z = (-llambda/(upper_tail_G-1))^(1/1.5)
        double omegaUp = 1.5;
        for (int i = 0; i < upperTailPpfG.size(); i++) {
            double value = upperTailPpfG.get(i);
            double lambda = Math.pow(maxCdfOriginalMeasuredValues,1.5) * (1-maxCdfOriginalValuesF);
            double inverseInterpolatedUpperG = Math.pow((-lambda / (value - 1)),(1/omegaUp));
            finalInterpolatedValues.add(inverseInterpolatedUpperG);
        }
        
        
        for( Entry<Integer, double[]> entry : targetIds2ValueMap.entrySet() ) {
            double[] value = entry.getValue();
            int indexOf = uniqueOrderedInterpolatedValues.indexOf(value[0]);
            Double invNomrValue = finalInterpolatedValues.get(indexOf);
            value[0] = invNomrValue;
        }
        
        
    }
    
    private NormalizationStore normalizeData( HashMap<Integer, double[]> validStationIds2ValueMap ) {
        NormalizationStore store = new NormalizationStore();
        
        Set<Integer> stationsIdsSet = validStationIds2ValueMap.keySet();
        List<Double> orderedValues = new ArrayList<Double>();
        for( Integer id : stationsIdsSet ) {
            double[] sValues = inStationIds2ValueMap.get(id);
            for( double sv : sValues ) {
                orderedValues.add(sv);
            }
        }
        Collections.sort(orderedValues);
        // calculate ranking
        double[] orderedValuesArray = new double[orderedValues.size()];
        for( int i = 0; i < orderedValuesArray.length; i++ ) {
            orderedValuesArray[i] = orderedValues.get(i);
        }
        NaturalRanking nr = new NaturalRanking(TiesStrategy.AVERAGE);
        double[] rank = nr.rank(orderedValuesArray);
        List<Double> rankValues = new ArrayList<>();
        for( int i = 0; i < rank.length; i++ ) {
            rankValues.add(rank[i]);
        }
        List<Double> uniqueRankValues = rankValues.stream().distinct().collect(Collectors.toList());
        store.uniqueOrderedValues = orderedValues.stream().distinct().collect(Collectors.toList());

        // funzione densita' di prob cumulata dei valori
        
        for( int i = 0; i < uniqueRankValues.size(); i++ ) {
            double r = uniqueRankValues.get(i);
            double v = r / (orderedValuesArray.length + 1);
            store.cdfWeibullValues.add(v);
        }

        // trasformazione da cumulata dei valori in cumulata della normale
        List<Double> ppfNormal = new ArrayList<Double>();
        NormalDistribution nd = new NormalDistribution(0, 1);
        for( int i = 0; i < store.cdfWeibullValues.size(); i++ ) {
            double v = nd.inverseCumulativeProbability(store.cdfWeibullValues.get(i));
            ppfNormal.add(v);
        }

        // sostituire dati con cdf
        for( int i = 0; i < ppfNormal.size(); i++ ) {
            for( Integer id : stationsIdsSet ) {
                double[] sValues = inStationIds2ValueMap.get(id);
                for( int j = 0; j < sValues.length; j++ ) {
                    Double sv = sValues[j];
                    int indexOf = store.uniqueOrderedValues.indexOf(sv);
                    if (indexOf == -1) {
                        String collect = store.uniqueOrderedValues.stream().map(d -> d.toString()).collect(Collectors.joining(","));
                        throw new IllegalArgumentException("Could not find " + sv + " inside list: " + collect);
                    }
                    double ppf = ppfNormal.get(indexOf);
                    sValues[j] = ppf;
                }
            }
        }
        
        return store;
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
    
    private static class NormalizationStore {
        private List<Double> uniqueOrderedValues;
        private List<Double> cdfWeibullValues = new ArrayList<Double>();
    }
    
}
