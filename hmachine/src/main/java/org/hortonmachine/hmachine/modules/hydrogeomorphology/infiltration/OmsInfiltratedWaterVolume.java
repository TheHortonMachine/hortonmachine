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

        WritableRaster outNetInfWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, null);
        WritableRandomIter outNetInfIter = CoverageUtilities.getWritableRandomIterator(outNetInfWR);
        WritableRaster outInfWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, null);
        WritableRandomIter outInfIter = CoverageUtilities.getWritableRandomIterator(outInfWR);

        double novalue = HMConstants.getNovalue(inFlowdirections);

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlowdirections);
        RandomIter petIter = CoverageUtilities.getRandomIterator(inPet);
        RandomIter runoffIter = CoverageUtilities.getRandomIterator(inRunoff);
        RandomIter rainIter = CoverageUtilities.getRandomIterator(inRainfall);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);

        List<FlowNode> sourceCells = new ArrayList<>();
        try {
            pm.beginTask("Collect source cells...", rows);
            for( int r = 0; r < rows; r++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int c = 0; c < cols; c++ ) {
                    FlowNode node = new FlowNode(flowIter, cols, rows, c, r, novalue);
                    if (node.isSource()) {
                        sourceCells.add(node);
                    }
                }
                pm.worked(1);
            }
            pm.done();

            double[][] lSumAvailableMatrix = new double[rows][cols];

            for( FlowNode flowNode : sourceCells ) {

                double lSumAvailable = 0.0;

                double pet = flowNode.getValueFromMap(petIter);
                double runoff = flowNode.getValueFromMap(runoffIter);
                double rain = flowNode.getValueFromMap(rainIter);
                double net = flowNode.getValueFromMap(netIter);
                lSumAvailableMatrix[flowNode.row][flowNode.col] = lSumAvailable;

                if (!HMConstants.isNovalue(pet) && !HMConstants.isNovalue(rain) && !HMConstants.isNovalue(runoff)) {

                    double aet = 0;
                    if (HMConstants.isNovalue(net)) {
                        aet = Math.min(pet, rain - runoff + pAlpha * pBeta * lSumAvailable);
                    }
                    double li = rain - runoff - aet;
                    double lAvailable = Math.min(pGamma * li, li); // TODO Silli check

                    flowNode.setDoubleValueInMap(outInfIter, lAvailable);
                    flowNode.setDoubleValueInMap(outNetInfIter, li);

                    // go downstream
                    FlowNode cell = flowNode.goDownstream();

                    Set<FlowNode> seen = new HashSet<>();

                    while( cell != null ) {

                        List<FlowNode> upstreamCells = cell.getEnteringNodes();
                        // check if all upstream have a value
                        boolean canProcess = true;
                        for( FlowNode upstreamCell : upstreamCells ) {
                            double upstreamLAvailable = upstreamCell.getDoubleValueFromMap(netIter);// infiltratedWaterVolumeState.get(upstreamCell,
                                                                                                    // Double.class);
                            if (HMConstants.isNovalue(upstreamLAvailable)) {
                                // stop, we still need the other upstream values
                                canProcess = false;
                                break;
                            }
                        }

                        if (canProcess) {
                            pet = cell.getDoubleValueFromMap(petIter);
                            rain = cell.getDoubleValueFromMap(rainIter);
                            runoff = cell.getDoubleValueFromMap(runoffIter);
                            net = cell.getDoubleValueFromMap(netIter);

                            if (!HMConstants.isNovalue(pet) && !HMConstants.isNovalue(rain) && !HMConstants.isNovalue(runoff)) {

                                double lAvailableUpstream = 0.0;
                                double lSumAvailableUpstream = 0.0;
                                for( FlowNode upstreamCell : upstreamCells ) {
                                    double upstreamLAvailable = upstreamCell.getDoubleValueFromMap(netIter);// infiltratedWaterVolumeState.get(upstreamCell,
                                                                                                            // Double.class);
                                    int x = upstreamCell.col;
                                    int y = upstreamCell.row;

                                    lSumAvailableUpstream += lSumAvailableMatrix[y][x];
                                    lAvailableUpstream += upstreamLAvailable;
                                }
                                double lSumAvailableCurrentCell = lSumAvailableUpstream + lAvailableUpstream;

                                lSumAvailableMatrix[cell.row][cell.col] = lSumAvailableCurrentCell;

                                double aetCC = 0;
                                if (HMConstants.isNovalue(net)) {
                                    aetCC = Math.min(pet, rain - runoff + pAlpha * pBeta * lSumAvailableCurrentCell);
                                }
                                double liCC = rain - runoff - aetCC;
                                double lAvailableCC = Math.min(pGamma * liCC, liCC); // TODO Silli
                                                                                     // check
                                cell.setDoubleValueInMap(outInfIter, lAvailableCC);
                                cell.setDoubleValueInMap(outNetInfIter, liCC);

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

            outInfiltration = CoverageUtilities.buildCoverage("infiltration", outInfWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem());
            outNetInfiltration = CoverageUtilities.buildCoverage("netinfiltration", outNetInfWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem());

        } finally {
            flowIter.done();
            petIter.done();
            runoffIter.done();
            rainIter.done();
            netIter.done();

            outNetInfIter.done();
            outInfIter.done();

        }

    }

}