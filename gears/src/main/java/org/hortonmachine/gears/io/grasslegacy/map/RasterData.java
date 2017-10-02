/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 package org.hortonmachine.gears.io.grasslegacy.map;

/**
 * <p>
 * Interface describing a raster map for JGrass internal usage.
 * </p>
 * <p>
 * This was thought instead of the direct data matrix usage in order to be able to migrate to the
 * coverage standard at some point.
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public interface RasterData {

    /**
     * Get the raster's value at given row and col.
     * 
     * @param row
     * @param col
     * @return the value in double format
     */
    public double getValueAt( int row, int col );

    /**
     * Get the raster's value at given position.
     * 
     * @param index
     * @return the value in double format
     */
    public double getValueAt( int index );

    /**
     * Get the raster's value at given position.
     * 
     * @param point the row, col position
     * @return the value in double format
     */
    public double getValueAt( java.awt.Point point );

    /**
     * Set the raster's value at given row and col.
     * 
     * @param row
     * @param col
     * @param value
     */
    public void setValueAt( int row, int col, double value );

    /**
     * Set the raster's value at given position.
     * 
     * @param index
     * @param value
     */
    public void setValueAt( int index, double value );

    /**
     * @return the number of rows of the raster
     */
    public int getRows();

    /**
     * @return the number of cols of the raster
     */
    public int getCols();

    /**
     * @return a string representation of the raster
     */
    public String toString();

	/**
	 * Get a row of the raster matrix
	 * 
	 * @param row the row to extract
	 */
	public double[] getRowValue(int row);
	
	
	/**
	 * Get the matrix of data.
	 * 
	 * @return the matrix of data.
	 */
	public double[][] getData();

}