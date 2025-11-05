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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.swy;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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
import oms3.annotations.Unit;

@Description(OmsSWYRechargeRouting.DESCRIPTION)
@Author(name = OmsSWYRechargeRouting.AUTHORNAMES, contact = OmsSWYRechargeRouting.AUTHORCONTACTS)
@Keywords(OmsSWYRechargeRouting.KEYWORDS)
@Label(OmsSWYRechargeRouting.LABEL)
@Name(OmsSWYRechargeRouting.NAME)
@Status(OmsSWYRechargeRouting.STATUS)
@License(OmsSWYRechargeRouting.LICENSE)
public class OmsSWYRechargeRouting extends HMModel {
    @Description(inPet_DESCRIPTION)
    @Unit("mm")
    @In
    public GridCoverage2D inPet = null;

    @Description(inPrecipitation_DESCRIPTION)
    @Unit("mm")
    @In
    public GridCoverage2D inPrecipitation;

    @Description(inFlowdirections_DESCRIPTION)
    @In
    public GridCoverage2D inFlowdirections = null;

    @Description(inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(inQuickflow_DESCRIPTION)
    @Unit("mm")
    @In
    public GridCoverage2D inQuickflow = null;

    @Description(pAlpha_DESCRIPTION)
    @In
    public double pAlpha = 1.0;

    @Description(pBeta_DESCRIPTION)
    @In
    public double pBeta = 1.0;

    @Description(pGamma_DESCRIPTION)
    @In
    public double pGamma = 1.0;

    @Description(outAet_DESCRIPTION)
    @Unit("mm")
    @Out
    public GridCoverage2D outAet = null;

    @Description(outRecharge_DESCRIPTION)
    @Unit("mm")
    @Out
    /**
     * Local recharge per cell: what’s left from rainfall after AET and quickflow (R = P - AET - QF).
     * 
     * INVEST's L, i.e. L in cell i and month m
     */
    public GridCoverage2D outRecharge = null;

    @Description(outUpslopeSubsidy_DESCRIPTION)
    @Unit("mm")
    @Out
    /**
	 * Cumulative recharge from all upstream cells that drains into this cell (i.e. flow accumulation of R).
	 * 
	 * INVEST's Lsum, i.e. L_sum in cell i and month m
	 */
    public GridCoverage2D outUpslopeSubsidy = null;

    @Description(outAvailableRecharge_DESCRIPTION)
    @Unit("mm")
    @Out
    /**
	 * Local + upstream recharge actually available to that cell — i.e. R_avail = R + L_sum.
	 * 
	 * INVEST's L_avail, i.e. L_avail in cell i and month m
	 */
    public GridCoverage2D outAvailableRecharge = null;


    // VARS DOC START
    public static final String DESCRIPTION = "Implements recharge and evapotranspiration routing from the InVEST Seasonal Water Yield (SWY) model.";
    public static final String DOCUMENTATION = "";
    public static final String KEYWORDS = "recharge";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "SWYRechargeRouting";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "The klab team.";
    public static final String AUTHORCONTACTS = "www.integratedmodelling.org";

    public static final String inFlowdirections_DESCRIPTION = "The map of flowdirections (D8).";
    public static final String inNet_DESCRIPTION = "The map of the stream.";
    public static final String inQuickflow_DESCRIPTION = "The map of superficial quickflow.";
    public static final String outAvailableRecharge_DESCRIPTION = "The map of routed, cumulative recharge from upstream cells (the “upslope subsidy”) that is available for baseflow generation.";
    public static final String outRecharge_DESCRIPTION = "The map of local recharge remaining after AET and quickflow are removed from precipitation.";
    public static final String outAet_DESCRIPTION = "The map of actual evapotranspiration.";
    public static final String outUpslopeSubsidy_DESCRIPTION = "The map of the upslope subsidy (the routed cumulative recharge, Lsum).";
    public static final String inPrecipitation_DESCRIPTION = "The total precipitation in the month.";
    public static final String inPet_DESCRIPTION = "The potential evapotranspired watervolume.";
    public static final String pAlpha_DESCRIPTION = "Fraction of upslope available recharge (upgradient subsidy) that is available for month m or for the selected reference interval.";
    public static final String pBeta_DESCRIPTION = "Spatial availability parameter: the fraction of the upgradient subsidy that is available for downgradient evapotranspiration, it is based on local topography and geology";
    public static final String pGamma_DESCRIPTION = "Fraction of pixel recharge that is available to downgradient pixels, represents what extent local recharge enters a local groundwater system and might be used again as oppose to entering a deeper groundwater system";

    public static final String pRainfall_UNIT = "mm";
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        /*
         * Implements recharge and evapotranspiration routing from the InVEST Seasonal Water Yield (SWY) model.
         * Computes actual evapotranspiration (AET), local recharge, routed available recharge, and cumulative upslope subsidy."
         */
        checkNull(inPet, inPrecipitation, inFlowdirections, inNet, inQuickflow);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlowdirections);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        double outNv = -9999;//-1E32;// as INVEST HMConstants.doubleNovalue;

