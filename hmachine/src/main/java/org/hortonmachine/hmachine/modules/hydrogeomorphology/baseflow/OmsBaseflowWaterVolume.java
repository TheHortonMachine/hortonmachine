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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
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

    @Description(outLsum_DESCRIPTION)
    @Out
    public GridCoverage2D outLsum = null;

    @Description(outB_DESCRIPTION)
    @Out
    public GridCoverage2D outB = null;

    @Description(outVri_DESCRIPTION)
    @Out
    public GridCoverage2D outVri = null;

    @Description(outQb_DESCRIPTION)
    @Out
    public Double outQb = null;

    @Description(outVriSum_DESCRIPTION)
    @Out
    public Double outVriSum = null;

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
    public static final String outBaseflow_DESCRIPTION = "The map of cumulated baseflow.";
    public static final String outB_DESCRIPTION = "The map of single cell baseflow.";
    public static final String outVri_DESCRIPTION = "The map of contribution of local recharge in pixel to baseflow.";
    public static final String outLsum_DESCRIPTION = "The map of Lsum.";
    public static final String outQb_DESCRIPTION = "The total baseflow.";
    public static final String outVriSum_DESCRIPTION = "The Vri sum value.";
    public static final String inInf_DESCRIPTION = "The infiltrated watervolume.";
    public static final String inNetInf_DESCRIPTION = "The net infiltrated watervolume.";

    // VARS DOC END

    private double infNv;

    private double netNv;

    private int infNovaluesCount = 0;

    @Execute
    public void process() throws Exception {
        checkNull(inInfiltration, inNetInfiltration, inFlowdirections, inNet);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlowdirections);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        double outNv = HMConstants.doubleNovalue;
        double lsumNv = -1E32;

        WritableRaster outBaseflowWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outBaseflowIter = CoverageUtilities.getWritableRandomIterator(outBaseflowWR);

        WritableRaster outBWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outBIter = CoverageUtilities.getWritableRandomIterator(outBWR);
        WritableRaster outVriWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, outNv);
        WritableRandomIter outVriIter = CoverageUtilities.getWritableRandomIterator(outVriWR);

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlowdirections);
        int flowNv = HMConstants.getIntNovalue(inFlowdirections);
        RandomIter netInfiltrationIter = CoverageUtilities.getRandomIterator(inNetInfiltration);
        double netInfNv = HMConstants.getNovalue(inNetInfiltration);
        RandomIter infiltrationIter = CoverageUtilities.getRandomIterator(inInfiltration);
        infNv = HMConstants.getNovalue(inInfiltration);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);
        netNv = HMConstants.getNovalue(inNet);

        List<FlowNode> sourceCells = new ArrayList<>();
        List<FlowNode> exitCells = new ArrayList<>();
        try {
            pm.beginTask("Collect source and exit cells...", rows);
            for( int r = 0; r < rows; r++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int c = 0; c < cols; c++ ) {
                    if(r==1&&c==959) {
                        System.out.println();
                    }
                    FlowNode node = new FlowNode(flowIter, cols, rows, c, r, flowNv);

                    // get exit cells
                    if (node.isHeadingOutside()) {
                        exitCells.add(node);
                    }
                    // and source cells on network
                    if (node.isSource()) {
                        sourceCells.add(node);
                    }
                }
                pm.worked(1);
            }
            pm.done();

            // calculate matrix of cumulated infiltration
            double[][] lSumMatrix = new double[rows][cols];
            for( int row = 0; row < rows; row++ ) {
                for( int col = 0; col < cols; col++ ) {
                    lSumMatrix[row][col] = lsumNv;
                }
            }
            calculateLsumMatrix(sourceCells, lSumMatrix, infiltrationIter, netIter, lsumNv, pm);

            // calculate matrix of cumulated baseflow
            double[][] bSumMatrix = new double[rows][cols];
            pm.beginTask("Calcuate bsum...", exitCells.size());
            for( FlowNode exitCell : exitCells ) {
                if (pm.isCanceled()) {
                    return;
                }
                walkUpAndProcess(exitCell, bSumMatrix, lSumMatrix, outBaseflowIter, netIter, infiltrationIter,
                        netInfiltrationIter);
                pm.worked(1);
            }
            pm.done();

            double qb = 0;
            int count = 0;
            pm.beginTask("Calculate total baseflow...", rows);
            for( int row = 0; row < rows; row++ ) {
                for( int col = 0; col < cols; col++ ) {
                    double li = infiltrationIter.getSampleDouble(col, row, 0);
                    if (!HMConstants.isNovalue(li, infNv)) {
                        qb += li;
                        count++;
                    }
                }
                pm.worked(1);
            }
            double qbTmp = qb / count;
            outQb = qbTmp;
            pm.done();

            pm.beginTask("Calculate Vri...", rows);
            double vriSum = 0;
            for( int row = 0; row < rows; row++ ) {
                for( int col = 0; col < cols; col++ ) {
                    double li = infiltrationIter.getSampleDouble(col, row, 0);
                    double bSum = outBaseflowIter.getSampleDouble(col, row, 0);
                    double lSum = lSumMatrix[row][col];

                    if (!HMConstants.isNovalue(li, infNv)) {
                        double vri = li / (qbTmp * count);
                        vriSum += vri;
                        outVriIter.setSample(col, row, 0, vri);

                        double bi = Math.max(bSum * li / lSum, 0);
                        outBIter.setSample(col, row, 0, bi);
                    }
                }
                pm.worked(1);
            }
            pm.done();
            outVriSum = vriSum;

            outBaseflow = CoverageUtilities.buildCoverageWithNovalue("baseflow", outBaseflowWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outLsum = CoverageUtilities.buildCoverageWithNovalue("lsum", lSumMatrix, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), true, lsumNv);

            outB = CoverageUtilities.buildCoverageWithNovalue("b", outBWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
            outVri = CoverageUtilities.buildCoverageWithNovalue("vri", outVriWR, regionMap,
                    inFlowdirections.getCoordinateReferenceSystem(), lsumNv);
        } finally {
            flowIter.done();
            netInfiltrationIter.done();
            infiltrationIter.done();
            netIter.done();

            outBaseflowIter.done();
            outBIter.done();
            outVriIter.done();
        }

    }

    private void calculateLsumMatrix( List<FlowNode> sourceCells, double[][] lSumMatrix, RandomIter infiltrationIter,
            RandomIter netIter, double lsumNv, IHMProgressMonitor pm ) {

        pm.beginTask("Calculating lsum...", sourceCells.size());
        for( FlowNode sourceCell : sourceCells ) {
            double li = sourceCell.getValueFromMap(infiltrationIter);
            if (HMConstants.isNovalue(li, infNv)) {
                infNovaluesCount++;
                if (infNovaluesCount == 5) {
                    pm.errorMessage(
                            "No more debug messages printed. Just setting infiltration to 0 due to novalue in infiltration.");
                } else if (infNovaluesCount < 5) {
                    pm.errorMessage("Found novalue in infiltration map. Check your data. Setting infiltration to 0.");
                }
                li = 0;
            }
            int x = sourceCell.col;
            int y = sourceCell.row;
            lSumMatrix[y][x] = li; // no upstream contribution

            // go downstream
            Set<FlowNode> seen = new HashSet<>();
            FlowNode cell = sourceCell.goDownstream();
            while( cell != null ) {
                List<FlowNode> upstreamCells = cell.getEnteringNodes();
                // check if all upstream have a value
                boolean canProcess = canProcess(lSumMatrix, lsumNv, upstreamCells);
                if (canProcess) {
                    double currentCellLi = cell.getValueFromMap(infiltrationIter);
                    // infiltratedWaterVolumeState.get(locator,
                    // Double.class);
                    if (HMConstants.isNovalue(currentCellLi, infNv)) {
                        infNovaluesCount++;
                        if (infNovaluesCount == 5) {
                            pm.errorMessage(
                                    "No more debug messages printed. Just setting infiltration to 0 due to novalue in infiltration.");
                        } else if (infNovaluesCount < 5) {
                            pm.errorMessage("Found novalue in infiltration map. Check your data. Setting infiltration to 0.");
                        }
                        currentCellLi = 0;
                    }
                    double lSumUpstreamCells = 0.0;
                    for( FlowNode upstreamCell : upstreamCells ) {
                        int subX = upstreamCell.col;
                        int subY = upstreamCell.row;

                        lSumUpstreamCells += lSumMatrix[subY][subX];
                    }
                    double currentCellLSum = currentCellLi + lSumUpstreamCells;
                    lSumMatrix[cell.row][cell.col] = currentCellLSum;
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
            pm.worked(1);
        }
        pm.done();
    }

    private boolean canProcess( double[][] lSumMatrix, double lsumNv, List<FlowNode> upstreamCells ) {
        boolean canProcess = true;
        for( FlowNode upstreamCell : upstreamCells ) {
            double lsum = lSumMatrix[upstreamCell.row][upstreamCell.col];
            if (HMConstants.isNovalue(lsum, lsumNv)) {
                // stop, we still need the other upstream values
                canProcess = false;
                break;
            }
        }
        return canProcess;
    }

    private void walkUpAndProcess( FlowNode cell, double[][] bSumMatrix, double[][] lSumMatrix, WritableRandomIter baseflowIter,
            RandomIter netIter, RandomIter infiltrationIter, RandomIter netInfiltrationIter ) {
        // process current cell
        double net = cell.getDoubleValueFromMap(netIter);
        boolean isStream = !HMConstants.isNovalue(net, netNv) && net != 0.0;

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
            double downLi = downCell.getDoubleValueFromMap(infiltrationIter);
            double downLAvailable = downCell.getDoubleValueFromMap(netInfiltrationIter);

            if (downLSum != 0 && downLSum - downLi != 0) {
                bSum = lSumMatrix[y][x] * (1 - downLAvailable / downLSum) * downBSum / (downLSum - downLi);
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