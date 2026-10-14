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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
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

@Description(OmsSWYBaseflowRouting.DESCRIPTION)
@Documentation(OmsSWYBaseflowRouting.DOCUMENTATION)
@Bibliography({OmsSWYQuickflow.BIBLIOGRAPHY_INVEST_SWY, OmsSWYQuickflow.BIBLIOGRAPHY_HAMEL_ET_AL_2020})
@Author(name = OmsSWYBaseflowRouting.AUTHORNAMES, contact = OmsSWYBaseflowRouting.AUTHORCONTACTS)
@Keywords(OmsSWYBaseflowRouting.KEYWORDS)
@Label(OmsSWYBaseflowRouting.LABEL)
@Name(OmsSWYBaseflowRouting.NAME)
@Status(OmsSWYBaseflowRouting.STATUS)
@License(OmsSWYBaseflowRouting.LICENSE)
public class OmsSWYBaseflowRouting extends HMModel {
    @Description(inRecharge_DESCRIPTION)
    @In
    public GridCoverage2D inRecharge = null;

    @Description(inAvailableRecharge_DESCRIPTION)
    @In
    public GridCoverage2D inAvailableRecharge = null;

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
    public static final String DESCRIPTION = "The baseflow of the InVEST Seasonal Water Yield model: the contribution of the local recharge of each cell to the baseflow of the streams, and its share of the total recharge.";
    public static final String DOCUMENTATION = """
            The recharge of the upslope cells is accumulated along the flowdirections (L_sum). Walking up from the \
            streams, each cell gets the part of the accumulated recharge that reaches the streams as baseflow \
            (B_sum, InVEST equation 11): all of it for the cells that drain directly into a stream or out of the \
            map, a part scaled by what the downslope cells evapotranspire for the others. The baseflow of the cell \
            itself (B) is its share of B_sum, proportional to its local recharge, and never negative.

            The values index of each cell (VRI) is its local recharge divided by the total recharge of the map: \
            it tells which cells matter most for the baseflow. It is defined only where the total recharge is positive.

            The results have the time span of the recharge: with the recharge of a year, as in InVEST, the \
            baseflow is annual. The rasters are in mm; a volume is the value times 0.001 times the cell area.
            """;
    public static final String KEYWORDS = "baseflow";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "SWYBaseflowRouting";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "The klab team.";
    public static final String AUTHORCONTACTS = "www.integratedmodelling.org";

    public static final String inFlowdirections_DESCRIPTION = "The map of flowdirections (D8).";
    public static final String inNet_DESCRIPTION = "The map of the stream network.";
    public static final String outBaseflow_DESCRIPTION = "The map of the cumulative baseflow (B_sum), the part of the accumulated recharge of each cell that reaches the streams.";
    public static final String outB_DESCRIPTION = "The map of the baseflow contribution of each cell (B).";
    public static final String outVri_DESCRIPTION = "The map of the values index (VRI), the local recharge of each cell divided by the total recharge.";
    public static final String outLsum_DESCRIPTION = "The map of the accumulated recharge (L_sum), of the cell and of all the cells upslope.";
    public static final String outQb_DESCRIPTION = "The total recharge, the sum of the local recharge of all the cells, in mm: divided by the number of cells it is the mean recharge (qb of InVEST).";
    public static final String outVriSum_DESCRIPTION = "The sum of the values index of all the cells, 1 as a check.";
    public static final String inAvailableRecharge_DESCRIPTION = "The map of the part of the local recharge that is available to downslope cells (L_avail).";
    public static final String inRecharge_DESCRIPTION = "The map of the local recharge (L).";


    // VARS DOC END

    /**
     * Recharge values [mm] below this are considered zero when used as divisors.
     */
    private static final double ZERO_TOLERANCE = 1E-6;

    private double infNv;

    private double netInfNv;

    private double netNv;

    private int infNovaluesCount = 0;

