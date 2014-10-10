/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been entirely contributed by Marco Foi (www.mcfoi.it)
 * 
 * "CI-slam module" is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

public class MapCalculationFunctions {
    
    public enum MapOfCumulatedValue {
        RATIO,SLOPE_CUM, DIST, AB, CONT, SOIL_THICK, TH_TIME;
    }
    

    /**
     * Computes the map of the times required by each basing node to develop a perched water table
     * given the rainfall intensity (assumed uniform in space and time),
     * the map of initial soil moisture content V0 [L] and the map of soil moisture requires to develop the water table Vwt.
     * Relies on the method {@link calculateTimeForWaterTableDevelopmentTwt() }
     * The calculation is based on Equation (1) of the reference paper.
     * @param mapVwt GridCoverage2D
     * @param mapV0 GridCoverage2D
     * @param pRainfallIntensity [L/T]
     * @param pm The progress monitor to return process information.
     * @return
     */
    public static GridCoverage2D computeMapOfTimeForWaterTableDevelopmentTwt( GridCoverage2D mapVwt, GridCoverage2D mapV0,
            double pRainfallIntensity, IJGTProgressMonitor pm ) {

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(mapVwt);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        RandomIter mapVwtIter = RandomIterFactory.create(mapVwt.getRenderedImage(), null);
        RandomIter mapV0Iter = RandomIterFactory.create(mapV0.getRenderedImage(), null);

        WritableRaster mapTwtWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter mapTwtIter = RandomIterFactory.createWritable(mapTwtWR, null);

        double mTwt, pVwt, pV0 = Double.NaN;

        pm.beginTask("Start computing Twt map..", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {

                pVwt = mapVwtIter.getSampleDouble(c, r, 0);
                pV0 = mapV0Iter.getSampleDouble(c, r, 0);
                mTwt = ParameterCalculationFunctions.calculateTimeForWaterTableDevelopmentTwt(pVwt, pV0, pRainfallIntensity);

                mapTwtIter.setSample(c, r, 0, mTwt);

            }
            // pm.worked(1);
        }
        pm.done();

        GridCoverage2D mapTwt = CoverageUtilities.buildCoverage("twt", mapTwtWR, regionMap, mapVwt.getCoordinateReferenceSystem());

        return mapTwt;
    }

    /**
     * Returns a multi-banded GridCoverage2D containing a set of Psi maps at soil-bedrock interface.
     * Logic is from Equation (9) of reference paper. 
     * Each band in the coverage contains the Psi values after i+1 hours of rainfall, so that the band #3 represents
     * the status after 4 hours of rainfall.
     * The total number of bands depends on the max value in the passed Twt map,
     * so depends on the time required for having all cells developed their local perched water table due to vertical infiltration.
     * NOTE THAT any multi-banded map set is computed for a specific Return Time [years] and a simulated Rainfall Duration [hours] that,
     * thoug not passed to the method, is embedded in the calculation of Twt map and Rainfall Intensity I. 
     * @param Twt GridCoverage2D (depends on Return Time and Rainfall Duration)
     * @param psiInit GridCoverage2D (depends on basin)
     * @param I double (depends on Return Time and Rainfall Duration)
     * @param soil_thickness GridCoverage2D (depends on basin)
     * @param theta_s GridCoverage2D (depends on basin)
     * @param theta_r GridCoverage2D (depends on basin)
     * @param alfaVanGenuchten GridCoverage2D (depends on basin)
     * @param nVanGenuchten GridCoverage2D (depends on basin)
     * @param pm
     * @return
     */
    public static GridCoverage2D computeMapsOfPsiAtBedrockDuringVerticalInfiltration( GridCoverage2D Twt, GridCoverage2D psiInit,
            double I, GridCoverage2D soil_thickness, GridCoverage2D theta_s, GridCoverage2D theta_r, GridCoverage2D alfaVanGenuchten,
            GridCoverage2D nVanGenuchten, IJGTProgressMonitor pm ) {

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(soil_thickness);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        RandomIter mapPsiInitIter = RandomIterFactory.create(psiInit.getRenderedImage(), null);
        RandomIter mapSoilThicknessIter = RandomIterFactory.create(soil_thickness.getRenderedImage(), null);
        RandomIter mapTheta_SIter = RandomIterFactory.create(theta_s.getRenderedImage(), null);
        RandomIter mapTheta_RIter = RandomIterFactory.create(theta_r.getRenderedImage(), null);
        RandomIter mapAalfaVanGenuchtenIter = RandomIterFactory.create(alfaVanGenuchten.getRenderedImage(), null);
        RandomIter mapNVanGenuchtenIter = RandomIterFactory.create(nVanGenuchten.getRenderedImage(), null);

        double maxTwt = CoverageUtilities.getMinMaxMeanSdevCount(Twt, null)[1];
        int countOfToBeStoredPsiCoverages = (int) (Math.round(maxTwt)) + 1;

        BandedSampleModel sampleModel = new BandedSampleModel(DataBuffer.TYPE_DOUBLE, cols, rows, countOfToBeStoredPsiCoverages);

        int numBands = sampleModel.getNumBands();
        WritableRaster psiMultiHoursWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, sampleModel,
                JGTConstants.doubleNovalue);
        WritableRandomIter psiWRIter = RandomIterFactory.createWritable(psiMultiHoursWR, null);

        double pPreviousPsi_bb, pI, pSoil_thickness, pTheta_s, pTheta_r, pAlfaVanGenuchten, pNVanGenuchten;
        int dt = ParameterCalculationFunctions.DELTA_T_INTEGRATION_STEP_IN_SECONDS;

        pm.beginTask("Started computing Psi maps after each of the " + numBands
                + " hour/s of required rainfall simulation to exceed MAX Twt time...", rows);
        long start = System.nanoTime();
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                // Check we are in the basin by probing Soil Thickness
                pSoil_thickness = mapSoilThicknessIter.getSampleDouble(c, r, 0);

