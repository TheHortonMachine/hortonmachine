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

/**
 * A superclass representing a node. 
 * 
 * @author Andrea Antonello (www.g-ant.eu)
 * @since 0.11.0
 */
public abstract class NodeNG {

    public final int row;
    public final int col;
    public final int cols;
    public final int rows;
    protected boolean isValid;
    protected boolean touchesBound = false;
    protected boolean touchesNovalue = false;
    protected final HMRaster raster;

    public NodeNG( HMRaster raster, int col, int row) {
        this.raster = raster;
        this.cols = raster.getCols();
        this.rows = raster.getRows();
        this.col = col;
        this.row = row;
    }

    /**
     * Get the value from a map. Default to getting a double value.
     * 
     * @param otherRaster the map to read from.
     * @return the float value read.
     */
    public double getValueFromRaster( HMRaster otherRaster ) {
        return getDoubleValueFromRaster(otherRaster);
    }

    /**
     * Get the float value of another map in the current node position.
     * 
     * @param otherRaster the map from which to get the value. 
     * @return the float value or a novalue.
     */
	public float getFloatValueFromRaster(HMRaster otherRaster) {
		if (otherRaster == null) {
			throw new IllegalArgumentException("Other raster is null");
		}
		float value = (float) otherRaster.getValue(col, row);
		return value;
	}

    /**
     * Get the int value of another map in the current node position.
     * 
     * @param otherRaster the map from which to get the value. 
     * @return the int value or a novalue.
     */
	public int getIntValueFromRaster(HMRaster otherRaster) {
		if (otherRaster == null) {
			throw new IllegalArgumentException("Other raster is null");
		}
		int value = otherRaster.getIntValue(col, row);
		return value;
	}

    /**
     * Get the double value of another map in the current node position.
     * 
     * @param otherRaster the map from which to get the value. 
     * @return the double value or a novalue.
     */
    public double getDoubleValueFromRaster( HMRaster otherRaster ) {
            if (otherRaster == null) {
                throw new IllegalArgumentException("Other raster is null");
            }
            double value = otherRaster.getValue(col, row);
            return value;
    }

    public void setValueInRaster( HMRaster otherRaster, double value ) {
        setDoubleValueInRaster(otherRaster, value);
    }

    /**
     * Utility method to set the value of a certain map in the current node position.
     * 
     * @param otherRaster the map to set the value in. if <code>null</code>, it is ignored.
     * @param value the value to set.
     */
    public void setFloatValueInMap( HMRaster otherRaster, float value ) {
        if (otherRaster == null) {
            return;
        }
        try {
            otherRaster.setValue(col, row, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setIntValueInRaster( HMRaster otherRaster, int value ) {
        if (otherRaster == null) {
            return;
        }
        try {
            otherRaster.setValue(col, row, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDoubleValueInRaster( HMRaster otherRaster, double value ) {
        if (otherRaster == null) {
            return;
        }
        try {
            otherRaster.setValue(col, row, value);
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