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

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

/**
 * A superclass representing a node. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class Node {

    public final int row;
    public final int col;
    public final int cols;
    public final int rows;
    protected boolean isValid;
    protected boolean touchesBound = false;
    protected boolean touchesNovalue = false;
    protected final RandomIter gridIter;
    protected double doubleNovalue = HMConstants.doubleNovalue;
    protected float floatNovalue = HMConstants.floatNovalue;
    protected int intNovalue = HMConstants.intNovalue;

    public Node( RandomIter gridIter, int cols, int rows, int col, int row, Double novalue ) {
        this.gridIter = gridIter;
        this.cols = cols;
        this.rows = rows;
        this.col = col;
        this.row = row;
        if (novalue != null) {
            this.doubleNovalue = novalue;
            this.floatNovalue = novalue.floatValue();
            if (!Double.isNaN(novalue)) {
                // set int nv only if different from NaN, else it will be set to 0
                this.intNovalue = novalue.intValue();
            }
        }
    }

    /**
     * Get the value from a map. Default to getting a double value.
     * 
     * @param map the map to read from.
     * @return the float value read.
     */
    public double getValueFromMap( RandomIter map ) {
        return getDoubleValueFromMap(map);
    }

    /**
     * Get the float value of another map in the current node position.
     * 
     * @param map the map from which to get the value. 
     * @return the float value or a novalue.
     */
    public float getFloatValueFromMap( RandomIter map ) {
        try {
            if (map == null) {
                return floatNovalue;
            }
            float value = map.getSampleFloat(col, row, 0);
            return value;
        } catch (Exception e) {
            // ignore and return novalue
            return floatNovalue;
        }
    }

    /**
     * Get the int value of another map in the current node position.
     * 
     * @param map the map from which to get the value. 
     * @return the int value or a novalue.
     */
    public int getIntValueFromMap( RandomIter map ) {
        try {
            if (map == null) {
                return intNovalue;
            }
            int value = map.getSample(col, row, 0);
            return value;
        } catch (Exception e) {
            // ignore and return novalue
            return intNovalue;
        }
    }

    /**
     * Get the double value of another map in the current node position.
     * 
     * @param map the map from which to get the value. 
     * @return the double value or a novalue.
     */
    public double getDoubleValueFromMap( RandomIter map ) {
        try {
            if (map == null) {
                return doubleNovalue;
            }
            double value = map.getSampleDouble(col, row, 0);
            return value;
        } catch (Exception e) {
            // ignore and return novalue
            return doubleNovalue;
        }
    }

    public void setValueInMap( WritableRandomIter map, double value ) {
        setDoubleValueInMap(map, value);
    }

    /**
     * Utility method to set the value of a certain map in the current node position.
     * 
     * @param map the map to set the value in. if <code>null</code>, it is ignored.
     * @param value the value to set.
     */
    public void setFloatValueInMap( WritableRandomIter map, float value ) {
        if (map == null) {
            return;
        }
        try {
            map.setSample(col, row, 0, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setIntValueInMap( WritableRandomIter map, int value ) {
        if (map == null) {
            return;
        }
        try {
            map.setSample(col, row, 0, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDoubleValueInMap( WritableRandomIter map, double value ) {
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
     * @return <code>true</code> if this node touches a boundary.
     */
    public boolean touchesBound() {
        return touchesBound;
    }

    /**
     * @return <code>true</code> if this node touches a novalue.
     */
    public boolean touchesNovalue() {
        return touchesNovalue;
    }

}