/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_doCalculateInitialSafetyFactor_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_doSaveByproducts_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inAb_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inAlfaVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inCohesion_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inGamma_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inKsat_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inNVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inPhi_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inPsiInitAtBedrock_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSoilThickness_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_r_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_s_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pCV_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pOutFolder_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pRainfallDurations_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pReturnTimes_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pSigma1_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pmF_DESCRIPTION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.demmanipulation.markoutlets.OmsMarkoutlets;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models.OmsV0;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models.OmsVwt;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils.MapCalculationFunctions;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils.MapCalculationFunctions.MapOfCumulatedValue;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils.MapDumpingFunctions;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils.MapPreprocessingUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils.ParameterCalculationFunctions;

/**
 * 
 * @author Marco Foi (www.mcfoi.it)
 * 
 */
@Description(OMSCISLAM_DESCRIPTION)
@Documentation(OMSCISLAM_DOCUMENTATION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_KEYWORDS)
@Label(OMSCISLAM_LABEL)
@Name(OMSCISLAM_NAME)
@Status(OMSCISLAM_STATUS)
@License(OMSCISLAM_LICENSE)
public class OmsCislam extends JGTModel {

    // Default minimum slope value used as a substitute for Slope Map values equal to zero. Required to avoid division by zero.
    public static final double MINIMM_ALLOWED_SLOPE = 0.009;
    // Default rainfall durations used to compute maps of times to develop water table.
    private static final String RAINFALL_DURATIONS_ARRAY = "1, 3, 6, 12, 24";



    @Description(OMSCISLAM_pReturnTimes_DESCRIPTION)
    @Unit("years")
    @In
    public String pReturnTimes = new String("30, 100, 200");

    @Description(OMSCISLAM_pRainfallDurations_DESCRIPTION)
    @Unit("hours")
    @In
    public String pRainfallDurations = RAINFALL_DURATIONS_ARRAY;
    
    // Input files
    @Description(OMSCISLAM_inPit_DESCRIPTION)
    @Unit("m")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inPit = null;

