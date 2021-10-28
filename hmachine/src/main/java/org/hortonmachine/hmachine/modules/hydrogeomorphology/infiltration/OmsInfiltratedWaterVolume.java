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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.infiltration;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

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

@Description(OmsInfiltratedWaterVolume.DESCRIPTION)
@Author(name = OmsInfiltratedWaterVolume.AUTHORNAMES, contact = OmsInfiltratedWaterVolume.AUTHORCONTACTS)
@Keywords(OmsInfiltratedWaterVolume.KEYWORDS)
@Label(OmsInfiltratedWaterVolume.LABEL)
@Name(OmsInfiltratedWaterVolume.NAME)
@Status(OmsInfiltratedWaterVolume.STATUS)
@License(OmsInfiltratedWaterVolume.LICENSE)
public class OmsInfiltratedWaterVolume extends HMModel {
    @Description(inPet_DESCRIPTION)
    @In
    public GridCoverage2D inPet = null;

    @Description(inRainfall_DESCRIPTION)
    @In
    public GridCoverage2D inRainfall;

    @Description(inFlowdirections_DESCRIPTION)
    @In
    public GridCoverage2D inFlowdirections = null;

    @Description(inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(inRunoff_DESCRIPTION)
    @In
    public GridCoverage2D inRunoff = null;

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
    @Out
    public GridCoverage2D outAet = null;

    @Description(outLsumAvailable_DESCRIPTION)
    @Out
    public GridCoverage2D outLsumAvailable = null;

    @Description(outNetInfiltration_DESCRIPTION)
    @Out
    public GridCoverage2D outNetInfiltration = null;

    @Description(outInfiltration_DESCRIPTION)
    @Out
    public GridCoverage2D outInfiltration = null;

    // VARS DOC START
    public static final String DESCRIPTION = "The Infiltrated Watervolume model (from INVEST).";
    public static final String DOCUMENTATION = "";
    public static final String KEYWORDS = "infiltration";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "InfiltratedWaterVolume";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "The klab team.";
    public static final String AUTHORCONTACTS = "www.integratedmodelling.org";

    public static final String inFlowdirections_DESCRIPTION = "The map of flowdirections (D8).";
    public static final String inNet_DESCRIPTION = "The map of net.";
    public static final String inRunoff_DESCRIPTION = "The map of atmospheric temperature.";
    public static final String outNetInfiltration_DESCRIPTION = "The map of net infiltration.";
    public static final String outInfiltration_DESCRIPTION = "The map of infiltration.";
    public static final String outAet_DESCRIPTION = "The map of actual evapotranspiration.";
    public static final String outLsumAvailable_DESCRIPTION = "The map of Lsum Available.";
    public static final String inRainfall_DESCRIPTION = "The rainfall volume.";
    public static final String inPet_DESCRIPTION = "The potential evapotranspired watervolume.";
    public static final String pAlpha_DESCRIPTION = "Fraction of upslope available recharge (upgradient subsidy) that is available for month m or for the selected reference interval.";
    public static final String pBeta_DESCRIPTION = "Spatial availability parameter: the fraction of the upgradient subsidy that is available for downgradient evapotranspiration, it is based on local topography and geology";
    public static final String pGamma_DESCRIPTION = "Fraction of pixel recharge that is available to downgradient pixels, represents what extent local recharge enters a local groundwater system and might be used again as oppose to entering a deeper groundwater system";

    public static final String pRainfall_UNIT = "mm";
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        checkNull(inPet, inRainfall, inFlowdirections, inNet, inRunoff);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlowdirections);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        double outNv = -9999;//-1E32;// as INVEST HMConstants.doubleNovalue;