    @Execute
    public void process() throws Exception {
        checkNull(inRecharge, inAvailableRecharge, inFlowdirections, inNet);

        RegionMap regionMap = RegionMap.fromGridGeometry(inFlowdirections.getGridGeometry());
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
        RandomIter netInfiltrationIter = CoverageUtilities.getRandomIterator(inAvailableRecharge);
        netInfNv = HMConstants.getNovalue(inAvailableRecharge);
        RandomIter infiltrationIter = CoverageUtilities.getRandomIterator(inRecharge);
        infNv = HMConstants.getNovalue(inRecharge);
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
                    FlowNode node = new FlowNode(flowIter, cols, rows, c, r, flowNv);

                    // get exit cells, i.e. valid cells without a valid downstream cell
                    if (node.isValid() && node.goDownstream() == null) {
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
            pm.beginTask("Calculate total baseflow...", rows);
            for( int row = 0; row < rows; row++ ) {
                for( int col = 0; col < cols; col++ ) {
                    double li = infiltrationIter.getSampleDouble(col, row, 0);
                    if (!HMConstants.isNovalue(li, infNv) && !Double.isInfinite(li)) {
                        qb += li;
                    }
                }
                pm.worked(1);
            }
            outQb = qb;
            pm.done();

            pm.beginTask("Calculate Vri...", rows);
            double vriSum = 0;
            for( int row = 0; row < rows; row++ ) {
                for( int col = 0; col < cols; col++ ) {
                    double li = infiltrationIter.getSampleDouble(col, row, 0);
                    double bSum = outBaseflowIter.getSampleDouble(col, row, 0);
                    double lSum = lSumMatrix[row][col];

                    if (!HMConstants.isNovalue(li, infNv)) {
                        // as in InVEST, vri is only defined if the basin recharge is positive
                        if (qb > 0) {
                            double vri = li / qb;
                            vriSum += vri;
                            outVriIter.setSample(col, row, 0, vri);
                        }

                        if (!HMConstants.isNovalue(bSum, outNv)) {
                            double bi = lSum != 0 ? Math.max(bSum * li / lSum, 0) : 0;
                            outBIter.setSample(col, row, 0, bi);
                        }
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
                    inFlowdirections.getCoordinateReferenceSystem(), outNv);
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

    /**
     * Build the Lsum raster, the total cumulative recharge (local + upstream)
     * that reaches each cell via shallow subsuface routing.
     * 
     * @param sourceCells
     * @param lSumMatrix
     * @param infiltrationIter
     * @param netIter
     * @param lsumNv
     * @param pm
     */
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

    /**
     * Calculate the cumulative baseflow (InVEST equation 11), walking upstream from an exit cell.
     *
     * <p>An explicit stack is used, since the recursion depth would be the length of the
     * longest flow path.</p>
     */
    private void walkUpAndProcess( FlowNode exitCell, double[][] bSumMatrix, double[][] lSumMatrix,
            WritableRandomIter baseflowIter, RandomIter netIter, RandomIter infiltrationIter, RandomIter netInfiltrationIter ) {
        Deque<FlowNode> stack = new ArrayDeque<>();
        stack.push(exitCell);
        while( !stack.isEmpty() ) {
            FlowNode cell = stack.pop();
            int x = cell.col;
            int y = cell.row;

            double bSum;
            FlowNode downCell = cell.goDownstream();
            if (downCell == null || isStream(downCell, netIter)) {
                // if it is an outlet or it drains into the stream
                bSum = lSumMatrix[y][x];
            } else {
                int downY = downCell.row;
                int downX = downCell.col;
                double downBSum = bSumMatrix[downY][downX];
                double downLSum = lSumMatrix[downY][downX];
                double downLi = valueOrZero(downCell.getDoubleValueFromMap(infiltrationIter), infNv);
                double downLAvailable = valueOrZero(downCell.getDoubleValueFromMap(netInfiltrationIter), netInfNv);

                // a tolerance is needed, since the cumulated recharge can cancel out to a value that is
                // zero only up to rounding, which would then be used as divisor
                if (Math.abs(downLSum) > ZERO_TOLERANCE && Math.abs(downLSum - downLi) > ZERO_TOLERANCE) {
                    bSum = lSumMatrix[y][x] * (1 - downLAvailable / downLSum) * downBSum / (downLSum - downLi);
                } else {
                    bSum = lSumMatrix[y][x];
                }
            }
            bSumMatrix[y][x] = bSum;
            cell.setDoubleValueInMap(baseflowIter, bSum);

            // the upstream cells can be processed now that this one is done
            for( FlowNode upCell : cell.getEnteringNodes() ) {
                stack.push(upCell);
            }
        }
    }

    private boolean isStream( FlowNode cell, RandomIter netIter ) {
        double net = cell.getDoubleValueFromMap(netIter);
        return !HMConstants.isNovalue(net, netNv) && net != 0.0;
    }

    private static double valueOrZero( double value, double novalue ) {
        if (HMConstants.isNovalue(value, novalue)) {
            return 0;
        }
        return value;
    }

}