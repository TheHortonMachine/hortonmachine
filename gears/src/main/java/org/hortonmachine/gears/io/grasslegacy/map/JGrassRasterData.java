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
 * Class representing JGrass's internal data format. Basically a wrapper.
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class JGrassRasterData implements RasterData {

    private double[][] data = null;

    /**
     * Create the object from an existing data matrix
     * 
     * @param data
     */
    public JGrassRasterData( double[][] data ) {
        this.data = data;
    }

    /**
     * Create a new data object initializing its values
     * 
     * @param data the new memory allocation for the data
     * @param value init value
     */
    public JGrassRasterData( double[][] data, double value ) {
        for( int i = 0; i < data.length; i++ ) {
            for( int j = 0; j < data[0].length; j++ ) {
                data[i][j] = value;
            }
        }
        this.data = data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.hydrologis.jgrass.libs.map.RasterData#getValueAt(int, int)
     */
    public double getValueAt( int row, int col ) {
        return data[row][col];
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.hydrologis.jgrass.libs.map.RasterData#getValueAt(int)
     */
    public double getValueAt( int index ) {

        int row = index / data[0].length;
        int col = index % data[0].length;

        return data[row][col];
    }

    public double getValueAt( java.awt.Point point ) {
        return data[point.x][point.y];
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.hydrologis.jgrass.libs.map.RasterData#setValueAt(int, int)
     */
    public void setValueAt( int row, int col, double value ) {
        data[row][col] = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.hydrologis.jgrass.libs.map.RasterData#setValueAt(int)
     */
    public void setValueAt( int index, double value ) {

        int row = index / data[0].length;
        int col = index % data[0].length;

        data[row][col] = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.hydrologis.jgrass.libs.map.RasterData#getRows()
     */
    public int getRows() {
        return data.length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.hydrologis.jgrass.libs.map.RasterData#getCols()
     */
    public int getCols() {
        return data[0].length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.hydrologis.jgrass.libs.map.RasterData#toString()
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("JGrass Raster Data Matrix: ").append("rows = ").append(data.length).append(
                " cols = ").append(data[0].length);
        return buf.toString();

    }

	/* (non-Javadoc)
	 * @see eu.hydrologis.jgrass.libs.map.RasterData#getRowValue(int)
	 */
	public double[] getRowValue(int row) {
		return data[row];
	}

    public double[][] getData() {
        return data;
    }
}
