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

import static java.lang.Math.*;
import static org.jgrasstools.gears.libs.modules.JGTConstants.*;

import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

/**
 * A node in the grid environment of a digital elevation model. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GridNode {

    public final int row;
    public final int col;
    public final double elevation;
    public final double xRes;
    public final double yRes;

    private RandomIter elevationIter;
    private int cols;
    private int rows;
    private boolean isOutlet = false;
    private boolean touchesBound = false;

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
        this.elevationIter = elevationIter;
        this.cols = cols;
        this.rows = rows;
        this.xRes = xRes;
        this.yRes = yRes;
        this.col = col;
        this.row = row;

        if (isInRaster(col, row)) {
            elevation = elevationIter.getSampleDouble(col, row, 0);
        } else {
            elevation = doubleNovalue;
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
                    tmp = elevationIter.getSampleDouble(newC, newR, 0);
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
                }
            }
        }
    }

    @Override
    public String toString() {
        return "GridNode [\n\tcol=" + col + //
                ", \n\trow=" + row + //
                ", \n\televation=" + elevation + //
                ", \n\ttisValid=" + isValid() + //
                ", \n\ttouchesBounds=" + touchesBound + //
                "\n]";
    }

    /**
     * Checks if the node is valid.
     * 
     * <p>A node is valid if</p>
     * <ul>
     *  <li>it is placed inside the raster bounds</li>
     *  <li>its elevation value is not novalue</li>
     * </ul>
     * 
     * @return <code>true</code> if the node is valid.
     */
    public boolean isValid() {
        if (JGTConstants.isNovalue(elevation)) {
            return false;
        } else if (!isInRaster(col, row)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return <code>true</code> if this node can't flow anywhere following the steepest path downstream. 
     */
    public boolean isOutlet() {
        return isOutlet;
    }

    /**
     * @return <code>true</code> if this node touches a boundary, i.e. any novalue or raster limit.
     */
    public boolean touchesBound() {
        return touchesBound;
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
                GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, newCol, newRow);
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

    /**
     * Get next upstream {@link GridNode node}, based on least cost.
     * 
     * @return the next least cost, upstream node.
     */
    public GridNode goLeastCostUpstream() {

        return null;
    }

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
                GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, newCol, newRow);
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
        double slope = (elevation - node.elevation)
                / (sqrt(pow((node.col - col) * xRes, 2.0) + pow((node.row - row) * yRes, 2.0)));
        return slope;
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