    @Description(OMSCISLAM_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSCISLAM_inSlope_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inSlope = null;

    @Description(OMSCISLAM_inAb_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inAb = null;
    
    @Description(OMSCISLAM_inSoilThickness_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @Unit("m")
    @In
    public GridCoverage2D inSoilThickness = null;

    // #############################################
    // Rainfall-statistics related parameters
    // #############################################
    @Description(OMSCISLAM_pSigma1_DESCRIPTION)
    @In
    public double pSigma1 = 11.82;

    @Description(OMSCISLAM_pmF_DESCRIPTION)
    @In
    public double pmF = 0.54;

    @Description(OMSCISLAM_pCV_DESCRIPTION)
    @In
    public double pCV = 0.23;

    // #############################################
    // Hydrologic parameters
    // #############################################
    @Description(OMSCISLAM_inPsiInitAtBedrock_DESCRIPTION)
    @Unit("m")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inPsiInitAtBedrock = null;

    // #############################################
    // Geo-Techical parameters
    // #############################################
    @Description(OMSCISLAM_inCohesion_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @Unit("kPa")
    @In
    public GridCoverage2D inCohesion = null;
    /*
    @Description(OMSCISLAM_pCohesion_DESCRIPTION)
    @Unit("Pa")
    @In
    public double pCohesion = -1.0;
    */
    @Description(OMSCISLAM_inPhi_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inPhi = null;
    /*
    @Description(OMSCISLAM_pPhi_DESCRIPTION)
    @In
    public double pPhi = 30.0;
    */
    @Description(OMSCISLAM_inGamma_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inGammaSoil = null;
    /*
    @Description(OMSCISLAM_pGamma_DESCRIPTION)
    @In
    public double pGamma = 19.0;
    */
    @Description(OMSCISLAM_inKsat_DESCRIPTION)
    @Unit("m/s")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inKsat = null;
    /*
    @Description(OMSCISLAM_pKsat_DESCRIPTION)
    @In
    public double pKsat_ = 10 ^ (-6);
	*/
    @Description(OMSCISLAM_inTheta_s_DESCRIPTION)
    @Unit("-")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inTheta_s = null;
    /*
    @Description(OMSCISLAM_pTheta_s_DESCRIPTION)
    @In
    public double pTheta_s = 0.3;
	*/
    @Description(OMSCISLAM_inTheta_r_DESCRIPTION)
    @Unit("-")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inTheta_r = null;
    /*
    @Description(OMSCISLAM_pTheta_r_DESCRIPTION)
    @In
    public double pTheta_r = 0.03;
    */
    
    // #############################################
    // van Genuchten parameters
    // #############################################
    @Description(OMSCISLAM_inAlfaVanGen_DESCRIPTION)
    @Unit("1/m")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inAlfaVanGen = null;
    /*
    @Description(OMSCISLAM_pAlfaVanGen_DESCRIPTION)
    @In
    public double pAlfaVanGen = 6.5;
    */
    @Description(OMSCISLAM_inNVanGen_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @Unit("-")
    @In
    public GridCoverage2D inNVanGen = null;
    /*
    @Description(OMSCISLAM_pNVanGen_DESCRIPTION)
    @Unit("-")
    @In
    public double pNVanGen = 1.36;
	*/
    
    // #############################################
    // Calculate hydrologic Initial Safety Factor
    // #############################################
    @Description(OMSCISLAM_doCalculateInitialSafetyFactor_DESCRIPTION)
    @In
    public boolean doCalculateInitialSafetyFactor = false;
    
    
    // #############################################
    // Save by-products to file
    // #############################################
    @Description(OMSCISLAM_doSaveByproducts_DESCRIPTION)
    @In
    public boolean doSaveByProducts = false;

    @Description(OMSCISLAM_pOutFolder_DESCRIPTION)
    @UI(JGTConstants.FOLDEROUT_UI_HINT)
    @In
    public String pOutFolder = System.getProperty("java.io.tmpdir");

    // #############
    // References to internal by-products for DEBUGGING and TESTING purposes
    public GridCoverage2D outMarkedFlow_Raster = null;
    public GridCoverage2D outFixedSlope_Raster = null;
    public GridCoverage2D out_avg_slope_cum_Raster = null;
    public GridCoverage2D out_avg_soil_thickness_cum_Raster = null;
    public GridCoverage2D out_avg_th_time_cum_Raster = null;
    public GridCoverage2D out_avg_ab_cum_Raster = null;
    public GridCoverage2D out_cumRatio_Raster = null;
    public GridCoverage2D out_cumDist_Raster = null;
    public GridCoverage2D out_cumCont_Raster = null;
    public GridCoverage2D out_avg_slope_cum_fixed_Raster = null;
    public GridCoverage2D out_avg_soil_thickness_cum_fixed_Raster = null;
    public GridCoverage2D out_avg_th_time_cum_fixed_Raster = null;
    public GridCoverage2D out_avg_ab_cum_fixed_Raster = null;
    public GridCoverage2D out_cumRatio_fixed_Raster = null;
    public GridCoverage2D out_cumDist_fixed_Raster = null;
    public GridCoverage2D out_IT_din_lin_Raster = null;
    public GridCoverage2D out_psi_b_Eq9_Raster = null;
    public GridCoverage2D out_psi_b_Eq12_Raster = null;
    public GridCoverage2D out_psi_b_Combined_Raster = null;
    public GridCoverage2D out_SafetyFactor_Initial_Raster = null;
    public GridCoverage2D out_SafetyFactor_InfiniteSlope_accounting_for_SaturatedZones_Raster = null;
    public GridCoverage2D out_SafetyFactor_InfiniteSlope_accounting_for_SaturatedZones_RetTime_TOTAL_Raster = null;
    // #############

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        // if (!concatOr(outAspect == null, doReset)) {
        // return;
        // }

        // Check required parameters
    	checkNull(pReturnTimes);
    	checkNull(pRainfallDurations);
    	    	
        checkNull(inPit);
        checkNull(inFlow);
        checkNull(inSlope);
        checkNull(inAb);
        checkNull(inSoilThickness);
        checkNull(inPsiInitAtBedrock);
        
        checkNull(pSigma1, pmF, pCV);
        
        checkNull(inTheta_s);
        checkNull(inTheta_r);        
        
        checkNull(inAlfaVanGen);
        checkNull(inNVanGen);
        
        checkNull(inKsat);
        checkNull(inGammaSoil);
        checkNull(inCohesion);
        checkNull(inPhi);
        
        if(pOutFolder == "" || pOutFolder == null){
        	pOutFolder = System.getProperty("java.io.tmpdir");
        }

        pOutFolder = MapDumpingFunctions.fixOutputFolderString(pOutFolder);
        
        // Take flow map, created by the user using OmsFlowDirections, and rebuild border
        GridCoverage2D inFlowWithBorder = MapPreprocessingUtilities.flowMapRebuildBorder(inFlow, inPit, pm);

        // Take flow map with proper border and mark basin outlet with value 10
        GridCoverage2D inFlowWithBorderAndOutlet = MapPreprocessingUtilities.flowMapMarkOutlet(inFlowWithBorder, pm);

        // Use default framework facility to mark outlets
        // Mark basin outlet with value 10
        /*
        OmsMarkoutlets markOutlets = new OmsMarkoutlets();
        markOutlets.inFlow = inFlowWithBorder;
        markOutlets.process();
        inFlowWithBorderAndOutlet = markOutlets.outFlow;
        */

        // ##################################################################################
        // Rebuild border of slope map, computing cell values, and replace zero values with a not-null minimum
        GridCoverage2D inSlopeFixZeroValuesAndBorder = MapPreprocessingUtilities.slopeMapFixZeroValuesAndBorder(inSlope, inPit,
                OmsCislam.MINIMM_ALLOWED_SLOPE, pm);

        // ##################################################################################
        // Parse simulated Rainfall Durations string into an array of integers
        ArrayList<Integer> rainfallDurationsArrayList = ParameterCalculationFunctions.parseRainfallDurationsString(pRainfallDurations, pm);

        // Parse Return Times string into an array of integers
        ArrayList<Integer> returnTimesArrayList = ParameterCalculationFunctions.parseReturnTimesString(pReturnTimes, pm);

        // ##################################################################################
        // Compute Th_time from Eq. (7) and Eq. (6) of reference paper.

        // ##==## Equation (7)
        OmsVwt mVwt = new OmsVwt();
        mVwt.inSoilThickness = inSoilThickness;
        mVwt.inTheta_r = inTheta_r;
        mVwt.inTheta_s = inTheta_s;
        mVwt.inAlfaVanGen = inAlfaVanGen;
        mVwt.inNVanGen = inNVanGen;
        mVwt.process();
        GridCoverage2D outVwt = mVwt.outVwt;

        /*
        OmsPsiInitAtBedrock psiInitAtBedrock = new OmsPsiInitAtBedrock();
        psiInitAtBedrock.inPit = inPit;
        psiInitAtBedrock.pPsiInitAtBedrokConstant = 0.05;
        psiInitAtBedrock.process();
        GridCoverage2D inPsiInitAtBedrock = psiInitAtBedrock.outPsiInitAtBedrock;
        */

        // ##==## Equation (6)
        OmsV0 mV0 = new OmsV0();
        mV0.inSoilThickness = inSoilThickness;
        mV0.inTheta_r = inTheta_r;
        mV0.inTheta_s = inTheta_s;
        mV0.inAlfaVanGen = inAlfaVanGen;
        mV0.inNVanGen = inNVanGen;
        mV0.inPsiInitAtBedrock = inPsiInitAtBedrock;
        mV0.process();
        GridCoverage2D outV0 = mV0.outV0;

        HashMap<Integer[], GridCoverage2D> mapsOfTwt = new HashMap<Integer[], GridCoverage2D>();
        // Calculate Time-to-develop-Watertable maps for each provided return time for each of the default rainfall durations
        for( int pReturnTime : returnTimesArrayList ) {

            for( int pRainfallDuration : rainfallDurationsArrayList ) {

                double pRainfallIntensity = ParameterCalculationFunctions.calculateRainfallIntensity(pSigma1, pmF, pCV, pReturnTime,
                        pRainfallDuration, null);
                pm.message(" ");
                pm.message("######################################################################################");
                pm.message("######################################################################################");
                pm.message("###                                                                                ###");
                pm.message("Computing Twt map for Rainfall Duration: " + pRainfallDuration + "hours and Return Time: " + pReturnTime
                        + "years");
                GridCoverage2D rasterTwt = MapCalculationFunctions.computeMapOfTimeForWaterTableDevelopmentTwt(outVwt, outV0,
                        pRainfallIntensity, pm);

                pm.message("Stats for Twt map for Rainfall Duration " + pRainfallDuration + "hours and Return Time: " + pReturnTime
                        + "years");
                pm.message(MapCalculationFunctions.getCoverageStatsAsString(rasterTwt));

                mapsOfTwt.put(new Integer[]{pRainfallDuration, pReturnTime}, rasterTwt);
            }
        }

        int computedMaps = mapsOfTwt.size();
        pm.message(" ");
        pm.message("A total number of " + computedMaps + " have been calculated.");
        pm.message("###                                                                                ###");
        pm.message("######################################################################################");
        pm.message("######################################################################################");
        // Dump results to file
        if (doSaveByProducts) {
            MapDumpingFunctions.dumpMapsOfTwt(mapsOfTwt, pOutFolder, pm);
        }
        // End of Twt maps computation
        // ##################################################################################



        for( int y : returnTimesArrayList ) {
            
            GridCoverage2D[] mapsSafetyFactor_InfSlope_with_SaturZones_LastHour = new GridCoverage2D[rainfallDurationsArrayList.size()];
            int durationArrayStep = -1;
            
            for( int h : rainfallDurationsArrayList ) {

                durationArrayStep++;
                double pRainfallIntensity = ParameterCalculationFunctions.calculateRainfallIntensity(pSigma1, pmF, pCV, y, h, null);

                GridCoverage2D mapPsiMultiHours_Eq9 = null;
                pm.message(" ");
                pm.message("######################################################################################");
                pm.message("### COMPUTING Psi Maps FOR Return Time: " + y + " years and Rainfall Duration: " + h + " hours.");

                GridCoverage2D singleTwtMap = getTwsMap(mapsOfTwt, y, h);

                // ##################################################################################
                // Compute maps of Psi at bedrock during rainfall due to vertical infiltration
                // for increasing hours of rainfall duration
                //
                // Equation (9) of reference paper.
                //
                // Relies on Twt maps, rainfall parameters (pSigma1, pmF, pCV) and
                // 
                mapPsiMultiHours_Eq9 = MapCalculationFunctions.computeMapsOfPsiAtBedrockDuringVerticalInfiltration(singleTwtMap,
                        inPsiInitAtBedrock, pRainfallIntensity, inSoilThickness, inTheta_s, inTheta_r, inAlfaVanGen, inNVanGen, pm);

                int numMaps = mapPsiMultiHours_Eq9.getNumSampleDimensions();
                pm.message("Number of Psi hourly maps returned from computation: " + numMaps);
                
                // Dump results to file
                if (doSaveByProducts){
                    MapDumpingFunctions.dumpMapsOfPsiMultiHours(mapPsiMultiHours_Eq9, y, h, pOutFolder, pm);
                }
                
                // Export map references for allowing testing
                out_psi_b_Eq9_Raster = mapPsiMultiHours_Eq9;
            //} 
        //}
                pm.message(" ");
                pm.message("### COMPUTING Psi Maps IS COMPLETED ###");
                pm.message("#######################################");
                pm.message("#######################################");
                // End of Psi-at-bedrock-during-rainfall maps computation
                // ##################################################################################
        
                // ##################################################################################
                // Compute maps of Concentration time
                pm.message(" ");
                pm.message("###########################################");
                pm.message("### Compute maps of Concentration time. ###");
                pm.message(" ");
                
        //for( int y : returnTimesArrayList ) {

            //for( int h : rainfallDurationsArrayList ) {
                // y = 10;
                // h = 24;
                //GridCoverage2D singleTwtMap = getTwsMap(mapsOfTwt, y, h);

                pm.message(" ");
                pm.message("### First compute maps of cumulated parameters. ########################");
                pm.message(" ");
                HashMap<MapOfCumulatedValue, GridCoverage2D> mapsOfCumulatedParameters = MapCalculationFunctions
                        .calculateMapsOfCumulatedParameters(inPit, inFlowWithBorderAndOutlet, inKsat, inSoilThickness, inTheta_s, inAb,
                                singleTwtMap, pm);
                
                // Dump results to file
                if (doSaveByProducts) {
                    MapDumpingFunctions.dumpCumulatedParametersToFiles(mapsOfCumulatedParameters, pOutFolder, pm);
                }

                // Export map references for allowing testing
                out_cumRatio_Raster = mapsOfCumulatedParameters.get(MapOfCumulatedValue.RATIO);
                out_cumDist_Raster = mapsOfCumulatedParameters.get(MapOfCumulatedValue.DIST);
                out_cumCont_Raster = mapsOfCumulatedParameters.get(MapOfCumulatedValue.CONT);

                // Compute averages of cumulated parameters
                pm.message(" ");
                pm.message("### Then average maps of cumulated parameters with step-count. #########");
                pm.message(" ");
                GridCoverage2D avg_slope_cum = MapCalculationFunctions.computeMapOfAverageFromCumulated(
                        mapsOfCumulatedParameters.get(MapOfCumulatedValue.SLOPE_CUM),
                        mapsOfCumulatedParameters.get(MapOfCumulatedValue.CONT));

                GridCoverage2D avg_soil_thickness_cum = MapCalculationFunctions.computeMapOfAverageFromCumulated(
                        mapsOfCumulatedParameters.get(MapOfCumulatedValue.SOIL_THICK),
                        mapsOfCumulatedParameters.get(MapOfCumulatedValue.CONT));

                GridCoverage2D avg_th_time_cum = MapCalculationFunctions
                        .computeMapOfAverageFromCumulated(mapsOfCumulatedParameters.get(MapOfCumulatedValue.TH_TIME),
                                mapsOfCumulatedParameters.get(MapOfCumulatedValue.CONT));

                GridCoverage2D avg_ab_cum = MapCalculationFunctions.computeMapOfAverageFromCumulated(
                        mapsOfCumulatedParameters.get(MapOfCumulatedValue.AB), mapsOfCumulatedParameters.get(MapOfCumulatedValue.CONT));

                // Export map references for allowing testing
                out_avg_slope_cum_Raster = avg_slope_cum;
                out_avg_soil_thickness_cum_Raster = avg_soil_thickness_cum;
                out_avg_th_time_cum_Raster = avg_th_time_cum;
                out_avg_ab_cum_Raster = avg_ab_cum;

                // Dump results to file
                if (doSaveByProducts) {
                    MapDumpingFunctions.dumpSingleBandCoverage(avg_slope_cum, pOutFolder, pm);
                    MapDumpingFunctions.dumpSingleBandCoverage(avg_soil_thickness_cum, pOutFolder, pm);
                    MapDumpingFunctions.dumpSingleBandCoverage(avg_th_time_cum, pOutFolder, pm);
                    MapDumpingFunctions.dumpSingleBandCoverage(avg_ab_cum, pOutFolder, pm);
                }

                // Rebuild borders in average parameter maps
                pm.message(" ");
                pm.message("### Rebuild borders of averaged maps. ##################################");
                pm.message(" ");
                GridCoverage2D avg_slope_cum_BorderFixed = MapCalculationFunctions.fixBorders_AvgSlopeCum(avg_slope_cum,
                        inSlopeFixZeroValuesAndBorder);
                GridCoverage2D avg_soil_thickness_cum_BorderFixed = MapCalculationFunctions.fixBorders_AvgSoilThicknessCum(
                        avg_soil_thickness_cum, inSoilThickness);
                GridCoverage2D avg_ab_cum_BorderFixed = MapCalculationFunctions.fixBorders_AvgAbCum(avg_ab_cum, inPit);
                GridCoverage2D DIST_BorderFixed = MapCalculationFunctions.fixBorders_Dist(
                        mapsOfCumulatedParameters.get(MapOfCumulatedValue.DIST), inPit, inFlowWithBorderAndOutlet);
                GridCoverage2D RATIO_BorderFixed = MapCalculationFunctions.fixBorders_Ratio(
                        mapsOfCumulatedParameters.get(MapOfCumulatedValue.RATIO), DIST_BorderFixed, inPit, inTheta_s, inKsat);
                GridCoverage2D avg_TH_TIME_BorderFixed = MapCalculationFunctions.fixBorders_AvgThTime(avg_th_time_cum, singleTwtMap);

                // Dump results to file
                if (doSaveByProducts) {
                    MapDumpingFunctions.dumpSingleBandCoverage(avg_slope_cum_BorderFixed, pOutFolder, pm);
                    MapDumpingFunctions.dumpSingleBandCoverage(avg_soil_thickness_cum_BorderFixed, pOutFolder, pm);
                    MapDumpingFunctions.dumpSingleBandCoverage(avg_ab_cum_BorderFixed, pOutFolder, pm);
                    MapDumpingFunctions.dumpSingleBandCoverage(DIST_BorderFixed, pOutFolder, pm);
                    MapDumpingFunctions.dumpSingleBandCoverage(RATIO_BorderFixed, pOutFolder, pm);
                    MapDumpingFunctions.dumpSingleBandCoverage(avg_TH_TIME_BorderFixed, pOutFolder, pm);
                }

                // Export map references for allowing testing
                out_avg_slope_cum_fixed_Raster = avg_slope_cum_BorderFixed;
                out_avg_soil_thickness_cum_fixed_Raster = avg_soil_thickness_cum_BorderFixed;
                out_avg_ab_cum_fixed_Raster = avg_ab_cum_BorderFixed;
                out_cumDist_fixed_Raster = DIST_BorderFixed;
                out_cumRatio_fixed_Raster = RATIO_BorderFixed;
                out_avg_th_time_cum_fixed_Raster = avg_TH_TIME_BorderFixed;

                // Compute map of Dynamic Linear Topographic Index
                pm.message(" ");
                pm.message("### Now computing Dinamic Linear Topographic Index. ####################");
                pm.message(" ");
                GridCoverage2D IT_din_lin_Multiband_Coverage = MapCalculationFunctions.computeMapOfDynamicLinearTopographicIndex(avg_TH_TIME_BorderFixed,
                        singleTwtMap, y, h, RATIO_BorderFixed, inAb, inSlopeFixZeroValuesAndBorder, pm);
                
                // Dump results to file
                if (doSaveByProducts) {
                    String message = "Dumping Dinamic-Linear Topographic Index maps to ";
                    MapDumpingFunctions.dumpMultiBandCoverage(IT_din_lin_Multiband_Coverage, pOutFolder, null, message);
                }
                                
                // Export map references for allowing testing
                out_IT_din_lin_Raster = IT_din_lin_Multiband_Coverage;
                
                // ##################################################################################
                // Compute maps of Psi  for t < t_tw but t > t_wt_up 
                // so during infiltration but with active lateral flow from catchment           
                // Compute pressure at bedrock in the pore pressure positive zones
                pm.message(" ");
                pm.message("### Compute Pressure at bedrock (Psi) in the pore pressure positive zones.##");
                pm.message(" ");

                GridCoverage2D mapPsiMultiHours_Eq12 = MapCalculationFunctions.computeMapOfPsiAtBedrockInPositivePressureZones(y, h, inSoilThickness, pRainfallIntensity, inKsat, IT_din_lin_Multiband_Coverage, pm);
                
                // Dump results to file
                if (doSaveByProducts) {
                    String message = "Dumping maps of Psi at bedrock in the pore pressure positive zones to ";
                    MapDumpingFunctions.dumpMultiBandCoverage(mapPsiMultiHours_Eq12, pOutFolder, null, message);
                }
                                
                // Export map references for allowing testing
                out_psi_b_Eq12_Raster = mapPsiMultiHours_Eq12;
                
                // ##################################################################################
                // Compute maps of combined Pressure at soil-bedrock interface (Psi)
                // 
                // This matrix is the combination of the two psi matrices computed for different conditions
                //
                //  = mapPsiMultiHours_Eq9 (Eq. 9) for t < t_wt (during infiltration)
                //  = mapPsiMultiHours_Eq12  (Eq.12) for t < t_tw but t > t_wt_up (during infiltration but with active lateral flow from catchment)
                //  ( D = simulation duration )
                pm.message(" ");
                pm.message("### Compute maps of combined Pressure at soil-bedrock interface.      ##");
                pm.message(" ");
                
                GridCoverage2D mapPsiMultiHours_Combined_Eq9_Eq12 = MapCalculationFunctions.computeMapOfPsiCombined_Eq9_Eq12(y, mapPsiMultiHours_Eq9, mapPsiMultiHours_Eq12, inSoilThickness, pm);
                
                if (doSaveByProducts) {
                    String message = "Dumping maps of Combined Psi at bedrock to ";
                    MapDumpingFunctions.dumpMultiBandCoverage(mapPsiMultiHours_Combined_Eq9_Eq12, pOutFolder, null, message);
                }
                                
                // Export map references for allowing testing
                out_psi_b_Combined_Raster = mapPsiMultiHours_Combined_Eq9_Eq12;
                pm.message("###                           Calculation of combined Psi map completed                                ###");
                pm.message("##########################################################################################################");
                
                
                // ##################################################################################                
                // Compute Safety Factor with  infinite slope model and hydrologic control    #######
                // 2. SF for t=1...D
                // 
                // Equation (14a) & (14b) of reference paper
                // 
                // but using combined Psi from Eq.(9) and Eq.(12) instead of initial Psi map
                // 
                pm.message(" ");
                pm.message("##########################################################################################################");
                pm.message("### Compute the HYDROLOGY-and-GEOMECHANIC driven SAFETY FACTOR for: R.Time="+y+"y & RainDuration="+h+"h ##");
                pm.message(" ");
                
                //GridCoverage2D mapSafetyFactor_InfiniteSlope_with_SaturatedZones = MapCalculationFunctions.computeSafetyFactor_InfiniteSlope_with_SaturatedZones(y, h, mapPsiMultiHours_Combined_Eq9_Eq12, inSoilThickness, inPhi, inGammaSoil, inCohesion, inAlfaVanGen, inNVanGen, inSlopeFixZeroValuesAndBorder, pm);              
                GridCoverage2D mapSafetyFactor_InfiniteSlope_with_SaturatedZones_SingleBand = MapCalculationFunctions.computeSafetyFactor_InfiniteSlope_with_SaturatedZones_SingleBand(y, h, mapPsiMultiHours_Combined_Eq9_Eq12, inSoilThickness, inPhi, inGammaSoil, inCohesion, inAlfaVanGen, inNVanGen, inSlopeFixZeroValuesAndBorder, pm);              
                
                // Dump results to file
                if (doSaveByProducts) {
                    String message = "Dumping maps of HYDROLOGY-and-GEOMECHANIC driven SAFETY FACTOR to ";
                    //MapDumpingFunctions.dumpMultiBandCoverage(mapSafetyFactor_InfiniteSlope_with_SaturatedZones, (h-1), pOutFolder, pm, message);
                    MapDumpingFunctions.dumpSingleBandCoverage(mapSafetyFactor_InfiniteSlope_with_SaturatedZones_SingleBand, pOutFolder, pm, message);
                }
                // Export map references for allowing testing
                //out_SafetyFactor_InfiniteSlope_accounting_for_SaturatedZones_Raster = mapSafetyFactor_InfiniteSlope_with_SaturatedZones;
                out_SafetyFactor_InfiniteSlope_accounting_for_SaturatedZones_Raster = mapSafetyFactor_InfiniteSlope_with_SaturatedZones_SingleBand;
                
                //mapsSafetyFactor_InfSlope_with_SaturZones_LastHour[durationArrayStep] = MapCalculationFunctions.getSingleBandFromMultibandCoverage((h-1), y, mapSafetyFactor_InfiniteSlope_with_SaturatedZones);
                mapsSafetyFactor_InfSlope_with_SaturZones_LastHour[durationArrayStep] = mapSafetyFactor_InfiniteSlope_with_SaturatedZones_SingleBand;
                
            } // Step to next simulated Rainfall Duration [h]
            
            // ##################################################################################
            // Condense all computed Safety Factor maps into an overall one basing on worst-case#
            
            pm.message(" ");
            pm.message("######################################################################################");
            pm.message("######################################################################################");
            pm.errorMessage("### Merging all final SAFETY FACTOR maps computed for RETURN TIME: "+y+"y             ##");
            pm.message(" ");
            
            GridCoverage2D mapSafetyFactor_InfiniteSlope_with_SaturatedZones_RetTime_TOTAL = MapCalculationFunctions.computeSafetyFactorTOTAL(mapsSafetyFactor_InfSlope_with_SaturZones_LastHour, y, pm);
            
            MapDumpingFunctions.dumpSingleBandCoverage(mapSafetyFactor_InfiniteSlope_with_SaturatedZones_RetTime_TOTAL, pOutFolder, pm);
           
            out_SafetyFactor_InfiniteSlope_accounting_for_SaturatedZones_RetTime_TOTAL_Raster = mapSafetyFactor_InfiniteSlope_with_SaturatedZones_RetTime_TOTAL;
            
            pm.message(" ");
            pm.message("######################################################################################");
            pm.message("######################################################################################");
            pm.message(" ");
            
        } // Step to next simulated Return Time [year]
        
        // ################################################################################## 
        // ##################################################################################
        // ##################################################################################
        // ### END OF LOOPS BETWEEN Return Times AND Rainfall Durations #####################
        // ################################################################################## 
        // ##################################################################################
        // ##################################################################################
        

        
        
        if (doCalculateInitialSafetyFactor) {
            // ##################################################################################                
            // Calculation of Safety Factor with  infinite slope model and initial Psi    #######
            // 1. Initial SF (based on theta_I) ##==## theta_I is in paper 'V'
            // 
            // Equation (14a) & (14b)
            // 
            // inPsiInitAtBedrock is usually a matrix of 0.05 values covering the whole basin
            // 
            pm.message(" ");
            pm.message("######################################################################################");
            pm.message("### Compute the INITIAL SAFETY FACTOR drivend by geo-technic an hydrologic forces   ##");
            pm.message(" ");
            
            GridCoverage2D mapSafetyFactor_Initial = MapCalculationFunctions.computeSafetyFactor_Initial(inPsiInitAtBedrock, inSoilThickness, inPhi, inGammaSoil, inCohesion, inAlfaVanGen, inNVanGen, inSlopeFixZeroValuesAndBorder, pm);              
       
            // Dump results to file
            
            MapDumpingFunctions.dumpSingleBandCoverage(mapSafetyFactor_Initial, pOutFolder, pm);
            
            // Export map references for allowing testing
            out_SafetyFactor_Initial_Raster = mapSafetyFactor_Initial;
        
        }
        
        pm.message(" ");
        pm.message("######################################################################################");
        pm.message("###    !!!     ---  Cislam model completed successfully ---           !!!           ##");
        pm.message("######################################################################################");
        pm.message("######################################################################################");
        pm.message("######################################################################################");
        pm.message(" ");

        // End of maps computation
        // ##################################################################################

    }

    private GridCoverage2D getTwsMap( HashMap<Integer[], GridCoverage2D> mapsOfTwt, int year, int hour ) {

        // Cycle in the Twt map set to extract the map corresponding the current Return Time year (y) and rainfall simulation duration (h)
        Set<Integer[]> s = mapsOfTwt.keySet();
        Iterator<Integer[]> i = s.iterator();
        Integer[] key = null;
        while( i.hasNext() ) {
            Integer[] keyTmp = i.next();
            if (keyTmp[0] == hour && keyTmp[1] == year) {
                key = keyTmp;
                GridCoverage2D mapTwt = mapsOfTwt.get(key);
                return mapTwt;
            }
        }
        return null;
    }

    private GridCoverage2D flowMapMarkOutlet_NotWorking( GridCoverage2D inFlow ) {
        // #### DOES NOT WORK ######
        OmsMarkoutlets markoutlet = new OmsMarkoutlets();
        Map<String, Object> inputMap = new HashMap<String, Object>();
        inputMap.put("inFlow", inFlow);
        Map<String, Object> outputMap = markoutlet.execute(inputMap, null);
        GridCoverage2D outRaster = (GridCoverage2D) outputMap.get("outFlow");
        return outRaster;
    }

}