        WritableRaster outLiWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outLiIter = CoverageUtilities.getWritableRandomIterator(outLiWR);
        WritableRaster outLiAvailableWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outLiAvailableIter = CoverageUtilities.getWritableRandomIterator(outLiAvailableWR);
        WritableRaster outAetWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outAetIter = CoverageUtilities.getWritableRandomIterator(outAetWR);

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlowdirections);
        int flowNv = HMConstants.getIntNovalue(inFlowdirections);
        RandomIter petIter = CoverageUtilities.getRandomIterator(inPet);
        double petNv = HMConstants.getNovalue(inPet);
        RandomIter runoffIter = CoverageUtilities.getRandomIterator(inRunoff);
        double runoffNv = HMConstants.getNovalue(inRunoff);
        RandomIter rainIter = CoverageUtilities.getRandomIterator(inRainfall);
        double rainNv = HMConstants.getNovalue(inRainfall);
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

            double[][] lSumAvailableMatrix = new double[rows][cols];
            for( int r = 0; r < lSumAvailableMatrix.length; r++ ) {
                for( int c = 0; c < lSumAvailableMatrix[0].length; c++ ) {
                    lSumAvailableMatrix[r][c] = outNv;
                }
            }

            for( FlowNode sourceCell : sourceCells ) {
                double pet = sourceCell.getValueFromMap(petIter);
                double runoff = sourceCell.getValueFromMap(runoffIter);
                double rain = sourceCell.getValueFromMap(rainIter);
                double net = sourceCell.getValueFromMap(netIter);

                if (!HMConstants.isNovalue(rain, rainNv) && !HMConstants.isNovalue(runoff, runoffNv)) {

                    boolean isPetNv = HMConstants.isNovalue(pet, petNv);

                    boolean isStream = !HMConstants.isNovalue(net, netNv) && net != 0.0;
                    double initialAet = 0;
                    double li = 0;
                    if (!isStream) {
                        if (isPetNv) {
                            initialAet = rain - runoff;
                        } else {
                            initialAet = Math.min(pet, rain - runoff);
                        }
                        li = rain - runoff - initialAet;
                    }
                    double lAvailable = Math.min(pGamma * li, li);

                    lSumAvailableMatrix[sourceCell.row][sourceCell.col] = 0;

                    sourceCell.setDoubleValueInMap(outLiAvailableIter, lAvailable);
                    sourceCell.setDoubleValueInMap(outLiIter, li);
                    sourceCell.setDoubleValueInMap(outAetIter, initialAet);

                    // go downstream
                    FlowNode cell = sourceCell.goDownstream();

                    Set<FlowNode> seen = new HashSet<>();

                    while( cell != null ) {
                        List<FlowNode> upstreamCells = cell.getEnteringNodes();
                        // check if all upstream have a value
                        boolean canProcess = canProcess(outNv, outLiAvailableIter, upstreamCells);
                        if (canProcess) {
                            pet = cell.getDoubleValueFromMap(petIter);
                            isPetNv = HMConstants.isNovalue(pet, petNv);
                            rain = cell.getDoubleValueFromMap(rainIter);
                            runoff = cell.getDoubleValueFromMap(runoffIter);
                            net = cell.getDoubleValueFromMap(netIter);

                            if (!HMConstants.isNovalue(rain, rainNv) && !HMConstants.isNovalue(runoff, runoffNv)) {
                                isStream = !HMConstants.isNovalue(net, netNv) && net != 0.0;

                                double lAvailableUpstream = 0.0;
                                double lSumAvailableUpstream = 0.0;
                                for( FlowNode upstreamCell : upstreamCells ) {
                                    double upstreamLAvailable = upstreamCell.getValueFromMap(outLiAvailableIter);
                                    // infiltratedWaterVolumeState.get(upstreamCell,
                                    // Double.class);
                                    int x = upstreamCell.col;
                                    int y = upstreamCell.row;

                                    lSumAvailableUpstream += lSumAvailableMatrix[y][x];
                                    lAvailableUpstream += upstreamLAvailable;
                                }
                                double lSumAvailableCurrentCell = lSumAvailableUpstream + lAvailableUpstream;

                                lSumAvailableCurrentCell /= upstreamCells.size();

                                lSumAvailableMatrix[cell.row][cell.col] = lSumAvailableCurrentCell;

                                double aetCC = 0;
                                double liCC = 0;
                                if (!isStream) {
                                    if (isPetNv) {
                                        aetCC = rain - runoff + pAlpha * pBeta * lSumAvailableCurrentCell;
                                    } else {
                                        aetCC = Math.min(pet, rain - runoff + pAlpha * pBeta * lSumAvailableCurrentCell);
                                    }
                                    liCC = rain - runoff - aetCC;
                                }
                                double lAvailableCC = Math.min(pGamma * liCC, liCC);
                                cell.setDoubleValueInMap(outLiAvailableIter, lAvailableCC);
                                cell.setDoubleValueInMap(outLiIter, liCC);
                                cell.setDoubleValueInMap(outAetIter, aetCC);


                            } else if (!HMConstants.isNovalue(net, netNv) && net != 0.0 && cell.isValid()) {
                                cell.setDoubleValueInMap(outLiAvailableIter, 0);
                                cell.setDoubleValueInMap(outLiIter, 0);
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

            outInfiltration = CoverageUtilities.buildCoverageWithNovalue("infiltration", outLiWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outNetInfiltration = CoverageUtilities.buildCoverageWithNovalue("netinfiltration", outLiAvailableWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outAet = CoverageUtilities.buildCoverageWithNovalue("aet", outAetWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outLsumAvailable = CoverageUtilities.buildCoverageWithNovalue("lsum", lSumAvailableMatrix, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), true, outNv);

        } finally {
            flowIter.done();
            petIter.done();
            runoffIter.done();
            rainIter.done();
            netIter.done();

            outLiIter.done();
            outLiAvailableIter.done();
            outAetIter.done();

        }

    }

    private boolean canProcess( double outNv, WritableRandomIter outLiAvailableIter, List<FlowNode> upstreamCells ) {
        boolean canProcess = true;
        for( FlowNode upstreamCell : upstreamCells ) {
            double upstreamLAvailable = upstreamCell.getValueFromMap(outLiAvailableIter);
            // infiltratedWaterVolumeState.get(upstreamCell,
            // Double.class);

            if (HMConstants.isNovalue(upstreamLAvailable, outNv)) {
                // stop, we still need the other upstream values
                canProcess = false;
                break;
            }
        }
        return canProcess;
    }

}