        WritableRaster outL_WR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outL_Iter = CoverageUtilities.getWritableRandomIterator(outL_WR);
        WritableRaster outL_Available_WR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outL_Available_Iter = CoverageUtilities.getWritableRandomIterator(outL_Available_WR);
        WritableRaster outAetWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outAetIter = CoverageUtilities.getWritableRandomIterator(outAetWR);

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlowdirections);
        int flowNv = HMConstants.getIntNovalue(inFlowdirections);
        RandomIter petIter = CoverageUtilities.getRandomIterator(inPet);
        double petNv = HMConstants.getNovalue(inPet);
        RandomIter quickflowIter = CoverageUtilities.getRandomIterator(inQuickflow);
        double runoffNv = HMConstants.getNovalue(inQuickflow);
        RandomIter rainIter = CoverageUtilities.getRandomIterator(inPrecipitation);
        double rainNv = HMConstants.getNovalue(inPrecipitation);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);
        double netNv = HMConstants.getNovalue(inNet);

        List<FlowNode> sourceCells = new ArrayList<>();
        try {
            pm.beginTask("Collect source cells...", rows);
            for( int r = 0; r < rows; r++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int c = 0; c < cols; c++ ) {
                    FlowNode node = new FlowNode(flowIter, cols, rows, c, r, flowNv);
                    if (node.isSource()) {
                        sourceCells.add(node);
                    }
                }
                pm.worked(1);
            }
            pm.done();

            double[][] lSum_Matrix = new double[rows][cols];
            for( int r = 0; r < lSum_Matrix.length; r++ ) {
                for( int c = 0; c < lSum_Matrix[0].length; c++ ) {
                    lSum_Matrix[r][c] = outNv;
                }
            }

            for( FlowNode sourceCell : sourceCells ) {
                double pet = sourceCell.getValueFromMap(petIter);
                double quickflow = sourceCell.getValueFromMap(quickflowIter);
                double rain = sourceCell.getValueFromMap(rainIter);
                double net = sourceCell.getValueFromMap(netIter);

                if (!HMConstants.isNovalue(rain, rainNv) && !HMConstants.isNovalue(quickflow, runoffNv)) {

                    boolean isPetNv = HMConstants.isNovalue(pet, petNv);

                    boolean isStream = !HMConstants.isNovalue(net, netNv) && net != 0.0;
                    double initialAet = 0;

                    /*
                     * Calculate aet, recharge and available recharge 
                     * for the source cell.
                     */
                    double l = 0;
                    if (!isStream) {
                        if (isPetNv) {
                            initialAet = rain - quickflow;
                        } else {
                            initialAet = Math.min(pet, rain - quickflow);
                        }                        
                        l = rain - quickflow - initialAet;
                    }
                    double lAvailable = Math.min(pGamma * l, l);

                    lSum_Matrix[sourceCell.row][sourceCell.col] = 0;

                    sourceCell.setDoubleValueInMap(outL_Available_Iter, lAvailable);
                    sourceCell.setDoubleValueInMap(outL_Iter, l);
                    sourceCell.setDoubleValueInMap(outAetIter, initialAet);

                    // go downstream
                    FlowNode cell = sourceCell.goDownstream();

                    Set<FlowNode> seen = new HashSet<>();

                    while( cell != null ) {
                        List<FlowNode> upstreamCells = cell.getEnteringNodes();
                        // check if all upstream have a value
                        boolean canProcess = canProcess(outNv, outL_Available_Iter, upstreamCells);
                        if (canProcess) {
                            pet = cell.getDoubleValueFromMap(petIter);
                            isPetNv = HMConstants.isNovalue(pet, petNv);
                            rain = cell.getDoubleValueFromMap(rainIter);
                            quickflow = cell.getDoubleValueFromMap(quickflowIter);
                            net = cell.getDoubleValueFromMap(netIter);
                            isStream = !HMConstants.isNovalue(net, netNv) && net != 0.0;

                            if (!HMConstants.isNovalue(rain, rainNv) && !HMConstants.isNovalue(quickflow, runoffNv)) {

                                double lAvailableUpstream = 0.0;
                                double lSumAvailableUpstream = 0.0;
                                for( FlowNode upstreamCell : upstreamCells ) {
                                    double upstreamLAvailable = upstreamCell.getValueFromMap(outL_Available_Iter);
                                    // infiltratedWaterVolumeState.get(upstreamCell,
                                    // Double.class);
                                    int x = upstreamCell.col;
                                    int y = upstreamCell.row;
                                    
                                    if (HMConstants.isNovalue(lSum_Matrix[y][x], outNv)) {
										pm.errorMessage("Logic error: upstream Lsum is novalue.");
									}

                                    lSumAvailableUpstream += lSum_Matrix[y][x];
                                    lAvailableUpstream += upstreamLAvailable;
                                }
                                double lSumAvailableCurrentCell = lSumAvailableUpstream + lAvailableUpstream;

                                lSumAvailableCurrentCell /= upstreamCells.size();

                                lSum_Matrix[cell.row][cell.col] = lSumAvailableCurrentCell;

                                double aetCC = 0;
                                double lCC = 0;
                                if (!isStream) {
                                    // compute potential AET including upslope subsidy
                                    double potentialAet = rain - quickflow + pAlpha * pBeta * lSumAvailableCurrentCell;
                                    // if pet is novalue use potentialAet, otherwise use the minimum of pet and potentialAet
                                    aetCC = isPetNv 
                                    		? potentialAet 
                                    		: Math.min(pet, potentialAet);
                                    lCC = rain - quickflow - aetCC;
                                } else {
                                	// stream pixels can evapotranspire using the upslope subsidy even though their local
                                	// rainfall-quickflow balance is 0
                                	aetCC = isPetNv 
                                			? pAlpha * pBeta * lSumAvailableCurrentCell 
                        					: Math.min(pet, pAlpha * pBeta * lSumAvailableCurrentCell);
                                }
                                double lAvailableCC = Math.min(pGamma * lCC, lCC);
                                cell.setDoubleValueInMap(outL_Available_Iter, lAvailableCC);
                                cell.setDoubleValueInMap(outL_Iter, lCC);
                                cell.setDoubleValueInMap(outAetIter, aetCC);
                            } else if (isStream && cell.isValid()) {
                                cell.setDoubleValueInMap(outL_Available_Iter, 0);
                                cell.setDoubleValueInMap(outL_Iter, 0);
                                cell.setDoubleValueInMap(outAetIter, 0);
                            }

                            cell = cell.goDownstream();

                            if (cell != null) {
                                if (seen.contains(cell)) {
                                    cell = null;
                                } else {
                                    seen.add(cell);
                                }
                            }

                        } else {
                            break;
                        }
                    }

                }

            }

            outRecharge = CoverageUtilities.buildCoverageWithNovalue("recharge", outL_WR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outAvailableRecharge = CoverageUtilities.buildCoverageWithNovalue("availablerecharge", outL_Available_WR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outAet = CoverageUtilities.buildCoverageWithNovalue("aet", outAetWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outUpslopeSubsidy = CoverageUtilities.buildCoverageWithNovalue("lsum", lSum_Matrix, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), true, outNv);

        } finally {
            flowIter.done();
            petIter.done();
            quickflowIter.done();
            rainIter.done();
            netIter.done();

            outL_Iter.done();
            outL_Available_Iter.done();
            outAetIter.done();

        }

    }

    private boolean canProcess( double outNv, WritableRandomIter outLiAvailableIter, List<FlowNode> upstreamCells ) {
        boolean canProcess = true;
        for( FlowNode upstreamCell : upstreamCells ) {
            double upstreamLAvailable = upstreamCell.getValueFromMap(outLiAvailableIter);

            if (HMConstants.isNovalue(upstreamLAvailable, outNv)) {
                // stop, we still need the other upstream values
                canProcess = false;
                break;
            }
        }
        return canProcess;
    }

}