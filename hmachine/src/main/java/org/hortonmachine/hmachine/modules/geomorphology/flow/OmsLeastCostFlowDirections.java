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
package org.hortonmachine.hmachine.modules.geomorphology.flow;

import static java.lang.Math.abs;
import static org.hortonmachine.gears.libs.modules.Direction.E;
import static org.hortonmachine.gears.libs.modules.Direction.EN;
import static org.hortonmachine.gears.libs.modules.Direction.N;
import static org.hortonmachine.gears.libs.modules.Direction.NW;
import static org.hortonmachine.gears.libs.modules.Direction.S;
import static org.hortonmachine.gears.libs.modules.Direction.SE;
import static org.hortonmachine.gears.libs.modules.Direction.W;
import static org.hortonmachine.gears.libs.modules.Direction.WS;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_DOCUMENTATION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_doAspect_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_doSlope_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_doTca_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_outAspect_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_outFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_outSlope_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_outTca_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.List;
import java.util.TreeSet;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.GridNodeElevationToLeastComparator;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.BitMatrix;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.aspect.OmsAspect;
import org.hortonmachine.hmachine.modules.geomorphology.slope.OmsSlope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OMSLEASTCOSTFLOWDIRECTIONS_DESCRIPTION)
@Documentation(OMSLEASTCOSTFLOWDIRECTIONS_DOCUMENTATION)
@Author(name = OMSLEASTCOSTFLOWDIRECTIONS_AUTHORNAMES, contact = OMSLEASTCOSTFLOWDIRECTIONS_AUTHORCONTACTS)
@Keywords(OMSLEASTCOSTFLOWDIRECTIONS_KEYWORDS)
@Label(OMSLEASTCOSTFLOWDIRECTIONS_LABEL)
@Name(OMSLEASTCOSTFLOWDIRECTIONS_NAME)
@Status(OMSLEASTCOSTFLOWDIRECTIONS_STATUS)
@License(OMSLEASTCOSTFLOWDIRECTIONS_LICENSE)
public class OmsLeastCostFlowDirections extends HMModel {
    @Description(OMSLEASTCOSTFLOWDIRECTIONS_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_doTca_DESCRIPTION)
    @In
    public boolean doTca = true;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_doSlope_DESCRIPTION)
    @In
    public boolean doSlope = true;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_doAspect_DESCRIPTION)
    @In
    public boolean doAspect = true;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_outFlow_DESCRIPTION)
    @Out
    public GridCoverage2D outFlow = null;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_outTca_DESCRIPTION)
    @Out
    public GridCoverage2D outTca = null;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_outAspect_DESCRIPTION)
    @Out
    public GridCoverage2D outAspect = null;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_outSlope_DESCRIPTION)
    @Out
    public GridCoverage2D outSlope = null;

    private BitMatrix assignedFlowsMap;

    private WritableRandomIter flowIter;

    private TreeSet<GridNode> orderedNodes;

    private WritableRandomIter tcaIter;
    private WritableRandomIter slopeIter;
    private WritableRandomIter aspectIter;

    private int cols;

    private int rows;

    private boolean doExcludeBorder = true;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFlow == null, doReset)) {
            return;
        }
        checkNull(inElev);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        cols = regionMap.getCols();
        rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter elevationIter = CoverageUtilities.getRandomIterator(inElev);

        WritableRaster flowWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, doubleNovalue);
        flowIter = CoverageUtilities.getWritableRandomIterator(flowWR);

        WritableRaster tcaWR = null;
        if (doTca) {
            tcaWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, doubleNovalue);
            tcaIter = CoverageUtilities.getWritableRandomIterator(tcaWR);
        }

        WritableRaster slopeWR = null;
        if (doSlope) {
            slopeWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, doubleNovalue);
            slopeIter = CoverageUtilities.getWritableRandomIterator(slopeWR);
        }

        WritableRaster aspectWR = null;
        if (doAspect) {
            aspectWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, doubleNovalue);
            aspectIter = CoverageUtilities.getWritableRandomIterator(aspectWR);
        }

        orderedNodes = new TreeSet<GridNode>(new GridNodeElevationToLeastComparator());
        assignedFlowsMap = new BitMatrix(cols, rows);

        double novalue = HMConstants.getNovalue(inElev);

        pm.beginTask("Check for potential outlets...", rows);
        int nonValidCellsNum = 0;
        for( int r = 0; r < rows; r++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int c = 0; c < cols; c++ ) {
                GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, c, r, novalue);
                if (!node.isValid()) {
                    nonValidCellsNum++;
                    assignedFlowsMap.mark(c, r);
                    continue;
                }
                if (node.touchesBound()) {
                    orderedNodes.add(node);
                    if (doExcludeBorder) {
                        assignedFlowsMap.mark(c, r);
                    } else {
                        flowIter.setSample(c, r, 0, Direction.getOutletValue());
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        pm.beginTask("Extract flowdirections...", (rows * cols - nonValidCellsNum));
        GridNode lowestNode = null;
        while( (lowestNode = orderedNodes.pollFirst()) != null ) {
            /*
             * set the current cell as marked. If it is an alone one,
             * it will stay put as an outlet (if we do not mark it, it 
             * might get overwritten. Else il will be redundantly set 
             * later again.
             */
            assignedFlowsMap.mark(lowestNode.col, lowestNode.row);

            List<GridNode> surroundingNodes = lowestNode.getSurroundingNodes();

            /*
             * vertical and horiz cells, if they exist, are 
             * set to flow inside the current cell and added to the 
             * list of cells to process.
             */
            GridNode e = surroundingNodes.get(0);
            if (nodeOk(e)) {
                // flow in current and get added to the list of nodes to process by elevation
                // order
                setNodeValues(e, E.getEnteringFlow());
            }
            GridNode n = surroundingNodes.get(2);
            if (nodeOk(n)) {
                setNodeValues(n, N.getEnteringFlow());
            }
            GridNode w = surroundingNodes.get(4);
            if (nodeOk(w)) {
                setNodeValues(w, W.getEnteringFlow());
            }
            GridNode s = surroundingNodes.get(6);
            if (nodeOk(s)) {
                setNodeValues(s, S.getEnteringFlow());
            }

            /*
             * diagonal cells are processed only if they are valid and 
             * they are not steeper than their attached vertical and horiz cells.
             */
            GridNode en = surroundingNodes.get(1);
            if (nodeOk(en) && assignFlowDirection(lowestNode, en, e, n)) {
                setNodeValues(en, EN.getEnteringFlow());
            }
            GridNode nw = surroundingNodes.get(3);
            if (nodeOk(nw) && assignFlowDirection(lowestNode, nw, n, w)) {
                setNodeValues(nw, NW.getEnteringFlow());
            }
            GridNode ws = surroundingNodes.get(5);
            if (nodeOk(ws) && assignFlowDirection(lowestNode, ws, w, s)) {
                setNodeValues(ws, WS.getEnteringFlow());
            }
            GridNode se = surroundingNodes.get(7);
            if (nodeOk(se) && assignFlowDirection(lowestNode, se, s, e)) {
                setNodeValues(se, SE.getEnteringFlow());
            }
        }
        pm.done();

        CoordinateReferenceSystem crs = inElev.getCoordinateReferenceSystem();
        outFlow = CoverageUtilities.buildCoverageWithNovalue("flowdirections", flowWR, regionMap, crs, doubleNovalue);
        if (doTca)
            outTca = CoverageUtilities.buildCoverageWithNovalue("tca", tcaWR, regionMap, crs, doubleNovalue);
        if (doSlope)
            outSlope = CoverageUtilities.buildCoverageWithNovalue("slope", slopeWR, regionMap, crs, doubleNovalue);
        if (doAspect)
            outAspect = CoverageUtilities.buildCoverageWithNovalue("aspect", aspectWR, regionMap, crs, doubleNovalue);
    }

    private void setNodeValues( GridNode node, int enteringFlow ) {
        int col = node.col;
        int row = node.row;
        flowIter.setSample(col, row, 0, enteringFlow);
        pm.worked(1);

        orderedNodes.add(node);
        assignedFlowsMap.mark(col, row);

        if (doSlope) {
            double slope = OmsSlope.calculateSlope(node, enteringFlow);
            if (slope <= 0.0) {
                // put smallest possible slope
                slope = Double.MIN_VALUE;
            }
            slopeIter.setSample(col, row, 0, slope);
        }
        if (doAspect) {
            double aspect = OmsAspect.calculateAspect(node, 1.0, false);
            aspectIter.setSample(col, row, 0, aspect);
        }

        /*
         * once a flow value is set, if tca is meant to be calculated,
         * the flow has to be followed downstream adding the contributing cell.
         */
        if (doTca) {
            int runningCol = col;
            int runningRow = row;
            while( isInRaster(runningCol, runningRow) ) {
                double tmpFlow = flowIter.getSampleDouble(runningCol, runningRow, 0);
                if (!isNovalue(tmpFlow)) {
                    double tmpTca = tcaIter.getSampleDouble(runningCol, runningRow, 0);
                    if (isNovalue(tmpTca)) {
                        tmpTca = 0.0;
                    }
                    tcaIter.setSample(runningCol, runningRow, 0, tmpTca + 1.0);
                    Direction flowDir = Direction.forFlow((int) tmpFlow);
                    if (flowDir != null) {
                        runningCol = runningCol + flowDir.col;
                        runningRow = runningRow + flowDir.row;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
    }

    private boolean isInRaster( int col, int row ) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the path from the current to the first node is steeper than to the others.
     * 
     * @param current the current node.
     * @param diagonal the diagonal node to check.
     * @param node1 the first other node to check.
     * @param node2 the second other node to check.
     * @return <code>true</code> if the path to the first node is steeper in module than 
     *         that to the others.
     */
    private boolean assignFlowDirection( GridNode current, GridNode diagonal, GridNode node1, GridNode node2 ) {
        double diagonalSlope = abs(current.getSlopeTo(diagonal));
        if (node1 != null) {
            double tmpSlope = abs(diagonal.getSlopeTo(node1));
            if (diagonalSlope < tmpSlope) {
                return false;
            }
        }
        if (node2 != null) {
            double tmpSlope = abs(diagonal.getSlopeTo(node2));
            if (diagonalSlope < tmpSlope) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the node is ok.
     * 
     * <p>A node is ok if:</p>
     * <ul>
     *  <li>if the node is valid (!= null in surrounding)</li>
     *  <li>if the node has not been processed already (!.isMarked)</li>
     * </ul> 
     */
    private boolean nodeOk( GridNode node ) {
        return node != null && !assignedFlowsMap.isMarked(node.col, node.row);
    }

}
