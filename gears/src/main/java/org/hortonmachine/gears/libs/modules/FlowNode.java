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
package org.hortonmachine.gears.libs.modules;

import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

import org.hortonmachine.gears.utils.math.NumericsUtilities;

/**
 * A node in the grid environment of a digital elevation model. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.6
 */
public class FlowNode extends Node {
    /**
     * The outlet value of flow.
     */
    public static final int OUTLET = 10;

    /**
     * The defaut value used for marking a network. 
     */
    public static final int NETVALUE = 2;

    public final int flow;

    private boolean isMarkedAsOutlet = false;
    private boolean isHeadingOutside = false;
    private boolean wasHeadingOutsideChecked = false;
    private int eFlow;
    private int enFlow;
    private int nFlow;
    private int nwFlow;
    private int wFlow;
    private int wsFlow;
    private int sFlow;
    private int seFlow;

    private List<FlowNode> enteringNodes;
    private double flowD;

    /**
     * The constructor.
     * 
     * @param flowIter the flow raster iterator.
     * @param cols the cols of the raster.
     * @param rows the rows of the raster.
     * @param col the col of the current {@link FlowNode node}.
     * @param row the row of the current {@link FlowNode node}.
     */
    public FlowNode( RandomIter flowIter, int cols, int rows, int col, int row, Integer novalue ) {
        super(flowIter, cols, rows, col, row, novalue.doubleValue());

        if (!isInRaster(col, row)) {
            isValid = false;
            flow = intNovalue;
        } else {
            // TODO remove the two sync blocks if possible
            synchronized (gridIter) {
                flowD = gridIter.getSampleDouble(col, row, 0);
            }
            if (HMConstants.isNovalue(flowD)) {
                isValid = false;
            } else {
                isValid = true;
            }
            flow = (int) flowD;
        }

        if (flow == OUTLET) {
            isMarkedAsOutlet = true;
        }

        int index = -1;
        for( int c = -1; c <= 1; c++ ) {
            for( int r = -1; r <= 1; r++ ) {
                index++;
                if (c == 0 && r == 0) {
                    continue;
                }
                int newC = col + c;
                int newR = row + r;
                int tmp = intNovalue;
                if (!isInRaster(newC, newR)) {
                    touchesBound = true;
                } else {
                    synchronized (gridIter) {
                        double tmpD = gridIter.getSampleDouble(newC, newR, 0);
                        if (!HMConstants.isNovalue(tmpD)) {
                            tmp = (int) tmpD;
                        }
                    }
                }

                switch( index ) {
                case 0:
                    nwFlow = tmp;
                    break;
                case 1:
                    wFlow = tmp;
                    break;
                case 2:
                    wsFlow = tmp;
                    break;
                case 3:
                    nFlow = tmp;
                    break;
                case 4:
                    throw new RuntimeException();
                case 5:
                    sFlow = tmp;
                    break;
                case 6:
                    enFlow = tmp;
                    break;
                case 7:
                    eFlow = tmp;
                    break;
                case 8:
                    seFlow = tmp;
                    break;
                default:
                    throw new RuntimeException();
                }

                if (HMConstants.isNovalue(tmp)) {
                    touchesBound = true;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "FlowNode [\n\tcol=" + col + //
                ", \n\trow=" + row + //
                ", \n\tflow=" + flow + "(" + flowD + ")" + //
                ", \n\tisValid=" + isValid() + //
                ", \n\ttouchesBounds=" + touchesBound + //
                ", \n\tisSource=" + isSource() + //
                "\n]";
    }

    /**
     * @return <code>true</code> if this node has a {@value #OUTLET} value in the flow map. 
     */
    public boolean isMarkedAsOutlet() {
        return isMarkedAsOutlet;
    }

    /**
     * @return <code>true</code> if this node is a pixel that will flow outside of the valid flow map. 
     */
    public boolean isHeadingOutside() {
        if (!wasHeadingOutsideChecked) {
            if (!isValid) {
                isHeadingOutside = false;
            } else if (touchesBound) {
                Node goDownstream = goDownstream();
                if (goDownstream == null || !goDownstream.isValid()) {
                    isHeadingOutside = true;
                } else {
                    isHeadingOutside = false;
                }
            } else {
                isHeadingOutside = false;
            }
            wasHeadingOutsideChecked = true;
        }
        return isHeadingOutside;
    }

    /**
     * Checks if it is a source node, i.e. no others entering.
     * 
     * @return true if it is valid and a source node.
     */
    public boolean isSource() {
        if (!isValid()) {
            return false;
        }
        List<FlowNode> enteringNodes = getEnteringNodes();
        return enteringNodes.size() == 0;
    }

    /**
     * Get the value of the flow in one of the surrounding direction.
     * 
     * @param direction the {@link Direction}.
     * @return the flow value.
     */
    public int getFlowAt( Direction direction ) {
        switch( direction ) {
        case E:
            return eFlow;
        case W:
            return wFlow;
        case N:
            return nFlow;
        case S:
            return sFlow;
        case EN:
            return enFlow;
        case NW:
            return nwFlow;
        case WS:
            return wsFlow;
        case SE:
            return seFlow;
        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Get the next downstream node.
     * 
     * @return the next downstream node or <code>null</code> if the end has been reached.
     */
    public FlowNode goDownstream() {
        if (isValid) {
            Direction direction = Direction.forFlow(flow);
            if (direction != null) {
                FlowNode nextNode = new FlowNode(gridIter, cols, rows, col + direction.col, row + direction.row, intNovalue);
                if (nextNode.isValid) {
                    return nextNode;
                }
            }
        }
        return null;
    }

    /**
     * Gets all surrounding {@link FlowNode nodes} that <b>DO</b> flow into this node.
     * 
     * @return the nodes that flow into this node.
     */
    public List<FlowNode> getEnteringNodes() {
        if (enteringNodes == null) {
            enteringNodes = new ArrayList<FlowNode>();
            Direction[] orderedDirs = Direction.getOrderedDirs();
            for( Direction direction : orderedDirs ) {
                switch( direction ) {
                case E:
                    if (eFlow == Direction.E.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(gridIter, cols, rows, newCol, newRow, intNovalue);
                        enteringNodes.add(node);
                    }
                    break;
                case N:
                    if (nFlow == Direction.N.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(gridIter, cols, rows, newCol, newRow, intNovalue);
                        enteringNodes.add(node);
                    }
                    break;
                case W:
                    if (wFlow == Direction.W.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(gridIter, cols, rows, newCol, newRow, intNovalue);
                        enteringNodes.add(node);
                    }
                    break;
                case S:
                    if (sFlow == Direction.S.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(gridIter, cols, rows, newCol, newRow, intNovalue);
                        enteringNodes.add(node);
                    }
                    break;
                case EN:
                    if (enFlow == Direction.EN.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(gridIter, cols, rows, newCol, newRow, intNovalue);
                        enteringNodes.add(node);
                    }
                    break;
                case NW:
                    if (nwFlow == Direction.NW.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(gridIter, cols, rows, newCol, newRow, intNovalue);
                        enteringNodes.add(node);
                    }
                    break;
                case WS:
                    if (wsFlow == Direction.WS.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(gridIter, cols, rows, newCol, newRow, intNovalue);
                        enteringNodes.add(node);
                    }
                    break;
                case SE:
                    if (seFlow == Direction.SE.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(gridIter, cols, rows, newCol, newRow, intNovalue);
                        enteringNodes.add(node);
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
                }
            }
        }
        return enteringNodes;
    }

    /**
     * Get the upstream node based on the max tca value. 
     * 
     * @param tcaIter the tca map.
     * @param hacklengthIter the optional hacklength map, if available 
     *                  it is used in cases with multiple equal in coming tcas.
     * @return the upstream node.
     */
    public FlowNode getUpstreamTcaBased( RandomIter tcaIter, RandomIter hacklengthIter ) {
        Direction[] orderedDirs = Direction.getOrderedDirs();
        double maxTca = Double.NEGATIVE_INFINITY;
        double maxHacklength = Double.NEGATIVE_INFINITY;
        int maxCol = 0;
        int maxRow = 0;
        boolean gotOne = false;
        for( Direction direction : orderedDirs ) {
            int newCol = 0;
            int newRow = 0;
            switch( direction ) {
            case E:
                if (eFlow == Direction.E.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                    gotOne = true;
                }
                break;
            case N:
                if (nFlow == Direction.N.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                    gotOne = true;
                }
                break;
            case W:
                if (wFlow == Direction.W.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                    gotOne = true;
                }
                break;
            case S:
                if (sFlow == Direction.S.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                    gotOne = true;
                }
                break;
            case EN:
                if (enFlow == Direction.EN.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                    gotOne = true;
                }
                break;
            case NW:
                if (nwFlow == Direction.NW.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                    gotOne = true;
                }
                break;
            case WS:
                if (wsFlow == Direction.WS.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                    gotOne = true;
                }
                break;
            case SE:
                if (seFlow == Direction.SE.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                    gotOne = true;
                }
                break;
            default:
                throw new IllegalArgumentException();
            }
            if (isInRaster(newCol, newRow)) {
                int flowValue = gridIter.getSample(newCol, newRow, 0);
                if (HMConstants.isNovalue(flowValue)) {
                    continue;
                }
                int tcaValue = tcaIter.getSample(newCol, newRow, 0);
                double hacklengthValue = 0.0;
                if (hacklengthIter != null)
                    hacklengthValue = tcaIter.getSampleDouble(newCol, newRow, 0);
                if (NumericsUtilities.dEq(tcaValue, maxTca) && hacklengthIter != null) {
                    /*
                     * if there are two equal tca values around
                     * and info about hacklength is available, 
                     * use that one to choose
                     */
                    if (hacklengthValue > maxHacklength) {
                        // this has larger hacklength, use this one as max tca
                        maxTca = tcaValue;
                        maxCol = newCol;
                        maxRow = newRow;
                        maxHacklength = hacklengthValue;
                    }
                } else if (tcaValue > maxTca) {
                    maxTca = tcaValue;
                    maxCol = newCol;
                    maxRow = newRow;
                    maxHacklength = hacklengthValue;
                }
            }
        }
        if (!gotOne) {
            return null;
        }
        FlowNode node = new FlowNode(gridIter, cols, rows, maxCol, maxRow, intNovalue);
        return node;
    }

    private boolean isInRaster( int col, int row ) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + col;
        long temp;
        temp = Double.doubleToLongBits(flowD);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + row;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FlowNode other = (FlowNode) obj;
        if (col != other.col || row != other.row)
            return false;
        if (Double.doubleToLongBits(flowD) != Double.doubleToLongBits(other.flowD))
            return false;
        return true;
    }

}
