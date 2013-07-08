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

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

/**
 * A superclass representing a node. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Node {

    public final int row;
    public final int col;
    public final int cols;
    public final int rows;
    protected boolean isValid;
    protected boolean touchesBound = false;
    protected final RandomIter gridIter;

    public Node( RandomIter gridIter, int cols, int rows, int col, int row ) {
        this.gridIter = gridIter;
        this.cols = cols;
        this.rows = rows;
        this.col = col;
        this.row = row;
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
     * @return <code>true</code> if this node touches a boundary, i.e. any novalue or raster limit.
     */
    public boolean touchesBound() {
        return touchesBound;
    }

}