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

import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;

/**
 * A node in the flow enviroment of a digital elevation model. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FlowNode {

    private int row;
    private int col;
    private GridCoverage2D raster;

    public FlowNode( GridCoverage2D raster, int col, int row ) {
        this.raster = raster;
        this.col = col;
        this.row = row;
    }

    /**
     * Get next downstream {@link FlowNode node}.
     * 
     * @return the next downstream node.
     */
    public FlowNode goDownstream() {
        return null;
    }

    /**
     * Get next upstream {@link FlowNode node}, based on least cost.
     * 
     * @return the next least cost, upstream node.
     */
    public FlowNode goLeastCostUpstream() {
        return null;
    }

    /**
     * Gets all surrounding {@link FlowNode nodes}.
     * 
     * @return the nodes surrounding the current node. 
     */
    public List<FlowNode> getSurroundingNodes() {

        return null;
    }

    /**
     * Gets all surrounding {@link FlowNode nodes} that <b>DO</b> flow into this node.
     * 
     * @return the nodes that flow into this node.
     */
    public List<FlowNode> getEnteringNodes() {

        return null;
    }

    /**
     * Gets all surrounding {@link FlowNode nodes} that do <b>NOT</b> flow into this node.
     * 
     * @return the nodes that flow into this node.
     */
    public List<FlowNode> getNonEnteringNodes() {

        return null;
    }

}
