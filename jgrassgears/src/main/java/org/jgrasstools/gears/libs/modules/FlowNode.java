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
package org.jgrasstools.gears.libs.modules;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;

import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.jgrasstools.gears.utils.math.NumericsUtilities;

/**
 * A node in the grid environment of a digital elevation model. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.6
 */
public class FlowNode {
    /**
     * The outlet value of flow.
     */
    public static final double OUTLET = 10.0;
    /**
     * The defaut value used for marking a network. 
     */
    public static double NETVALUE = 2.0;

    public final int row;
    public final int col;
    public final double flow;

    private RandomIter flowIter;
    private int cols;
    private int rows;

    private final boolean isValid;
    private boolean isMarkedAsOutlet = false;
    private boolean isHeadingOutside = false;
    private boolean wasHeadingOutsideChecked = false;
    private boolean touchesBound = false;

    private double eFlow;
    private double enFlow;
    private double nFlow;
    private double nwFlow;
    private double wFlow;
    private double wsFlow;
    private double sFlow;
    private double seFlow;

    private List<FlowNode> enteringNodes;

    /**
     * The constructor.
     * 
     * @param flowIter the elevation model raster iter.
     * @param cols the cols of the raster.
     * @param rows the rows of the raster.
     * @param col the col of the current {@link FlowNode node}.
     * @param row the row of the current {@link FlowNode node}.
     */
    public FlowNode( RandomIter flowIter, int cols, int rows, int col, int row ) {
        this.flowIter = flowIter;
        this.cols = cols;
        this.rows = rows;
        this.col = col;
        this.row = row;

        if (!isInRaster(col, row)) {
            isValid = false;
            flow = doubleNovalue;
        } else {
            flow = flowIter.getSampleDouble(col, row, 0);
            if (JGTConstants.isNovalue(flow)) {
                isValid = false;
            } else {
                isValid = true;
            }
        }

        if ((int) flow == (int) OUTLET) {
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
                double tmp = doubleNovalue;
                if (!isInRaster(newC, newR)) {
                    touchesBound = true;
                } else {
                    tmp = flowIter.getSampleDouble(newC, newR, 0);
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

                if (JGTConstants.isNovalue(tmp)) {
                    touchesBound = true;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "FlowNode [\n\tcol=" + col + //
                ", \n\trow=" + row + //
                ", \n\tflow=" + flow + //
                ", \n\tisValid=" + isValid() + //
                ", \n\ttouchesBounds=" + touchesBound + //
                "\n]";
    }

    /**
     * Checks if the node is valid.
     * 
     * <p>A node is valid if</p>
     * <ul>
     *  <li>it is placed inside the raster bounds</li>
     *  <li>its value is not novalue</li>
     * </ul>
     * 
     * @return <code>true</code> if the node is valid.
     */
    public boolean isValid() {
        return isValid;
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
            if (touchesBound) {
                FlowNode goDownstream = goDownstream();
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
     * @return <code>true</code> if this node touches a boundary, i.e. any novalue or raster limit.
     */
    public boolean touchesBound() {
        return touchesBound;
    }

    /**
     * Get the value of the flow in one of the surrounding direction.
     * 
     * @param direction the {@link Direction}.
     * @return the elevation value.
     */
    public double getFlowAt( Direction direction ) {
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
            Direction direction = Direction.forFlow((int) flow);
            if (direction != null) {
                FlowNode nextNode = new FlowNode(flowIter, cols, rows, col + direction.col, row + direction.row);
                if (nextNode.isValid) {
                    return nextNode;
                }
            }
        }
        return null;
    }

    /**
     * Get the value of another map in the current node position.
     * 
     * @param map the map from which to get the value. 
     * @return the double value or a novalue.
     */
    public double getValueFromMap( RandomIter map ) {
        try {
            double value = map.getSampleDouble(col, row, 0);
            return value;
        } catch (Exception e) {
            // ignore and return novalue
            return JGTConstants.doubleNovalue;
        }
    }

    /**
     * Utility method to set the value of a certain map in the current node position.
     * 
     * @param map the map to set the value in. if <code>null</code>, it is ignored.
     * @param value the value to set.
     */
    public void setValueInMap( WritableRandomIter map, double value ) {
        if (map == null) {
            return;
        }
        try {
            map.setSample(col, row, 0, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    if ((int) eFlow == Direction.E.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(flowIter, cols, rows, newCol, newRow);
                        enteringNodes.add(node);
                    }
                    break;
                case N:
                    if ((int) nFlow == Direction.N.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(flowIter, cols, rows, newCol, newRow);
                        enteringNodes.add(node);
                    }
                    break;
                case W:
                    if ((int) wFlow == Direction.W.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(flowIter, cols, rows, newCol, newRow);
                        enteringNodes.add(node);
                    }
                    break;
                case S:
                    if ((int) sFlow == Direction.S.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(flowIter, cols, rows, newCol, newRow);
                        enteringNodes.add(node);
                    }
                    break;
                case EN:
                    if ((int) enFlow == Direction.EN.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(flowIter, cols, rows, newCol, newRow);
                        enteringNodes.add(node);
                    }
                    break;
                case NW:
                    if ((int) nwFlow == Direction.NW.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(flowIter, cols, rows, newCol, newRow);
                        enteringNodes.add(node);
                    }
                    break;
                case WS:
                    if ((int) wsFlow == Direction.WS.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(flowIter, cols, rows, newCol, newRow);
                        enteringNodes.add(node);
                    }
                    break;
                case SE:
                    if ((int) seFlow == Direction.SE.getEnteringFlow()) {
                        int newCol = col + direction.col;
                        int newRow = row + direction.row;
                        FlowNode node = new FlowNode(flowIter, cols, rows, newCol, newRow);
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
    /**
     * @param tcaIter
     * @return
     */
    public FlowNode getUpstreamTcaBased( RandomIter tcaIter, RandomIter hacklengthIter ) {
        Direction[] orderedDirs = Direction.getOrderedDirs();
        double maxTca = Double.NEGATIVE_INFINITY;
        double maxHacklength = Double.NEGATIVE_INFINITY;
        int maxCol = 0;
        int maxRow = 0;
        for( Direction direction : orderedDirs ) {
            int newCol = 0;
            int newRow = 0;
            switch( direction ) {
            case E:
                if ((int) eFlow == Direction.E.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                }
                break;
            case N:
                if ((int) nFlow == Direction.N.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                }
                break;
            case W:
                if ((int) wFlow == Direction.W.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                }
                break;
            case S:
                if ((int) sFlow == Direction.S.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                }
                break;
            case EN:
                if ((int) enFlow == Direction.EN.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                }
                break;
            case NW:
                if ((int) nwFlow == Direction.NW.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                }
                break;
            case WS:
                if ((int) wsFlow == Direction.WS.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                }
                break;
            case SE:
                if ((int) seFlow == Direction.SE.getEnteringFlow()) {
                    newCol = col + direction.col;
                    newRow = row + direction.row;
                }
                break;
            default:
                throw new IllegalArgumentException();
            }
            if (isInRaster(newCol, newRow)) {
                double tcaValue = tcaIter.getSampleDouble(newCol, newRow, 0);
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
        FlowNode node = new FlowNode(flowIter, cols, rows, maxCol, maxRow);
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
        temp = Double.doubleToLongBits(flow);
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
        if (Double.doubleToLongBits(flow) != Double.doubleToLongBits(other.flow))
            return false;
        return true;
    }

    public void mark( WritableRandomIter basinIter, double value ) {
        basinIter.setSample(col, row, 0, value);
    }

}
