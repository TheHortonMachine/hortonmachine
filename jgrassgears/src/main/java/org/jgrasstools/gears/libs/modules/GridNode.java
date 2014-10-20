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

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;

import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

import org.jgrasstools.gears.utils.math.NumericsUtilities;

/**
 * A node in the grid environment of a digital elevation model. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GridNode extends Node {

    public final double elevation;
    public final double xRes;
    public final double yRes;

    private boolean isPit = false;
    private boolean isOutlet = false;

    private double eElev;
    private double enElev;
    private double nElev;
    private double nwElev;
    private double wElev;
    private double wsElev;
    private double sElev;
    private double seElev;

    /**
     * The constructor.
     * 
     * @param elevationIter the elevation model raster iter.
     * @param cols the cols of the raster.
     * @param rows the rows of the raster.
     * @param xRes the x resolution of the raster.
     * @param yRes the y resolution of the raster.
     * @param col the col of the current {@link GridNode node}.
     * @param row the row of the current {@link GridNode node}.
     */
    public GridNode( RandomIter elevationIter, int cols, int rows, double xRes, double yRes, int col, int row ) {
        super(elevationIter, cols, rows, col, row);

        this.xRes = xRes;
        this.yRes = yRes;

        if (isInRaster(col, row)) {
            elevation = gridIter.getSampleDouble(col, row, 0);
            if (JGTConstants.isNovalue(elevation)) {
                isValid = false;
            } else {
                isValid = true;
            }
        } else {
            elevation = doubleNovalue;
            isValid = false;
        }

        double tmpMin = Double.POSITIVE_INFINITY;
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
                    try {
                        tmp = gridIter.getSampleDouble(newC, newR, 0);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // touches - might be tiled, which needs better support
                        touchesBound = true;
                    }
                }

                switch( index ) {
                case 0:
                    nwElev = tmp;
                    break;
                case 1:
                    wElev = tmp;
                    break;
                case 2:
                    wsElev = tmp;
                    break;
                case 3:
                    nElev = tmp;
                    break;
                case 4:
                    throw new RuntimeException();
                case 5:
                    sElev = tmp;
                    break;
                case 6:
                    enElev = tmp;
                    break;
                case 7:
                    eElev = tmp;
                    break;
                case 8:
                    seElev = tmp;
                    break;
                default:
                    throw new RuntimeException();
                }

                if (JGTConstants.isNovalue(tmp)) {
                    touchesBound = true;
                } else {
                    if (tmp < tmpMin) {
                        tmpMin = tmp;
                    }
                }
            }
        }
        if (elevation < tmpMin) {
            isPit = true;
        }
    }

    @Override
    public String toString() {
        return "GridNode [\n\tcol=" + col + //
                ", \n\trow=" + row + //
                ", \n\televation=" + elevation + //
                ", \n\tisValid=" + isValid() + //
                ", \n\ttouchesBounds=" + touchesBound + //
                "\n]";
    }

    /**
     * @return <code>true</code> if this node can't flow anywhere following the steepest path downstream. 
     */
    public boolean isOutlet() {
        return isOutlet;
    }

    /**
     * @return <code>true</code> if all cells around the node are higher than the current.
     */
    public boolean isPit() {
        return isPit;
    }

    /**
     * Get a window of values surrounding the current node.
     * 
     * <p>Notes:</p>
     * <ul>
     *  <li>the size has to be odd, so that the current node can be in the center. 
     *      If the size is even, size+1 will be used.</li>
     *  <li>values outside the boundaries of the raster will be set to novalue.
     *      No exception is thrown.</li>
     * </ul>
     * 
     * @param size the size of the window. The window will be a matrix window[size][size].
     * @param doCircular if <code>true</code> the window values are set to novalue
     *              were necessary to make it circular.
     * @return the read window.
     */
    public double[][] getWindow( int size, boolean doCircular ) {
        if (size % 2 == 0) {
            size++;
        }
        double[][] window = new double[size][size];
        int delta = (size - 1) / 2;
        if (!doCircular) {
            for( int c = -delta; c <= delta; c++ ) {
                int tmpCol = col + c;
                for( int r = -delta; r <= delta; r++ ) {
                    int tmpRow = row + r;
                    GridNode n = new GridNode(gridIter, cols, rows, xRes, yRes, tmpCol, tmpRow);
                    window[r + delta][c + delta] = n.elevation;
                }
            }
        } else {
            double radius = delta; // rows + half cell
            for( int c = -delta; c <= delta; c++ ) {
                int tmpCol = col + c;
                for( int r = -delta; r <= delta; r++ ) {
                    int tmpRow = row + r;

                    double distance = sqrt(c * c + r * r);
                    if (distance <= radius) {
                        GridNode n = new GridNode(gridIter, cols, rows, xRes, yRes, tmpCol, tmpRow);
                        window[r + delta][c + delta] = n.elevation;
                    } else {
                        window[r + delta][c + delta] = doubleNovalue;
                    }
                }
            }
        }
        return window;
    }

    /**
     * Get the value of the elevation in one of the surrounding direction.
     * 
     * @param direction the {@link Direction}.
     * @return the elevation value.
     */
    public double getElevationAt( Direction direction ) {
        switch( direction ) {
        case E:
            return eElev;
        case W:
            return wElev;
        case N:
            return nElev;
        case S:
            return sElev;
        case EN:
            return enElev;
        case NW:
            return nwElev;
        case WS:
            return wsElev;
        case SE:
            return seElev;
        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Get next downstream {@link GridNode node} following the steepest path.
     * 
     * @return the next downstream node or <code>null</code> if it is an outlet.
     */
    public GridNode goDownstreamSP() {
        Direction[] orderedDirs = Direction.getOrderedDirs();
        double maxSlope = Double.NEGATIVE_INFINITY;
        GridNode nextNode = null;
        for( int i = 0; i < orderedDirs.length; i++ ) {
            Direction direction = orderedDirs[i];
            int newCol = col + direction.col;
            int newRow = row + direction.row;
            if (isInRaster(newCol, newRow)) {
                GridNode node = new GridNode(gridIter, cols, rows, xRes, yRes, newCol, newRow);
                if (node.isValid()) {
                    double slopeTo = getSlopeTo(node);
                    if (slopeTo > 0 && slopeTo > maxSlope) {
                        nextNode = node;
                        maxSlope = slopeTo;
                    }
                }
            }
        }
        if (nextNode == null) {
            isOutlet = true;
        }
        return nextNode;
    }

    // /**
    // * Get next upstream {@link GridNode node}, based on least cost.
    // *
    // * @return the next least cost, upstream node.
    // */
    // public GridNode goLeastCostUpstream() {
    //
    // return null;
    // }

    /**
     * Gets all surrounding {@link GridNode nodes}, starting from the most eastern.
     * 
     * Note that the list contains all 8 directions, but some might be null, if outside a boundary 
     * 
     * @return the nodes surrounding the current node. 
     */
    public List<GridNode> getSurroundingNodes() {
        List<GridNode> nodes = new ArrayList<GridNode>();
        Direction[] orderedDirs = Direction.getOrderedDirs();
        for( int i = 0; i < orderedDirs.length; i++ ) {
            Direction direction = orderedDirs[i];
            int newCol = col + direction.col;
            int newRow = row + direction.row;
            if (isInRaster(newCol, newRow)) {
                GridNode node = new GridNode(gridIter, cols, rows, xRes, yRes, newCol, newRow);
                if (node.isValid()) {
                    nodes.add(node);
                } else {
                    nodes.add(null);
                }
            } else {
                nodes.add(null);
            }
        }
        return nodes;
    }

    /**
     * Get a neighbor node at a certain direction.
     * 
     * @param direction the direction to get the node at.
     * @return the node.
     */
    public GridNode getNodeAt( Direction direction ) {
        int newCol = col + direction.col;
        int newRow = row + direction.row;
        GridNode node = new GridNode(gridIter, cols, rows, xRes, yRes, newCol, newRow);
        return node;
    }

    /**
     * Checks if the supplied node is adjacent to the current.
     * 
     * @return the {@link Direction} if the two cells touch, else <code>null</code>.
     */
    public Direction isNeighborOf( GridNode otherNode ) {
        Direction[] orderedDirs = Direction.getOrderedDirs();
        for( int i = 0; i < orderedDirs.length; i++ ) {
            Direction direction = orderedDirs[i];
            int newCol = col + direction.col;
            int newRow = row + direction.row;
            if (otherNode.col == newCol && otherNode.row == newRow) {
                return direction;
            }
        }
        return null;
    }

    /**
     * Checks if the supplied node is adjacent to the current and has the same value.
     * 
     * @return the {@link Direction} if the two cells touch and have the same value, else <code>null</code>.
     */
    public Direction isSameValueNeighborOf( GridNode otherNode ) {
        Direction direction = isNeighborOf(otherNode);
        if (direction != null && NumericsUtilities.dEq(elevation, otherNode.elevation)) {
            return direction;
        }
        return null;
    }

    /**
     * Gets only the valid surrounding {@link GridNode nodes}, starting from the most eastern.
     * 
     * @return the valid nodes surrounding the current node. 
     */
    public List<GridNode> getValidSurroundingNodes() {
        List<GridNode> nodes = new ArrayList<GridNode>();
        Direction[] orderedDirs = Direction.getOrderedDirs();
        for( int i = 0; i < orderedDirs.length; i++ ) {
            Direction direction = orderedDirs[i];
            int newCol = col + direction.col;
            int newRow = row + direction.row;
            if (isInRaster(newCol, newRow)) {
                GridNode node = new GridNode(gridIter, cols, rows, xRes, yRes, newCol, newRow);
                if (node.isValid()) {
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    /**
     * Gets all surrounding {@link GridNode nodes} that <b>DO</b> flow into this node by steepest path rule.
     * 
     * @return the nodes that flow into this node.
     */
    public List<GridNode> getEnteringNodesSP() {
        List<GridNode> nodes = new ArrayList<GridNode>();
        List<GridNode> surroundingNodes = getSurroundingNodes();
        for( GridNode flowNode : surroundingNodes ) {
            if (flowNode != null) {
                GridNode downstream = flowNode.goDownstreamSP();
                if (downstream.isValid() && this.equals(downstream)) {
                    nodes.add(flowNode);
                }
            }
        }
        return nodes;
    }

    /**
     * Gets all surrounding {@link GridNode nodes} that do <b>NOT</b> flow into this node by steepest path rule.
     * 
     * @return the nodes that flow into this node.
     */
    public List<GridNode> getNonEnteringNodesSP() {
        List<GridNode> nodes = new ArrayList<GridNode>();
        List<GridNode> surroundingNodes = getSurroundingNodes();
        for( GridNode flowNode : surroundingNodes ) {
            if (flowNode != null) {
                GridNode downstream = flowNode.goDownstreamSP();
                if (!downstream.isValid() || !this.equals(downstream)) {
                    nodes.add(flowNode);
                }
            }
        }
        return nodes;
    }

    private boolean isInRaster( int col, int row ) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return false;
        }
        return true;
    }

    /**
     * Calculates the slope from the current to the supplied point. 
     * 
     * @param node the node to which to calculate the slope to.
     * @return the slope.
     */
    public double getSlopeTo( GridNode node ) {
        double slope = (elevation - node.elevation) / getDistance(node);
        return slope;
    }

    public double getDistance( GridNode node ) {
        return sqrt(pow((node.col - col) * xRes, 2.0) + pow((node.row - row) * yRes, 2.0));
    }

    public double getEastElev() {
        return eElev;
    }

    public double getENElev() {
        return enElev;
    }

    public double getNorthElev() {
        return nElev;
    }

    public double getNWElev() {
        return nwElev;
    }

    public double getWestElev() {
        return wElev;
    }

    public double getWSElev() {
        return wsElev;
    }

    public double getSouthElev() {
        return sElev;
    }

    public double getSEElev() {
        return seElev;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + col;
        long temp;
        temp = Double.doubleToLongBits(elevation);
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
        GridNode other = (GridNode) obj;
        if (col != other.col || row != other.row)
            return false;
        if (Double.doubleToLongBits(elevation) != Double.doubleToLongBits(other.elevation))
            return false;
        return true;
    }

}
