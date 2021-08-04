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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.baseflow;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

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

@Description(OmsBaseflowWaterVolume.DESCRIPTION)
@Author(name = OmsBaseflowWaterVolume.AUTHORNAMES, contact = OmsBaseflowWaterVolume.AUTHORCONTACTS)
@Keywords(OmsBaseflowWaterVolume.KEYWORDS)
@Label(OmsBaseflowWaterVolume.LABEL)
@Name(OmsBaseflowWaterVolume.NAME)
@Status(OmsBaseflowWaterVolume.STATUS)
@License(OmsBaseflowWaterVolume.LICENSE)
public class OmsBaseflowWaterVolume extends HMModel {
    @Description(inInf_DESCRIPTION)
    @In
    public GridCoverage2D inInfiltration = null;

    @Description(inNetInf_DESCRIPTION)
    @In
    public GridCoverage2D inNetInfiltration = null;

    @Description(inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(inFlowdirections_DESCRIPTION)
    @In
    public GridCoverage2D inFlowdirections = null;

    @Description(outBaseflow_DESCRIPTION)
    @Out
    public GridCoverage2D outBaseflow = null;

    // VARS DOC START
    public static final String DESCRIPTION = "The Baseflow Watervolume model (from INVEST).";
    public static final String DOCUMENTATION = "";
    public static final String KEYWORDS = "baseflow";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "BaseflowWaterVolume";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "The klab team.";
    public static final String AUTHORCONTACTS = "www.integratedmodelling.org";

    public static final String inFlowdirections_DESCRIPTION = "The map of flowdirections (D8).";
    public static final String inNet_DESCRIPTION = "The map of net.";
    public static final String outBaseflow_DESCRIPTION = "The map of infiltration.";
    public static final String inInf_DESCRIPTION = "The infiltrated watervolume.";
    public static final String inNetInf_DESCRIPTION = "The net infiltrated watervolume.";
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        checkNull(inInfiltration, inNetInfiltration, inFlowdirections, inNet);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlowdirections);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        WritableRaster outBaseflowWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, null);
        WritableRandomIter outBaseflowIter = CoverageUtilities.getWritableRandomIterator(outBaseflowWR);

        double novalue = HMConstants.getNovalue(inFlowdirections);
        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlowdirections);
        RandomIter netInfiltrationIter = CoverageUtilities.getRandomIterator(inNetInfiltration);
        RandomIter infiltrationIter = CoverageUtilities.getRandomIterator(inInfiltration);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);

        List<FlowNode> sourceCells = new ArrayList<>();
        List<FlowNode> exitCells = new ArrayList<>();
        try {
            pm.beginTask("Collect source and exit cells...", rows);
            for( int r = 0; r < rows; r++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int c = 0; c < cols; c++ ) {
                    FlowNode node = new FlowNode(flowIter, cols, rows, c, r, novalue);

                    // get exit cells
                    if (node.isHeadingOutside()) {
                        exitCells.add(node);
                    }
                    // and source cells on network
                    double net = node.getDoubleValueFromMap(netIter);
                    if (!HMConstants.isNovalue(net) && node.isSource()) {
                        sourceCells.add(node);
                    }
                }
                pm.worked(1);
            }
            pm.done();

            // calculate matrix of cumulated infiltration
            double[][] lSumMatrix = new double[rows][cols];
            calculateLsumMatrix(sourceCells, lSumMatrix, infiltrationIter);

            // calculate matrix of cumulated baseflow
            double[][] bSumMatrix = new double[rows][cols];
            for( FlowNode exitCell : exitCells ) {
                walkUpAndProcess(exitCell, bSumMatrix, lSumMatrix, outBaseflowIter, netIter, infiltrationIter,
                        netInfiltrationIter);
            }

            outBaseflow = CoverageUtilities.buildCoverage("baseflow", outBaseflowWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem());
        } finally {
            flowIter.done();
            netInfiltrationIter.done();
            infiltrationIter.done();
            netIter.done();

            outBaseflowIter.done();
        }

    }

    private void calculateLsumMatrix( List<FlowNode> sourceCells, double[][] lSumMatrix, RandomIter infiltrationIter ) {
        for( FlowNode sourceCell : sourceCells ) {
            double li = sourceCell.getDoubleValueFromMap(infiltrationIter);
            int x = sourceCell.col;
            int y = sourceCell.row;
            lSumMatrix[y][x] = li; // no upstream contribution

            // go downstream
            FlowNode downCell = sourceCell.goDownstream();
            while( downCell != null ) {
                FlowNode cell = downCell;
                List<FlowNode> upstreamCells = cell.getEnteringNodes();
                // check if all upstream have a value
                boolean canProcess = true;
                for( FlowNode upstreamCell : upstreamCells ) {
                    double upstreamLi = upstreamCell.getDoubleValueFromMap(infiltrationIter);
                    if (HMConstants.isNovalue(upstreamLi)) {
                        // stop, we still need the other upstream values
                        canProcess = false;
                        break;
                    }
                }

                if (canProcess) {

                    // TODO check this line. Is there really sourceCell? Or downCell?
                    double currentCellLi = sourceCell.getDoubleValueFromMap(infiltrationIter);// infiltratedWaterVolumeState.get(locator,
                                                                                              // Double.class);
                    if (!HMConstants.isNovalue(currentCellLi)) {
                        double lSumCurrentCell = 0.0;
                        for( FlowNode upstreamCell : upstreamCells ) {
                            int subX = upstreamCell.col;
                            int subY = upstreamCell.row;

                            lSumCurrentCell += lSumMatrix[subY][subX];
                        }
                        lSumMatrix[y][x] = currentCellLi + lSumCurrentCell;
                    }
                    downCell = cell.goDownstream();
                } else {
                    break;
                }
            }
        }
    }

    private void walkUpAndProcess( FlowNode cell, double[][] bSumMatrix, double[][] lSumMatrix, WritableRandomIter baseflowIter,
            RandomIter netIter, RandomIter infiltrationIter, RandomIter netInfiltrationIter ) {
        // process current cell
        double net = cell.getDoubleValueFromMap(netIter);
        boolean isStream = !HMConstants.isNovalue(net);

        double bSum = 0.0;
        int x = cell.col;
        int y = cell.row;

        FlowNode downCell = cell.goDownstream();
        if (downCell == null || isStream) {
            // is it is and outlet or on the stream
            bSum = lSumMatrix[y][x];
        } else {
            int downY = downCell.row;
            int downX = downCell.col;
            double downBSum = bSumMatrix[downY][downX];
            double downLSum = lSumMatrix[downY][downX];
            Double downLi = downCell.getDoubleValueFromMap(infiltrationIter);
            Double downLAvailable = downCell.getDoubleValueFromMap(netInfiltrationIter);

            if (downLSum != 0 && downLSum - downLi != 0) {
                bSum = lSumMatrix[y][x] * (1 - (downLAvailable / downLSum) * downBSum / (downLSum - downLi));
            } else {
                bSum = lSumMatrix[y][x];
            }
        }
        bSumMatrix[y][x] = bSum;
        cell.setDoubleValueInMap(baseflowIter, bSum);

        // recursively move to upstream cells
        List<FlowNode> upstreamCells = cell.getEnteringNodes();
        for( FlowNode upCell : upstreamCells ) {
            walkUpAndProcess(upCell, bSumMatrix, lSumMatrix, baseflowIter, netIter, infiltrationIter, netInfiltrationIter);
        }
    }

}