                if (!Double.isNaN(pSoil_thickness)) {

                    pPreviousPsi_bb = mapPsiInitIter.getSampleDouble(c, r, 0);
                    pI = I;
                    pTheta_s = mapTheta_SIter.getSampleDouble(c, r, 0);
                    pTheta_r = mapTheta_RIter.getSampleDouble(c, r, 0);
                    pAlfaVanGenuchten = mapAalfaVanGenuchtenIter.getSampleDouble(c, r, 0);
                    pNVanGenuchten = mapNVanGenuchtenIter.getSampleDouble(c, r, 0);

                    double[] pixel = new double[countOfToBeStoredPsiCoverages];
                    // Cycle in the total number of simulation hours (derived from the max Twt time of the basin for given Return Time and
                    // Rainfall Intensity
                    for( int j = 0; j < countOfToBeStoredPsiCoverages; j++ ) {
                        int i = 1;
                        // Loop until the sum of iteration steps over one cell reaches one hour (3600 sec) then write computed psi to raster
                        while( (i * dt) % ((3600 / dt) + 1) != 0 ) {

                            // int counter = ((i * ParameterCalculationFunctions.DELTA_T_INTEGRATION_STEP_IN_SECONDS) % 3600);

                            pPreviousPsi_bb = ParameterCalculationFunctions.calculatePsiAtBedrockDuringVerticalInfiltration(
                                    pPreviousPsi_bb, pI, pSoil_thickness, pTheta_s, pTheta_r, pAlfaVanGenuchten, pNVanGenuchten);
                            i++;
                        }

                        pixel[j] = pPreviousPsi_bb;
                    }
                    psiWRIter.setPixel(c, r, pixel);
                    // DEBUG CODE
                    /*
                    double[] outPixel = new double[4];
                    psiMultiHoursWR.getPixel(252,682,outPixel);
                    psiMultiHoursWR.getPixel(c,r,outPixel);
                    double[] bandValues = outPixel;
                    */
                }
            }
            pm.worked(1);
        }
        pm.message("Elapsed time: " + (System.nanoTime() - start) / 1000000000 + " sec.");
        pm.done();

        GridCoverage2D psiMapsAtIncreasingHours;

        psiMapsAtIncreasingHours = CoverageUtilities.buildCoverage("psimultihours", psiMultiHoursWR, regionMap,
                Twt.getCoordinateReferenceSystem());

        GridCoverage2D[] coverageArry = fromMultibandCoverageToCoverageArray(psiMapsAtIncreasingHours);
        for( int c = 0; c < coverageArry.length; c++ ) {
            pm.message("Stats for Psi map at end of " + (c + 1) + " hour rainfall " + getCoverageStatsAsString(coverageArry[c]));
        }

        // PROBE CODE for Coverages as MULTIBAND GridCoverage2D
        // final Point2D.Double point = new Point2D.Double();
        // point.x = 623630.1509;
        // point.y = 5137453.8354;
        // double[] bufferCov = null;

        // bufferCov = psiMapsAtIncreasingHours.evaluate(point, bufferCov);

        return psiMapsAtIncreasingHours;

    }

    /**
     * Computes from the input maps a set of maps containing variables cumulated along FLOW path
     * The returned maps are: RATIO,SLOPE_CUM, DIST, AB, CONT, SOIL_THICK, TH_TIME
     * @param inPit
     * @param inFlowWithBorderAndOutlet
     * @param inKsat
     * @param inSoilThickness
     * @param inTheta_s
     * @param inAb
     * @param inTwt
     * @param pm
     * @return GridCoverage2D[] containing following sequence of maps: {RATIO,SLOPE_CUM, DIST, AB, CONT, SOIL_THICK, TH_TIME}
     */
    public static HashMap<MapOfCumulatedValue, GridCoverage2D> calculateMapsOfCumulatedParameters( GridCoverage2D inPit, GridCoverage2D inFlowWithBorderAndOutlet,
            GridCoverage2D inKsat, GridCoverage2D inSoilThickness, GridCoverage2D inTheta_s, GridCoverage2D inAb,
            GridCoverage2D inTwt, IJGTProgressMonitor pm ) {

        double delta_st = 0.0;
        double delta_l = 0.0;
        double delta_ll = 0.0;
        double delta_s = 0.0;
        double delta_Ab = 0.0;
        double delta_tt = 0.0;
        int r = 0;
        int c = 0;
        int rnew = 0;
        int cnew = 0;
        double d_st = 0.0;
        double d_l = 0.0;
        double d_ll = 0.0;
        double d_s = 0.0;
        double d_tt = 0.0;
        double cont = 0.0;

        double d_Ab = 0.0;

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        // Prepare objects for iterating over the maps
        RandomIter mapPitIter = RandomIterFactory.create(inPit.getRenderedImage(), null);
        RandomIter mapFlowIter = RandomIterFactory.create(inFlowWithBorderAndOutlet.getRenderedImage(), null);
        RandomIter mapKsatIter = RandomIterFactory.create(inKsat.getRenderedImage(), null);
        RandomIter mapSoilThicknessIter = RandomIterFactory.create(inSoilThickness.getRenderedImage(), null);
        RandomIter mapTheta_SIter = RandomIterFactory.create(inTheta_s.getRenderedImage(), null);
        RandomIter mapAbIter = RandomIterFactory.create(inAb.getRenderedImage(), null);
        RandomIter mapTwtIter = RandomIterFactory.create(inTwt.getRenderedImage(), null);
        
        WritableRaster DIST_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        WritableRaster SOIL_THICK_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        WritableRaster SLOPE_CUM_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        WritableRaster TH_TIME_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        WritableRaster RATIO_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        WritableRaster CONT_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        WritableRaster AB_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        
        WritableRandomIter DIST_Iter = RandomIterFactory.createWritable(DIST_WR, null);
        WritableRandomIter SOIL_THICK_Iter = RandomIterFactory.createWritable(SOIL_THICK_WR, null);
        WritableRandomIter SLOPE_CUM_Iter = RandomIterFactory.createWritable(SLOPE_CUM_WR, null);
        WritableRandomIter TH_TIME_Iter = RandomIterFactory.createWritable(TH_TIME_WR, null);
        WritableRandomIter RATIO_Iter = RandomIterFactory.createWritable(RATIO_WR, null);
        WritableRandomIter CONT_Iter = RandomIterFactory.createWritable(CONT_WR, null);
        WritableRandomIter AB_Iter = RandomIterFactory.createWritable(AB_WR, null);

        for( int rloop = 0; rloop < rows; rloop++ ) {
            for( int cloop = 0; cloop < cols; cloop++ ) {

                delta_st = 0.0;
                delta_l = 0.0;
                delta_ll = 0.0;
                delta_s = 0.0;
                delta_Ab = 0.0;
                delta_tt = 0.0;
                r = rloop;
                c = cloop;
                rnew = 0;
                cnew = 0;
                d_st = 0.0;
                d_l = 0.0;
                d_ll = 0.0;
                d_s = 0.0;
                d_tt = 0.0;
                cont = 0.0;
                
                FlowNode flowNode = new FlowNode(mapFlowIter, cols, rows, cloop, rloop);

                // flowNode.flow returns a double - not an int!!
                while( !Double.isNaN(flowNode.flow) && flowNode.flow != 10.0 ) {

                    d_st = 0.0;
                    d_l = 0.0;
                    d_s = 0.0;
                    d_tt = 0.0;
                    d_Ab = 0.0;

                    c = flowNode.col; // Useless at first loop but leads processing downstream from second loop hence
                    r = flowNode.row; // Useless at first loop but leads processing downstream from second loop hence
                    
                    FlowNode nextFlowNode = flowNode.goDownstream();

                    cnew = nextFlowNode.col;
                    rnew = nextFlowNode.row;


                    // Actual distance between starting and destination cell is returned
                    d_ll = sqrt(pow((cnew - c) * xRes, 2.0) + pow((rnew - r) * yRes, 2.0)); //OK 4.0
                    
                    double d_quota = mapPitIter.getSampleDouble(c, r, 0) - mapPitIter.getSampleDouble(cnew, rnew, 0);//OK 3.6293945

                    // Incrementally store the path length along FLOW direction
                    delta_ll = delta_ll + d_ll;//OK 4.0 8.0
                    
                    d_l = 
                    (
                            (d_ll)*
                            (( mapTheta_SIter.getSampleDouble(c, r, 0) + mapTheta_SIter.getSampleDouble(cnew, rnew, 0) )/2)//OK 0.23
                    )/
                    (
                            // quello che segue e' la K saturo nella direzione del flusso (Ksat e' in origine espressa orizzontalmente)
                            Math.cos(Math.atan(Math.max(0.01,(d_quota))/(d_ll)))* //OK 0.740582
                            Math.sin(Math.atan(Math.max(0.01,(d_quota))/(d_ll)))* //OK 0.671966
                            ((mapKsatIter.getSampleDouble(c, r, 0)+mapKsatIter.getSampleDouble(cnew, rnew, 0))/2) //OK 1.0E-5
                    ); //OK 184870.4  184003.5
                    
                    // Incrementally store the value found (d_l) along FLOW direction
                    delta_l = delta_l + d_l; //OK 184870.4 368873.9

                    // slope in path direction
                    d_s = Math.max(0.01,d_quota)/(d_ll); //OK 0.9073486 1.006226
                   
                    //Incrementally store the value found (d_s) along FLOW direction
                    delta_s = delta_s + d_s; //OK 0.9073486 1.913574
                            
                    // Ab = AbFLOW
                    d_Ab = mapAbIter.getSampleDouble(c, r, 0); //OK 2.105263 5.912811
                    // Incrementally store the value found (d_Ab) along FLOW direction
                    delta_Ab = delta_Ab + d_Ab; //OK 2.105263 8.018075

                    d_st = mapSoilThicknessIter.getSampleDouble(c, r, 0); //OK 0.2347537
                    // Incrementally store the value found (d_st) along FLOW direction
                    delta_st = delta_st + d_st; //0.2347537 0.3854619
                    // Remind that 'th_time' is computed for a specific return time (here 200 years as on line 294)
                    d_tt = Math.max(0,mapTwtIter.getSampleDouble(c, r, 0)); //OK 1.518616 1.129852
                    // Incrementally store the value found (d_tt) along FLOW direction
                    delta_tt = delta_tt + d_tt; //OK 1.518616 2.648468
                    
                    cont = cont +1;
                    
                    // RATIO is initially a matrix of 0 values
                    // If a cell with RATIO >= value_to_be_assigned is met, FLOW path processing is halted and next cell is inspected.
                    double nextRatio = RATIO_Iter.getSampleDouble(cnew, rnew, 0);
                    if (nextRatio < delta_l) {
                        // Assign to cells along FLOW path the cumulated value of preceeding ones (each variable has its own matrix)
                        // NOTE THAT PATH STARTING CELLS (k,l) get no value: this is fixed for each parameter matrix on line 633 and following
                        // Cumulated 'd_l' parameter
                        RATIO_Iter.setSample(cnew, rnew, 0, delta_l);
                        // Cumulated SLOPE along path
                        SLOPE_CUM_Iter.setSample(cnew, rnew, 0, delta_s);
                        // Total DISTANCE along path
                        DIST_Iter.setSample(cnew, rnew, 0, delta_ll);
                        // Cumulated AbSLOPE (directly from file on line 26)
                        AB_Iter.setSample(cnew, rnew, 0, delta_Ab);
                        // COUNTER of path steps
                        CONT_Iter.setSample(cnew, rnew, 0, cont);
                        // Cumulated SOIL THICKNESS
                        SOIL_THICK_Iter.setSample(cnew, rnew, 0, delta_st);
                        // Cumulated time to water table generation t_wt (Equation (1))
                        // NOTE that delta_tt derives from th_time that is computed for a specific Return Time (200 years as on line 294)
                        // so also TH_TIME is related to the same Return Time
                        TH_TIME_Iter.setSample(cnew, rnew, 0, delta_tt);
                    }
                    else {break;}

                    // Move on to the new cell along the D8 FLOW direction
                    //k=knew;
                    //l=lnew;
                    flowNode = flowNode.goDownstream();
                }
            } // Next Column
        }// Next Row

        GridCoverage2D RATIO = CoverageUtilities.buildCoverage("RATIO", RATIO_WR, regionMap, inPit.getCoordinateReferenceSystem());
        GridCoverage2D SLOPE_CUM = CoverageUtilities.buildCoverage("SLOPE_CUM", SLOPE_CUM_WR, regionMap, inPit.getCoordinateReferenceSystem());
        GridCoverage2D DIST = CoverageUtilities.buildCoverage("DIST", DIST_WR, regionMap, inPit.getCoordinateReferenceSystem());
        GridCoverage2D AB = CoverageUtilities.buildCoverage("AB", AB_WR, regionMap, inPit.getCoordinateReferenceSystem());
        GridCoverage2D CONT = CoverageUtilities.buildCoverage("CONT", CONT_WR, regionMap, inPit.getCoordinateReferenceSystem());        
        GridCoverage2D SOIL_THICK = CoverageUtilities.buildCoverage("SOIL_THICK", SOIL_THICK_WR, regionMap, inPit.getCoordinateReferenceSystem());        
        GridCoverage2D TH_TIME = CoverageUtilities.buildCoverage("TH_TIME", TH_TIME_WR, regionMap, inPit.getCoordinateReferenceSystem());
        
        HashMap<MapCalculationFunctions.MapOfCumulatedValue, GridCoverage2D> maps = new HashMap<MapOfCumulatedValue, GridCoverage2D>(cols*rows*8*8, 1);
        
        maps.put(MapOfCumulatedValue.RATIO, RATIO);
        maps.put(MapOfCumulatedValue.SLOPE_CUM, SLOPE_CUM);
        maps.put(MapOfCumulatedValue.DIST, DIST);
        maps.put(MapOfCumulatedValue.AB, AB);
        maps.put(MapOfCumulatedValue.CONT, CONT);
        maps.put(MapOfCumulatedValue.SOIL_THICK, SOIL_THICK);
        maps.put(MapOfCumulatedValue.TH_TIME, TH_TIME);
        
        return maps;
    }
    
    /**
     * Takes two GridCoverage2D coverage maps, divides the values of the first with the second and returns the resulting map.
     * It is used to compute the averages of the cumulated parameters computed with calculateMapsOfCumulatedParameters
     * by passing it one of the maps as first argument and the COUNT map as second. 
     * @param GridCoverage2D toBeAveragedMap
     * @param GridCoverage2D countsMapToUseToAverage
     * @return GridCoverage2D the map containing the ratio between first and second
     */
    public static GridCoverage2D computeMapOfAverageFromCumulated(GridCoverage2D toBeAveragedMap, GridCoverage2D countsMapToUseToAverage){
        
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(toBeAveragedMap);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        RandomIter toBeAveragedMapIter = RandomIterFactory.create(toBeAveragedMap.getRenderedImage(), null);
        RandomIter dividerMapIter = RandomIterFactory.create(countsMapToUseToAverage.getRenderedImage(), null);
        
        WritableRaster averagedMapWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter averagedMapIter = RandomIterFactory.createWritable(averagedMapWR, null);
        
        double toBeAveragedValue;
        double divider;
        double averagedValue;
        
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                toBeAveragedValue = toBeAveragedMapIter.getSampleDouble(c, r, 0);
                divider = dividerMapIter.getSampleDouble(c, r, 0);
                if(!Double.isNaN(toBeAveragedValue) && !Double.isNaN(divider) && !Double.isInfinite(divider)){
                    averagedValue = toBeAveragedValue/divider;
                    averagedMapIter.setSample(c, r, 0, averagedValue);
                }
            }
        }
        
        String newName = "avg_"+toBeAveragedMap.getName();
        
        GridCoverage2D outAveragedCoverage = CoverageUtilities.buildCoverage(newName, averagedMapWR, regionMap,
                toBeAveragedMap.getCoordinateReferenceSystem());
        
        return outAveragedCoverage;
    }
    
    
    
    /**
     * Computes a multi-band GridCoverage2D containing one map for each hour of simulated rainfall.
     * Each map contains the value of a dinamic linear Topographic Index computed using formula from
     * {@link ParameterCalculationFunctions.calculateDinamicLinearTopographicIndexValue}
     * @param avg_TH_TIME_BorderFixed Map computed by calculateMapsOfCumulatedParameters() then passed in computeAverageFromCumulated() and finally in fixBorders_AvgThTime() 
     * @param singleTwtMap the map of the time-to-water-table creation computed for the provided D value
     * @param pReturnTime the return time for which the simulation is run [years]
     * @param D Rainfall simulated duration
     * @param mapRATIO Map computed by calculateMapsOfCumulatedParameters() and then passed in fixBorders_Ratio()
     * @param inAb Ab map computed for the basin
     * @param inSlopeFixZeroValuesAndBorder Slope map computed for the basin and fixed with slopeMapFixZeroValuesAndBorder() in MapPreprocessingUtilities.class
     * @return
     */
    public static GridCoverage2D computeMapOfDynamicLinearTopographicIndex(GridCoverage2D avg_TH_TIME_BorderFixed, GridCoverage2D singleTwtMap, int pReturnTime, int D, GridCoverage2D mapRATIO, GridCoverage2D inAb, GridCoverage2D inSlopeFixZeroValuesAndBorder, IJGTProgressMonitor pm){
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(avg_TH_TIME_BorderFixed);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();               

        // Prepare objects for iterating over the maps
        BandedSampleModel bandedSampleModel = new BandedSampleModel(DataBuffer.TYPE_DOUBLE, cols, rows, D);
        WritableRaster IT_din_lin_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, bandedSampleModel, JGTConstants.doubleNovalue);
        
        RandomIter avg_TH_TIME_BorderFixed_Iter = RandomIterFactory.create(avg_TH_TIME_BorderFixed.getRenderedImage(), null);
        RandomIter singleTwtMap_Iter = RandomIterFactory.create(singleTwtMap.getRenderedImage(), null);
        RandomIter mapRATIO_Iter = RandomIterFactory.create(mapRATIO.getRenderedImage(), null);
        RandomIter inAb_Iter = RandomIterFactory.create(inAb.getRenderedImage(), null);
        RandomIter inSlopeFixZeroValuesAndBorder_Iter = RandomIterFactory.create(inSlopeFixZeroValuesAndBorder.getRenderedImage(), null);
        
        double tempo_soglia;
        double th_time;
        
        pm.beginTask("Starting computing TopoIndex_din_lin ...", rows*D);
        for( int t = 1; t <= D; t++ ) {
            
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    
                    tempo_soglia = avg_TH_TIME_BorderFixed_Iter.getSampleDouble(c, r, 0);
                    th_time = singleTwtMap_Iter.getSampleDouble(c, r, 0); 
                    
                    if( !Double.isNaN(th_time) && !Double.isNaN(th_time) ) {                        
                        
                        if( t <= Math.max(tempo_soglia, th_time) ){
                            
                            IT_din_lin_WR.setSample(c, r, (t-1), JGTConstants.doubleNovalue);
                            
                        } else if (t > Math.max(tempo_soglia, th_time)){
                            
                            double Ab = inAb_Iter.getSampleDouble(c, r, 0);
                            double ratio = mapRATIO_Iter.getSampleDouble(c, r, 0);
                            double slope = inSlopeFixZeroValuesAndBorder_Iter.getSampleDouble(c, r, 0);
                            
                            double IT_din_lin_Value = ParameterCalculationFunctions.calculateDinamicLinearTopographicIndexValue(t, Ab, tempo_soglia, th_time, ratio, slope);
                            // t starts from 1 (h) but bands are 0-indexed
                            IT_din_lin_WR.setSample(c, r, (t-1), IT_din_lin_Value);
                            
                        }
                        
                    }
                }
                pm.worked(1);
            }
        }
        pm.done();
        
        return CoverageUtilities.buildCoverage("Topographic_Index_dinamic_linear_" + pReturnTime + "y_", IT_din_lin_WR, regionMap, avg_TH_TIME_BorderFixed.getCoordinateReferenceSystem());
        
    }
    
    /**
     * Calculates the pressure at bedrock in the pore pressure positive zones (psi > 0) according to
     * Equation (12) in reference paper.
     * The returned multi-band Coverage has a number of bands equal to D: the number of simulate rainfall hours.
     * Note that IT_din_lin is used as  equivalent to  Area/(perimeter*sin(beta))
     * @param pReturnTime The return time for which simulation is running 
     * @param D The rainfall duration for which simulation is running 
     * @param inSoilThickness Map of soil thickness
     * @param I Rainfall intensity computed for pReturnTime and D
     * @param inKsat
     * @param IT_din_lin GricDoverage2D The Dynamic Linear Topographic Index multibadn map containing the computed TI for each of the D rainfall hours
     * @param pm IJGTProgressMonitor
     * @return
     */
    public static GridCoverage2D computeMapOfPsiAtBedrockInPositivePressureZones(int pReturnTime, int D, GridCoverage2D inSoilThickness, double I,
            GridCoverage2D inKsat, GridCoverage2D IT_din_lin, IJGTProgressMonitor pm ) {
        

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inSoilThickness);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();               

        // Prepare objects for iterating over the maps
        BandedSampleModel bandedSampleModel = new BandedSampleModel(DataBuffer.TYPE_DOUBLE, cols, rows, D);
        WritableRaster psi_b_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, bandedSampleModel, JGTConstants.doubleNovalue);
        
        RandomIter inSoilThickness_Iter = RandomIterFactory.create(inSoilThickness.getRenderedImage(), null);
        RandomIter inKsat_Iter = RandomIterFactory.create(inKsat.getRenderedImage(), null);
        RandomIter IT_din_lin_Iter = RandomIterFactory.create(IT_din_lin.getRenderedImage(), null);
        
        double IT_din_lin_Val;
        
        pm.beginTask("Starting computing Psi at bedrock in positive_pressure_zones (t < T_tw but t > T_wt_up)...", rows*D);
        for( int t = 1; t <= D; t++ ) {
            
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    
                    IT_din_lin_Val = IT_din_lin_Iter.getSampleDouble(c, r, (t-1));
                    
                    if( !Double.isNaN(IT_din_lin_Val) ) {                        
                                                                         
                        double soil_thickness = inSoilThickness_Iter.getSampleDouble(c, r, 0);
                        double K = inKsat_Iter.getSampleDouble(c, r, 0);
                        
                        double psi_b = ParameterCalculationFunctions.calculatePsiAtBedrockInPositivePressureZonesVAlue(soil_thickness, I, K, IT_din_lin_Val, pm );
                        // t starts from 1 (h) but bands are 0-indexed
                        psi_b_WR.setSample(c, r, (t-1), psi_b);
                         
                                               
                    }
                }
                pm.worked(1);
            }
        }
        pm.done();
        
        return CoverageUtilities.buildCoverage("Psi_at_Bedrock_in_positive_pressure_zones_" + pReturnTime + "y_", psi_b_WR, regionMap, inSoilThickness.getCoordinateReferenceSystem());
        
    }
    
    /**
     * Compute maps of combined Pressure at soil-bedrock interface (Psi)      
     * This matrix is the combination of the two psi matrices computed for different conditions
     *
     *  = mapPsiMultiHours_Eq9 (Eq. 9) for t < t_wt (during infiltration)
     *  = mapPsiMultiHours_Eq12  (Eq.12) for t < t_tw but t > t_wt_up (during infiltration but with active lateral flow from catchment)
     *  ( D = simulation duration )
     * @param pReturnTime
     * @param mapPsiMultiHours_Eq9
     * @param mapPsiMultiHours_Eq12
     * @param inSoilThickness Map of soil thickness
     * @param pm
     * @return
     */
    public static GridCoverage2D computeMapOfPsiCombined_Eq9_Eq12(int pReturnTime, GridCoverage2D mapPsiMultiHours_Eq9, GridCoverage2D mapPsiMultiHours_Eq12, GridCoverage2D inSoilThickness, IJGTProgressMonitor pm) {
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inSoilThickness);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        
        // Get the number of bands on which to run the computation
        // mapPsiMultiHours_Eq9 has a number of bands equal to the hours needed to let all cells in basin to develop a perched water table ~ Math.ceil(MAX Twt)
        // mapPsiMultiHours_Eq12 has ALWAYS a number of bands equal to the number of hours of simulated rainfall D
        int minNumBands = Math.min(mapPsiMultiHours_Eq9.getNumSampleDimensions(), mapPsiMultiHours_Eq12.getNumSampleDimensions());
        int D = mapPsiMultiHours_Eq12.getNumSampleDimensions();

        // Prepare objects for iterating over the maps
        BandedSampleModel bandedSampleModel = new BandedSampleModel(DataBuffer.TYPE_DOUBLE, cols, rows, D);
        WritableRaster mapsPsiComposedMultiBandedWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, bandedSampleModel, JGTConstants.doubleNovalue);
        
        RandomIter inSoilThickness_Iter = RandomIterFactory.create(inSoilThickness.getRenderedImage(), null);
        RandomIter mapPsiMultiHours_Eq9_Iter = RandomIterFactory.create(mapPsiMultiHours_Eq9.getRenderedImage(), null);
        RandomIter mapPsiMultiHours_Eq12_Iter = RandomIterFactory.create(mapPsiMultiHours_Eq12.getRenderedImage(), null);
        
        if(pm!=null){pm.beginTask("Starting computing combined Psi at bedrock...", rows*minNumBands);}
        double psi_bEq9;
        double psi_bEq12;
        // First cycle through the bands for which we have changing Psi due to v. infiltration while no lateral flow
        if(pm!=null){pm.message("First calculating Composed Psi combinig Psi from Eq 9 and Eq 12 of reference paper.. ");}
        for( int t = 1; t <= minNumBands; t++ ) {            
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    
                    psi_bEq9 = mapPsiMultiHours_Eq9_Iter.getSampleDouble(c, r, (t-1));
                    psi_bEq12 = mapPsiMultiHours_Eq12_Iter.getSampleDouble(c, r, (t-1));
                    
                    if (psi_bEq12 < 0 && !Double.isNaN(psi_bEq12)){
                        mapsPsiComposedMultiBandedWR.setSample(c, r, (t-1), psi_bEq12);
                    }
                    if (psi_bEq9 > 0 && !Double.isNaN(psi_bEq9)){
                        mapsPsiComposedMultiBandedWR.setSample(c, r, (t-1), psi_bEq9);
                    }                    
                }
            }
        }
              
        // Now cycle through bands whose rainfall duration exceeds max Twt in basing
        if(pm!=null){pm.message("Now setting Composed Psi to Psi-during-lateral-flow for simulation hours exceeding T_wt_up.. ");}
        for( int t = minNumBands+1; t <= D; t++ ) {            
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    
                    psi_bEq12 = mapPsiMultiHours_Eq12_Iter.getSampleDouble(c, r, (t-1));
                    
                    mapsPsiComposedMultiBandedWR.setSample(c, r, (t-1), psi_bEq12);

                }
            }
        }
            
        if(pm!=null){pm.message("Now setting Composed Psi to zero where NaN.. ");}
        double soil_thickness, psi_b_Composed;
        for( int t = 1; t <= D; t++ ) {            
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    
                    psi_b_Composed = mapsPsiComposedMultiBandedWR.getSampleDouble(c, r, (t-1));
                    soil_thickness = inSoilThickness_Iter.getSampleDouble(c, r, 0);
                    
                    if(soil_thickness > 0.0 && Double.isNaN(psi_b_Composed)){
                        mapsPsiComposedMultiBandedWR.setSample(c, r, (t-1), 0.0);
                    }
                }
            }
        }
        
        return CoverageUtilities.buildCoverage("Psi_at_Bedrock_Combined_" + pReturnTime + "y_", mapsPsiComposedMultiBandedWR, regionMap, inSoilThickness.getCoordinateReferenceSystem());

    }
    
    
    /**
     * #### calcolo FS infinite slope model #######
     * Initial SAFETY FACTOR based on V : initial soil moisture content.
     * Equation (14a) & (14b)
     * psi_i = the provided initial psi map out of a constant replicated all over the basin or out of some algebra
     * 
     * @param inPsi_Initial GridCoverage2D
     * @param inSoil_Thickness GridCoverage2D
     * @param inPhi GridCoverage2D
     * @param inGammaSoil GridCoverage2D
     * @param inCohesion GridCoverage2D
     * @param inAlfaVanGen GridCoverage2D
     * @param inNVanGen GridCoverage2D
     * @param inSlopeFixedBorders GridCoverage2D
     * @param pm
     * @return GridCoverage2D Safety_Factor_initial
     */
    public static GridCoverage2D computeSafetyFactor_Initial(GridCoverage2D inPsi_Initial, GridCoverage2D inSoil_Thickness, GridCoverage2D inPhi, GridCoverage2D inGammaSoil, GridCoverage2D inCohesion, GridCoverage2D inAlfaVanGen, GridCoverage2D inNVanGen, GridCoverage2D inSlopeFixedBorders, IJGTProgressMonitor pm) {
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPhi);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        
        RandomIter inPsi_Initial_Iter = RandomIterFactory.create(inPsi_Initial.getRenderedImage(), null);
        RandomIter inSoil_Thickness_Iter = RandomIterFactory.create(inSoil_Thickness.getRenderedImage(), null);
        RandomIter inPhi_Iter = RandomIterFactory.create(inPhi.getRenderedImage(), null);
        RandomIter inGammaSoil_Iter = RandomIterFactory.create(inGammaSoil.getRenderedImage(), null);
        RandomIter inCohesion_Iter = RandomIterFactory.create(inCohesion.getRenderedImage(), null);
        RandomIter inAlfaVanGen_Iter = RandomIterFactory.create(inAlfaVanGen.getRenderedImage(), null);
        RandomIter inNVAnGen_Iter = RandomIterFactory.create(inNVanGen.getRenderedImage(), null);
        RandomIter inSlopeFixedBorders_Iter = RandomIterFactory.create(inSlopeFixedBorders.getRenderedImage(), null);
        
        WritableRaster mapSafetyFactor_Initial_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        
        double psi_i;
        double soil_thick;
        double tan_phi;
        double gamma;
        double gamma_w = 1;
        double cohesion;
        double alfaVanGen;
        double nVanGen;
        double angle;
        double tg;
        double ctg;
        double pSF_Initial;
        
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                
                psi_i = inPsi_Initial_Iter.getSampleDouble(c, r, 0);
                soil_thick = inSoil_Thickness_Iter.getSampleDouble(c, r, 0);
                tan_phi = Math.tan(inPhi_Iter.getSampleDouble(c, r, 0)/180.0*Math.PI);
                gamma = inGammaSoil_Iter.getSampleDouble(c, r, 0)/10.0;
                cohesion = inCohesion_Iter.getSampleDouble(c, r, 0);
                alfaVanGen = inAlfaVanGen_Iter.getSampleDouble(c, r, 0);
                nVanGen = inNVAnGen_Iter.getSampleDouble(c, r, 0);
                angle = Math.atan(inSlopeFixedBorders_Iter.getSampleDouble(c, r, 0));
                tg = Math.tan(angle);
                ctg = 1.0/tg;
                
                if(psi_i <= 0){
                    pSF_Initial =
                            (tan_phi/tg)+
                            (gamma_w/gamma)*(psi_i/soil_thick)*(tg+ctg)*tan_phi+
                            (2*cohesion)/
                            ((gamma*10)*soil_thick*Math.sin(2*angle));

                    mapSafetyFactor_Initial_WR.setSample(c, r, 0, pSF_Initial);
                }
                
                if(psi_i > 0){                
                    pSF_Initial = 
                        (tan_phi/tg)+
                        (Math.pow( (1+ Math.pow((alfaVanGen*psi_i),nVanGen)),(-1-1/nVanGen) ) )*
                        (gamma_w/gamma)*(psi_i/soil_thick)*(tg+ctg)*tan_phi+
                        (2*cohesion)/
                        ((gamma*10)*soil_thick*Math.sin(2*angle));
                    
                    mapSafetyFactor_Initial_WR.setSample(c, r, 0, pSF_Initial);
                }
            }
        }
       
        return CoverageUtilities.buildCoverage("Safety_Factor_initial", mapSafetyFactor_Initial_WR, regionMap, inSoil_Thickness.getCoordinateReferenceSystem());
        
    }
    
    /**
     * #### Calulate Saety Factor infinite slope model #######
     * 2. SF for t=1...D'
     * Equation (14a) & (14b)
     * but using combined Psi from Eq.(9) and Eq.(12) instead of initial Psi map
     * 
     * @param pReturnTime int
     * @param pRainfallDuration int
     * @param inPsi_Initial GridCoverage2D
     * @param inSoil_Thickness GridCoverage2D
     * @param inPhi GridCoverage2D
     * @param inGammaSoil GridCoverage2Di
     * @param inCohesion GridCoverage2D
     * @param inAlfaVanGen GridCoverage2D
     * @param inNVanGen GridCoverage2D
     * @param inSlopeFixedBorders GridCoverage2D
     * @param pm
     * @return GridCoverage2D Safety_Factor_initial
     */
    public static GridCoverage2D computeSafetyFactor_InfiniteSlope_with_SaturatedZones_SingleBand(int pReturnTime, int pRainfallDuration, GridCoverage2D inPsi_Combined, GridCoverage2D inSoil_Thickness, GridCoverage2D inPhi, GridCoverage2D inGammaSoil, GridCoverage2D inCohesion, GridCoverage2D inAlfaVanGen, GridCoverage2D inNVanGen, GridCoverage2D inSlopeFixedBorders, IJGTProgressMonitor pm) {
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPhi);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        
        RandomIter inPsi_Combined_Iter = RandomIterFactory.create(inPsi_Combined.getRenderedImage(), null);
        RandomIter inSoil_Thickness_Iter = RandomIterFactory.create(inSoil_Thickness.getRenderedImage(), null);
        RandomIter inPhi_Iter = RandomIterFactory.create(inPhi.getRenderedImage(), null);
        RandomIter inGammaSoil_Iter = RandomIterFactory.create(inGammaSoil.getRenderedImage(), null);
        RandomIter inCohesion_Iter = RandomIterFactory.create(inCohesion.getRenderedImage(), null);
        RandomIter inAlfaVanGen_Iter = RandomIterFactory.create(inAlfaVanGen.getRenderedImage(), null);
        RandomIter inNVAnGen_Iter = RandomIterFactory.create(inNVanGen.getRenderedImage(), null);
        RandomIter inSlopeFixedBorders_Iter = RandomIterFactory.create(inSlopeFixedBorders.getRenderedImage(), null);
        
        // Prepare objects for iterating over the maps
        BandedSampleModel bandedSampleModel = new BandedSampleModel(DataBuffer.TYPE_DOUBLE, cols, rows, pRainfallDuration);
        
        WritableRaster mapSafetyFactor_Hydologic_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        
        double psi_combined;
        double soil_thick;
        double tan_phi;
        double gamma;
        double gamma_w = 1;
        double cohesion;
        double alfaVanGen;
        double nVanGen;
        double angle;
        double tg;
        double ctg;
        double pSF_Hydologic;
        
        //for( int t = 1; t <= pRainfallDuration; t++ ) {
            for( int c = 0; c < cols; c++ ) {
                for( int r = 0; r < rows; r++ ) {
                    
                    psi_combined = inPsi_Combined_Iter.getSampleDouble(c, r, (pRainfallDuration-1));
                    
                    soil_thick = inSoil_Thickness_Iter.getSampleDouble(c, r, 0);
                    tan_phi = Math.tan(inPhi_Iter.getSampleDouble(c, r, 0)/180.0*Math.PI);
                    gamma = inGammaSoil_Iter.getSampleDouble(c, r, 0)/10.0;
                    cohesion = inCohesion_Iter.getSampleDouble(c, r, 0);
                    alfaVanGen = inAlfaVanGen_Iter.getSampleDouble(c, r, 0);
                    nVanGen = inNVAnGen_Iter.getSampleDouble(c, r, 0);
                    angle = Math.atan(inSlopeFixedBorders_Iter.getSampleDouble(c, r, 0));
                    tg = Math.tan(angle);
                    ctg = 1.0/tg;
                    
                    if(psi_combined <= 0.0){
                                            
                        pSF_Hydologic =
                                (tan_phi/tg)+
                                (gamma_w/gamma)*(psi_combined/soil_thick)*(tg+ctg)*tan_phi+
                                (2*cohesion)/
                                ((gamma*10)*soil_thick*Math.sin(2*angle));
                        
                        mapSafetyFactor_Hydologic_WR.setSample(c, r, 0, pSF_Hydologic);
                    }
                    
                    if(psi_combined > 0.0){
                    
                        pSF_Hydologic = 
                                (tan_phi/tg)+
                                ( Math.pow( (1+ Math.pow((alfaVanGen*psi_combined),nVanGen)),(-1-1/nVanGen) ) )*
                                (gamma_w/gamma)*(psi_combined/soil_thick)*(tg+ctg)*tan_phi+
                                (2*cohesion)/
                                ((gamma*10)*soil_thick*Math.sin(2*angle));
                        
                        mapSafetyFactor_Hydologic_WR.setSample(c, r, 0, pSF_Hydologic);
                    }
 
                }
            }
        //}
        return CoverageUtilities.buildCoverage("Safety_Factor_Hydrologic_" + pReturnTime + "y_" + pRainfallDuration + "h", mapSafetyFactor_Hydologic_WR, regionMap, inSoil_Thickness.getCoordinateReferenceSystem());
        
    }
    
    /**
     * #### Calulate Saety Factor infinite slope model #######
     * 2. SF for t=1...D'
     * Equation (14a) & (14b)
     * but using combined Psi from Eq.(9) and Eq.(12) instead of initial Psi map
     * 
     * @param pReturnTime int
     * @param pRainfallDuration int
     * @param inPsi_Initial GridCoverage2D
     * @param inSoil_Thickness GridCoverage2D
     * @param inPhi GridCoverage2D
     * @param inGammaSoil GridCoverage2Di
     * @param inCohesion GridCoverage2D
     * @param inAlfaVanGen GridCoverage2D
     * @param inNVanGen GridCoverage2D
     * @param inSlopeFixedBorders GridCoverage2D
     * @param pm
     * @return GridCoverage2D Safety_Factor_initial
     */
    public static GridCoverage2D computeSafetyFactor_InfiniteSlope_with_SaturatedZones(int pReturnTime, int pRainfallDuration, GridCoverage2D inPsi_Combined, GridCoverage2D inSoil_Thickness, GridCoverage2D inPhi, GridCoverage2D inGammaSoil, GridCoverage2D inCohesion, GridCoverage2D inAlfaVanGen, GridCoverage2D inNVanGen, GridCoverage2D inSlopeFixedBorders, IJGTProgressMonitor pm) {
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPhi);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        
        RandomIter inPsi_Combined_Iter = RandomIterFactory.create(inPsi_Combined.getRenderedImage(), null);
        RandomIter inSoil_Thickness_Iter = RandomIterFactory.create(inSoil_Thickness.getRenderedImage(), null);
        RandomIter inPhi_Iter = RandomIterFactory.create(inPhi.getRenderedImage(), null);
        RandomIter inGammaSoil_Iter = RandomIterFactory.create(inGammaSoil.getRenderedImage(), null);
        RandomIter inCohesion_Iter = RandomIterFactory.create(inCohesion.getRenderedImage(), null);
        RandomIter inAlfaVanGen_Iter = RandomIterFactory.create(inAlfaVanGen.getRenderedImage(), null);
        RandomIter inNVAnGen_Iter = RandomIterFactory.create(inNVanGen.getRenderedImage(), null);
        RandomIter inSlopeFixedBorders_Iter = RandomIterFactory.create(inSlopeFixedBorders.getRenderedImage(), null);
        
        // Prepare objects for iterating over the maps
        BandedSampleModel bandedSampleModel = new BandedSampleModel(DataBuffer.TYPE_DOUBLE, cols, rows, pRainfallDuration);
        
        WritableRaster mapSafetyFactor_Hydologic_WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, bandedSampleModel, JGTConstants.doubleNovalue);
        
        double psi_combined;
        double soil_thick;
        double tan_phi;
        double gamma;
        double gamma_w = 1;
        double cohesion;
        double alfaVanGen;
        double nVanGen;
        double angle;
        double tg;
        double ctg;
        double pSF_Hydologic;
        
        for( int t = 1; t <= pRainfallDuration; t++ ) {
            for( int c = 0; c < cols; c++ ) {
                for( int r = 0; r < rows; r++ ) {
                    
                    psi_combined = inPsi_Combined_Iter.getSampleDouble(c, r, (t-1));
                    
                    soil_thick = inSoil_Thickness_Iter.getSampleDouble(c, r, 0);
                    tan_phi = Math.tan(inPhi_Iter.getSampleDouble(c, r, 0)/180.0*Math.PI);
                    gamma = inGammaSoil_Iter.getSampleDouble(c, r, 0)/10.0;
                    cohesion = inCohesion_Iter.getSampleDouble(c, r, 0);
                    alfaVanGen = inAlfaVanGen_Iter.getSampleDouble(c, r, 0);
                    nVanGen = inNVAnGen_Iter.getSampleDouble(c, r, 0);
                    angle = Math.atan(inSlopeFixedBorders_Iter.getSampleDouble(c, r, 0));
                    tg = Math.tan(angle);
                    ctg = 1.0/tg;
                    
                    if(psi_combined <= 0.0){
                                            
                        pSF_Hydologic =
                                (tan_phi/tg)+
                                (gamma_w/gamma)*(psi_combined/soil_thick)*(tg+ctg)*tan_phi+
                                (2*cohesion)/
                                ((gamma*10)*soil_thick*Math.sin(2*angle));
                        
                        mapSafetyFactor_Hydologic_WR.setSample(c, r, (t-1), pSF_Hydologic);
                    }
                    
                    if(psi_combined > 0.0){
                    
                        pSF_Hydologic = 
                                (tan_phi/tg)+
                                ( Math.pow( (1+ Math.pow((alfaVanGen*psi_combined),nVanGen)),(-1-1/nVanGen) ) )*
                                (gamma_w/gamma)*(psi_combined/soil_thick)*(tg+ctg)*tan_phi+
                                (2*cohesion)/
                                ((gamma*10)*soil_thick*Math.sin(2*angle));
                        
                        mapSafetyFactor_Hydologic_WR.setSample(c, r, (t-1), pSF_Hydologic);
                    }
                    
                    
                    
                }
            }
        }
        return CoverageUtilities.buildCoverage("Safety_Factor_Hydrologic_" + pReturnTime + "y_" + pRainfallDuration + "h", mapSafetyFactor_Hydologic_WR, regionMap, inSoil_Thickness.getCoordinateReferenceSystem());
        
    }
    
    

    public static GridCoverage2D computeSafetyFactorTOTAL(GridCoverage2D[] singleBandCoverageArray, int returnTime, IJGTProgressMonitor pm ){
        
        if(singleBandCoverageArray.length>0){
            
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(singleBandCoverageArray[0]);
            int cols = regionMap.getCols();
            int rows = regionMap.getRows();
        
            RandomIter[] mapIterators = new RandomIter[singleBandCoverageArray.length];
            WritableRaster singleBandWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
            WritableRandomIter singleBand_Iter =  RandomIterFactory.createWritable(singleBandWR, null);
            
            String[] safetyFactorMapsNames = new String[singleBandCoverageArray.length];
            
            for(int i = 0; i <singleBandCoverageArray.length; i++){                
                mapIterators[i] = RandomIterFactory.create(singleBandCoverageArray[i].getRenderedImage(), null);
                safetyFactorMapsNames[i] = singleBandCoverageArray[i].getName().toString(Locale.US);
            }
            
            double[] safetyFactorValues = new double[singleBandCoverageArray.length];
            
            
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    
                    for(int i = 0; i <singleBandCoverageArray.length; i++){                
                        safetyFactorValues[i] = mapIterators[i].getSampleDouble(c, r, 0);    
                    }
                    
                    double worstCaseSafetyFactor = minValue(safetyFactorValues);
                    
                    if (!Double.isNaN(worstCaseSafetyFactor)){
                        singleBand_Iter.setSample(c, r, 0, worstCaseSafetyFactor);
                    }
                }
            }
            
            String suffix = buildSuffixFromSFMapsNames(safetyFactorMapsNames);
            
            GridCoverage2D outCoverage = CoverageUtilities.buildCoverage("SAFETY_FACTOR_HYDROLOGIC_" + returnTime + "y_" + suffix + "_TOT", singleBandWR, regionMap,
                    singleBandCoverageArray[0].getCoordinateReferenceSystem());
            
            return outCoverage;
        
        } else {
            if(pm!=null){pm.errorMessage("Provided coverage array is empty! Could not compute total.");}
        }
        return null;
        
    }
    
    

    public static GridCoverage2D fixBorders_AvgSlopeCum( GridCoverage2D avg_slope_cum, GridCoverage2D inSlopeFixZeroValuesAndBorder ) {
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(avg_slope_cum);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        WritableRaster avg_slope_cumWR = CoverageUtilities.renderedImage2WritableRaster(avg_slope_cum.getRenderedImage(), false);
        RandomIter inSlopeFixZeroValuesAndBorderIter = RandomIterFactory.create(inSlopeFixZeroValuesAndBorder.getRenderedImage(), null);
        
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double a = avg_slope_cumWR.getSampleDouble(c, r, 0);
                double b = inSlopeFixZeroValuesAndBorderIter.getSampleDouble(c, r, 0);
                if(Double.isNaN(a) && !Double.isNaN(b)){
                    avg_slope_cumWR.setSample(c, r, 0, b);
                }
            }
        }
        
        return CoverageUtilities.buildCoverage("avg_slope_cum_fixed", avg_slope_cumWR, regionMap, avg_slope_cum.getCoordinateReferenceSystem());
        
    }

    public static GridCoverage2D fixBorders_AvgSoilThicknessCum( GridCoverage2D avg_soil_thickness_cum, GridCoverage2D inSoilThickness ) {
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(avg_soil_thickness_cum);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        WritableRaster avg_soil_thickness_cumWR = CoverageUtilities.renderedImage2WritableRaster(avg_soil_thickness_cum.getRenderedImage(), false);
        RandomIter inSoilThicknessIter = RandomIterFactory.create(inSoilThickness.getRenderedImage(), null);
        
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double a = avg_soil_thickness_cumWR.getSampleDouble(c, r, 0);
                double b = inSoilThicknessIter.getSampleDouble(c, r, 0);
                if(Double.isNaN(a) && !Double.isNaN(b)){
                    avg_soil_thickness_cumWR.setSample(c, r, 0, b);
                }
            }
        }
        
        return CoverageUtilities.buildCoverage("avg_soil_thick_cum_fixed", avg_soil_thickness_cumWR, regionMap, avg_soil_thickness_cum.getCoordinateReferenceSystem());
        
    }

    public static GridCoverage2D fixBorders_AvgAbCum( GridCoverage2D avg_ab_cum,  GridCoverage2D inPit ) {
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(avg_ab_cum);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        final double cellArea = regionMap.getXres() * regionMap.getYres();

        // Prepare objects for iterating over the maps
        WritableRaster avg_ab_cumWR = CoverageUtilities.renderedImage2WritableRaster(avg_ab_cum.getRenderedImage(), false);
        RandomIter inPitIter = RandomIterFactory.create(inPit.getRenderedImage(), null);
        
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double a = avg_ab_cumWR.getSampleDouble(c, r, 0);
                double b = inPitIter.getSampleDouble(c, r, 0);
                if(Double.isNaN(a) && !Double.isNaN(b)){
                    avg_ab_cumWR.setSample(c, r, 0, cellArea);
                }
            }
        }
        
        return CoverageUtilities.buildCoverage("avg_ab_cum_fixed", avg_ab_cumWR, regionMap, avg_ab_cum.getCoordinateReferenceSystem());
        
    }

    
    
    public static GridCoverage2D fixBorders_Dist( GridCoverage2D cumDIST, GridCoverage2D inPit, GridCoverage2D inFlowWithBorderAndOutlet ) {
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes =regionMap.getXres();
        double yRes = regionMap.getYres();
                
        // Prepare objects for iterating over the maps
        WritableRaster cumDist_WR = CoverageUtilities.renderedImage2WritableRaster(cumDIST.getRenderedImage(), false);
        RandomIter inPitIter = RandomIterFactory.create(inPit.getRenderedImage(), null);
        RandomIter inFlowWithBorderAndOutletIter = RandomIterFactory.create(inFlowWithBorderAndOutlet.getRenderedImage(), null);
        
        double diag =  Math.sqrt( (xRes * xRes) + (yRes * yRes) );
        
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double d = cumDist_WR.getSampleDouble(c, r, 0);
                double p = inPitIter.getSampleDouble(c, r, 0);                
                if(d == 0 && !Double.isNaN(p)){
                    double f = inFlowWithBorderAndOutletIter.getSampleDouble(c, r, 0);
                    
                    if((f % 2) == 0){
                        cumDist_WR.setSample(c, r, 0, diag);
                        }
                    else{
                        if(f == 1.0 || f == 5.0)
                            cumDist_WR.setSample(c, r, 0, xRes);
                        if(f == 3.0 || f == 7.0)
                            cumDist_WR.setSample(c, r, 0, yRes);
                    }
                }
                else if (d == 0 && Double.isNaN(p)){
                    cumDist_WR.setSample(c, r, 0, JGTConstants.doubleNovalue);
                }
            }
        }
        
        return CoverageUtilities.buildCoverage("dist_cum_fixed", cumDist_WR, regionMap, inPit.getCoordinateReferenceSystem());
        
    }

    
    
    public static GridCoverage2D fixBorders_Ratio( GridCoverage2D cumRATIO, GridCoverage2D fixedBorders_Dist, GridCoverage2D inPit, GridCoverage2D inTheta_s, GridCoverage2D inKsat ) {

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double dx =regionMap.getXres();
        double dy = regionMap.getYres();                

        // Prepare objects for iterating over the maps
        WritableRaster cumRATIO_WR = CoverageUtilities.renderedImage2WritableRaster(cumRATIO.getRenderedImage(), false);
        RandomIter fixedBorders_DistIter = RandomIterFactory.create(fixedBorders_Dist.getRenderedImage(), null);
        RandomIter inPitIter = RandomIterFactory.create(inPit.getRenderedImage(), null);
        RandomIter inTheta_sIter = RandomIterFactory.create(inTheta_s.getRenderedImage(), null);
        RandomIter inKsatIter = RandomIterFactory.create(inKsat.getRenderedImage(), null);
        
        double diag =  Math.sqrt( (dx * dx) + (dy * dy) );
        
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double ratio = cumRATIO_WR.getSampleDouble(c, r, 0);
                double elev = inPitIter.getSampleDouble(c, r, 0);                
                if(ratio == 0 && !Double.isNaN(elev)){
                    
                    double dist = fixedBorders_DistIter.getSampleDouble(c, r, 0);
                    double theta_s = inTheta_sIter.getSampleDouble(c, r, 0);
                    
                    double borderRATIO =
                            (dist*theta_s)/
                            (
                            Math.cos(Math.atan(                            
                                    maxValue(new double[]{0.009,
                            ((elev-inPitIter.getSampleDouble(c+1, r, 0))/dx),
                            ((elev-inPitIter.getSampleDouble(c+1, r-1, 0))/(diag)),
                            ((elev-inPitIter.getSampleDouble(c, r-1, 0))/dy),
                            ((elev-inPitIter.getSampleDouble(c-1, r-1, 0))/(diag)),
                            ((elev-inPitIter.getSampleDouble(c-1, r, 0))/dx),
                            ((elev-inPitIter.getSampleDouble(c-1, r+1, 0))/(diag)),
                            ((elev-inPitIter.getSampleDouble(c, r+1, 0))/dy),
                            ((elev-inPitIter.getSampleDouble(c+1, r+1, 0))/(diag))
                            })
                            ))*
                            Math.sin(Math.atan(                            
                                    maxValue(new double[]{0.009,
                            ((elev-inPitIter.getSampleDouble(c+1, r, 0))/dx),
                            ((elev-inPitIter.getSampleDouble(c+1, r-1, 0))/(diag)),
                            ((elev-inPitIter.getSampleDouble(c, r-1, 0))/dy),
                            ((elev-inPitIter.getSampleDouble(c-1, r-1, 0))/(diag)),
                            ((elev-inPitIter.getSampleDouble(c-1, r, 0))/dx),
                            ((elev-inPitIter.getSampleDouble(c-1, r+1, 0))/(diag)),
                            ((elev-inPitIter.getSampleDouble(c, r+1, 0))/dy),
                            ((elev-inPitIter.getSampleDouble(c+1, r+1, 0))/(diag))
                            })
                            ))*
                            inKsatIter.getSampleDouble(c, r, 0)
                            );
                    
                    cumRATIO_WR.setSample(c, r, 0, borderRATIO);
                }
                else if (ratio == 0 && Double.isNaN(elev)){
                    cumRATIO_WR.setSample(c, r, 0, JGTConstants.doubleNovalue);
                }
            }
        }
        
        return CoverageUtilities.buildCoverage("dist_cum_fixed", cumRATIO_WR, regionMap, inPit.getCoordinateReferenceSystem());
        
    }

    /**
     * Runs across the map of along-flowpath-cumulated Twt times and rebuilds values at borders
     * @param avg_th_time_cum
     * @param singleTwtMap
     * @return
     */
    public static GridCoverage2D fixBorders_AvgThTime( GridCoverage2D avg_th_time_cum, GridCoverage2D singleTwtMap ) {

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(avg_th_time_cum);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();               

        // Prepare objects for iterating over the maps
        WritableRaster avg_th_time_cum_WR = CoverageUtilities.renderedImage2WritableRaster(avg_th_time_cum.getRenderedImage(), false);
        RandomIter singleTwtMapIter = RandomIterFactory.create(singleTwtMap.getRenderedImage(), null);
        
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                
                double TH_TIME = avg_th_time_cum_WR.getSampleDouble(c, r, 0);
                double th_time = singleTwtMapIter.getSampleDouble(c, r, 0); 
                
                if( Double.isNaN(TH_TIME) && !Double.isNaN(th_time) ){
                    avg_th_time_cum_WR.setSample(c, r, 0, th_time);
                } else if (Double.isNaN(th_time)){
                    avg_th_time_cum_WR.setSample(c, r, 0, JGTConstants.doubleNovalue);
                }
            }
        }
        
        return CoverageUtilities.buildCoverage("th_time_cum_fixed", avg_th_time_cum_WR, regionMap, avg_th_time_cum.getCoordinateReferenceSystem());
        
    }
    


    public static String getCoverageStatsAsString( GridCoverage2D coverage ) {
        double[] stats = CoverageUtilities.getMinMaxMeanSdevCount(coverage, null);
        String statsToString = "[Min Max Mean Sdev Count: ";
        String spacer = "";
        for( int s = 0; s < 4; s++ ) {
            statsToString += spacer + round8Decimals(stats[s]);
            spacer = ", ";
        }
        statsToString += spacer + Math.round(stats[4]);
        statsToString += " ]";
        return statsToString;
    }
    
    
    public static GridCoverage2D getSingleBandFromMultibandCoverage(int bandIndex, int returnTime, GridCoverage2D multibandCoverage){
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(multibandCoverage);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        RandomIter mapIter = RandomIterFactory.create(multibandCoverage.getRenderedImage(), null);

        int bands = multibandCoverage.getNumSampleDimensions();
        
        WritableRaster singleBandWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        //WritableRandomIter singleBand_Iter =  RandomIterFactory.createWritable(singleBandWR, null);
        
        double mapValue;
        
        if( bandIndex < bands) {

            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    mapValue = mapIter.getSampleDouble(c, r, bandIndex);
                    if (!Double.isNaN(mapValue)){
                        singleBandWR.setSample(c, r, 0, mapValue);
                    }
                }
            }
        }
        
        GridCoverage2D outCoverage = CoverageUtilities.buildCoverage(multibandCoverage.getName() + "_" + (bandIndex+1) + "h", singleBandWR, regionMap,
                multibandCoverage.getCoordinateReferenceSystem());
        
        return outCoverage;
    }
    
    
    public static String buildSuffixFromSFMapsNames(String[] names){
        
        String suffix = "";
        for(int i = 0; i <names.length; i++){                
            int start = names[i].lastIndexOf("_");                
            int end = names[i].lastIndexOf("h"); 
            
            suffix += names[i].substring(start+1, end+1);
            
        }
        return suffix;
    }
    
    public static GridCoverage2D[] fromMultibandCoverageToCoverageArray(GridCoverage2D multibandCoverage){
        
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(multibandCoverage);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        RandomIter mapIter = RandomIterFactory.create(multibandCoverage.getRenderedImage(), null);

        int bands = multibandCoverage.getNumSampleDimensions();
        double[] pixel = new double[bands];
        WritableRaster[] psiMultiHoursWR = new WritableRaster[bands];
        WritableRandomIter[] psiWRIter = new WritableRandomIter[bands];
        for( int i = 0; i < bands; i++ ) {
            psiMultiHoursWR[i] = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
            psiWRIter[i] = RandomIterFactory.createWritable(psiMultiHoursWR[i], null);
        }

        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                mapIter.getPixel(c, r, pixel);
                for( int b = 0; b < pixel.length; b++ ) {
                    if (pixel[b] > 0.0)
                        psiWRIter[b].setSample(c, r, 0, pixel[b]);
                }
            }
        }
        
        GridCoverage2D[] outCoverageArray = new GridCoverage2D[bands];
        for( int i = 0; i < bands; i++ ) {
            outCoverageArray[i] = CoverageUtilities.buildCoverage("psi", psiMultiHoursWR[i], regionMap,
                    multibandCoverage.getCoordinateReferenceSystem());
        }
        
        return outCoverageArray;
        
    } 


    public static double round8Decimals( double d ) {

    	Locale loc = Locale.US;
    	
    	NumberFormat f = NumberFormat.getInstance(loc);
    	 if (f instanceof DecimalFormat) {
    	     ((DecimalFormat) f).setDecimalSeparatorAlwaysShown(true);
    	 }
    	 
    	 f.setMaximumFractionDigits(8);
    	 f.setMinimumIntegerDigits(1);

        if (!Double.isInfinite(d) && !Double.isNaN(d)) {
            return Double.valueOf(f.format(d));
        } else {
            return d;
        }
    }
    
    
    public static double maxValue(double[] array) {
        double max = array[0];
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }
    
    public static double minValue(double[] array) {
        double min = array[0];
        for (int i = 0; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    
    /*
    public static GridCoverage2D[] calculateMapsOfPsiAtBedrockDuringVerticalInfiltration( GridCoverage2D Twt, GridCoverage2D psiInit,
            double I, GridCoverage2D soil_thickness, GridCoverage2D theta_s, GridCoverage2D theta_r, GridCoverage2D alfaVanGenuchten,
            GridCoverage2D nVanGenuchten, IJGTProgressMonitor pm ) {

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(soil_thickness);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        RandomIter mapPsiInitIter = RandomIterFactory.create(psiInit.getRenderedImage(), null);
        RandomIter mapSoilThicknessIter = RandomIterFactory.create(soil_thickness.getRenderedImage(), null);
        RandomIter mapTheta_SIter = RandomIterFactory.create(theta_s.getRenderedImage(), null);
        RandomIter mapTheta_RIter = RandomIterFactory.create(theta_r.getRenderedImage(), null);
        RandomIter mapAalfaVanGenuchtenIter = RandomIterFactory.create(alfaVanGenuchten.getRenderedImage(), null);
        RandomIter mapNVanGenuchtenIter = RandomIterFactory.create(nVanGenuchten.getRenderedImage(), null);

        // WritableRaster psiWR = CoverageUtilities.renderedImage2WritableRaster(psiInit.getRenderedImage(), false);
        // WritableRandomIter psiWRIter = RandomIterFactory.createWritable(psiWR, null);

        double maxTwt = CoverageUtilities.getMinMaxMeanSdevCount(Twt, null)[1];
        int countOfToBeStoredPsiCoverages = (int) (Math.round(maxTwt)) + 1;

        int[] bandOffsets = new int[countOfToBeStoredPsiCoverages];
        for( int o = 0; o < countOfToBeStoredPsiCoverages; o++ ) {
            bandOffsets[o] = o * cols;
        }

        ComponentSampleModel sampleModel_old = new ComponentSampleModel(DataBuffer.TYPE_DOUBLE, cols, rows, 1, cols, bandOffsets);
        BandedSampleModel sampleModel = new BandedSampleModel(DataBuffer.TYPE_DOUBLE, cols, rows, countOfToBeStoredPsiCoverages);
        
        int numBands = sampleModel.getNumBands();
        WritableRaster psiMultiHoursWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, sampleModel, JGTConstants.doubleNovalue);
        WritableRandomIter psiWRIter = RandomIterFactory.createWritable(psiMultiHoursWR, null);

        double pPreviousPsi_bb, pI, pSoil_thickness, pTheta_s, pTheta_r, pAlfaVanGenuchten, pNVanGenuchten;

        pm.beginTask("Started computing Psi maps after each of the " + numBands + " hour/s of simulated rainfall...", rows);
        long start = System.nanoTime();
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                // Check we are in the basin by probing Soil Thickness
                pSoil_thickness = mapSoilThicknessIter.getSampleDouble(c, r, 0);

                if (!Double.isNaN(pSoil_thickness)) {

                    pPreviousPsi_bb = mapPsiInitIter.getSampleDouble(c, r, 0);
                    pI = I;
                    pTheta_s = mapTheta_SIter.getSampleDouble(c, r, 0);
                    pTheta_r = mapTheta_RIter.getSampleDouble(c, r, 0);
                    pAlfaVanGenuchten = mapAalfaVanGenuchtenIter.getSampleDouble(c, r, 0);
                    pNVanGenuchten = mapNVanGenuchtenIter.getSampleDouble(c, r, 0);

                    double[] pixel = new double[countOfToBeStoredPsiCoverages];
                    // Cycle in the total number of simulation hours (derived from the max Twt time of the basin for given Return Time and
                    // Rainfall Intensity
                    for( int j = 0; j < countOfToBeStoredPsiCoverages; j++ ) {
                        int i = 1;
                        // Loop until the sum of iteration steps over one cell reaches one hour (3600 sec) then write computed psi to raster
                        while( ((i * ParameterCalculationFunctions.DELTA_T_INTEGRATION_STEP_IN_SECONDS) % 3600) != 0 ) {

                            // int counter = ((i * ParameterCalculationFunctions.DELTA_T_INTEGRATION_STEP_IN_SECONDS) % 3600);

                            pPreviousPsi_bb = ParameterCalculationFunctions.calculatePsiAtBedrockDuringVerticalInfiltration(
                                    pPreviousPsi_bb, pI, pSoil_thickness, pTheta_s, pTheta_r, pAlfaVanGenuchten, pNVanGenuchten);
                            i++;
                        }
                        // psiWRIter.setSample(c, r, j, pPreviousPsi_bb);
                        pixel[j] = pPreviousPsi_bb;
                    }
                    psiWRIter.setPixel(c, r, pixel);
                    // DEBUG CODE
                    //
                    //double[] outPixel = new double[4];
                    //psiMultiHoursWR.getPixel(252,682,outPixel);
                    //psiMultiHoursWR.getPixel(c,r,outPixel);
                    //double[] bandValues = outPixel;
                    //
                }
            }
            pm.worked(1);
        }
        pm.message("Elapsed time: " + (System.nanoTime() - start) / 1000000000 + " sec.");
        pm.done();

        GridCoverage2D[] psiMapsAtIncreasingHours = new GridCoverage2D[psiMultiHoursWR.getNumBands()];

        for( int band_Hour_Index = 0; band_Hour_Index < psiMultiHoursWR.getNumBands(); band_Hour_Index++ ) {

            int[] bandList = new int[]{band_Hour_Index};
            WritableRaster currentPsiBand = psiMultiHoursWR.createWritableChild(0, 0, cols, rows, 0, 0, bandList);

            psiMapsAtIncreasingHours[band_Hour_Index] = CoverageUtilities.buildCoverage("psimultihours", currentPsiBand, regionMap,
                    Twt.getCoordinateReferenceSystem());
        }
        // PROBE CODE for Coverages as GridCoverage2D ARRAY
        //
        //final Point2D.Double point = new Point2D.Double();
        //point.x = 623630.1509;
        //point.y = 5137453.8354;
        //double[] bufferCov = null;
        //for( int band_Hour_Index = 0; band_Hour_Index < psiMultiHoursWR.getNumBands(); band_Hour_Index++ ) {
        //    bufferCov = psiMapsAtIncreasingHours[band_Hour_Index].evaluate(point, bufferCov);
        //}
        //
        return psiMapsAtIncreasingHours;

    }
    */
    
    
}
