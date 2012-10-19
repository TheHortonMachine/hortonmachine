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
package org.jgrasstools.hortonmachine.modules.geomorphology.flow;

import static org.jgrasstools.gears.libs.modules.JGTConstants.*;
import static org.jgrasstools.gears.libs.modules.Direction.*;
import static java.lang.Math.abs;

import java.awt.image.WritableRaster;
import java.util.List;
import java.util.TreeSet;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.Direction;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.GridNodeElevationToLeastComparator;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.BitMatrix;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Calculates the drainage directions following the least cost method.")
// @Documentation("FlowDirections.html")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Geomorphology, Flowdirections, Least cost")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("flowlc")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LeastCostFlowDirections extends JGTModel {
    @Description("The depitted elevation map.")
    @In
    public GridCoverage2D inElev = null;

    @Description("Flag to consider or ignore boundary pixels.")
    @In
    public boolean doExcludeBorder = false;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of flowdirections.")
    @Out
    public GridCoverage2D outFlow = null;

    private BitMatrix assignedFlowsMap;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFlow == null, doReset)) {
            return;
        }
        checkNull(inElev);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter elevationIter = CoverageUtilities.getRandomIterator(inElev);

        WritableRaster flowWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, doubleNovalue);
        WritableRandomIter flowIter = CoverageUtilities.getWritableRandomIterator(flowWR);

        TreeSet<GridNode> orderedNodes = new TreeSet<GridNode>(new GridNodeElevationToLeastComparator());
        assignedFlowsMap = new BitMatrix(cols, rows);

        pm.beginTask("Check for potential outlets...", cols);
        for( int c = 0; c < cols; c++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int r = 0; r < rows; r++ ) {
                GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, c, r);
                if (!node.isValid()) {
                    assignedFlowsMap.mark(c, r);
                    flowIter.setSample(c, r, 0, doubleNovalue);
                    continue;
                }
                if (node.touchesBound()) {
                    orderedNodes.add(node);
                    if (doExcludeBorder) {
                        assignedFlowsMap.mark(c, r);
                        flowIter.setSample(c, r, 0, doubleNovalue);
                    } else {
                        flowIter.setSample(c, r, 0, Direction.getOutletValue());
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

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
                flowIter.setSample(e.col, e.row, 0, E.getEnteringFlow());
                orderedNodes.add(e);
                assignedFlowsMap.mark(e.col, e.row);
            }
            GridNode n = surroundingNodes.get(2);
            if (nodeOk(n)) {
                flowIter.setSample(n.col, n.row, 0, N.getEnteringFlow());
                orderedNodes.add(n);
                assignedFlowsMap.mark(n.col, n.row);
            }
            GridNode w = surroundingNodes.get(4);
            if (nodeOk(w)) {
                flowIter.setSample(w.col, w.row, 0, W.getEnteringFlow());
                orderedNodes.add(w);
                assignedFlowsMap.mark(w.col, w.row);
            }
            GridNode s = surroundingNodes.get(6);
            if (nodeOk(s)) {
                flowIter.setSample(s.col, s.row, 0, S.getEnteringFlow());
                orderedNodes.add(s);
                assignedFlowsMap.mark(s.col, s.row);
            }

            /*
             * diagonal cells are processed only if they are valid and 
             * they are not steeper than their attached vertical and horiz cells.
             */
            GridNode en = surroundingNodes.get(1);
            if (nodeOk(en) && assignFlowDirection(lowestNode, en, e, n)) {
                flowIter.setSample(en.col, en.row, 0, EN.getEnteringFlow());
                orderedNodes.add(en);
                assignedFlowsMap.mark(en.col, en.row);
            }
            GridNode nw = surroundingNodes.get(3);
            if (nodeOk(nw) && assignFlowDirection(lowestNode, nw, n, w)) {
                flowIter.setSample(nw.col, nw.row, 0, NW.getEnteringFlow());
                orderedNodes.add(nw);
                assignedFlowsMap.mark(nw.col, nw.row);
            }
            GridNode ws = surroundingNodes.get(5);
            if (nodeOk(ws) && assignFlowDirection(lowestNode, ws, w, s)) {
                flowIter.setSample(ws.col, ws.row, 0, WS.getEnteringFlow());
                orderedNodes.add(ws);
                assignedFlowsMap.mark(ws.col, ws.row);
            }
            GridNode se = surroundingNodes.get(7);
            if (nodeOk(se) && assignFlowDirection(lowestNode, se, s, e)) {
                flowIter.setSample(se.col, se.row, 0, SE.getEnteringFlow());
                orderedNodes.add(se);
                assignedFlowsMap.mark(se.col, se.row);
            }
        }

        outFlow = CoverageUtilities.buildCoverage("flowdirections", flowWR, regionMap, inElev.getCoordinateReferenceSystem());
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
