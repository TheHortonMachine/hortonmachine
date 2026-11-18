/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.io.opendap;

import java.util.List;

/**
 * The window of a grid variable to download, as found by {@link OpendapGridReader#findSubset}.
 *
 * <p>The index ranges are those of the dataset, the ends included for the columns and rows, excluded for the
 * timesteps.</p>
 *
 * @param variable the grid variable.
 * @param xDimension the name of the x (column) dimension.
 * @param yDimension the name of the y (row) dimension.
 * @param timeDimension the name of the time dimension, <code>null</code> if the grid has no time.
 * @param fromCol the first column.
 * @param toCol the last column, included.
 * @param fromRow the first row.
 * @param toRow the last row, included.
 * @param fromTimestep the first timestep, 0 if the grid has no time.
 * @param toTimestep the timestep at which to stop, excluded, 1 if the grid has no time.
 * @param coordinateVariables the coordinate variables of the dimensions present in the dataset, as x, y and time.
 * @param spatialVariables the auxiliary variables on the y and x dimensions, as the 2D lat and lon, subset with the
 *          grid.
 * @param otherVariables the variables taken whole, as the grid mapping.
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record OpendapSubset( String variable, String xDimension, String yDimension, String timeDimension, int fromCol,
        int toCol, int fromRow, int toRow, int fromTimestep, int toTimestep, List<String> coordinateVariables,
        List<String> spatialVariables, List<String> otherVariables ) {

    /**
     * @return the number of columns.
     */
    public int cols() {
        return toCol - fromCol + 1;
    }

    /**
     * @return the number of rows.
     */
    public int rows() {
        return toRow - fromRow + 1;
    }

    /**
     * @return the number of timesteps, 1 if the grid has no time.
     */
    public int timesteps() {
        return toTimestep - fromTimestep;
    }

    /**
     * @return the number of cells of a timestep.
     */
    public long cellsPerTimestep() {
        return (long) cols() * rows();
    }

    @Override
    public String toString() {
        return variable + ": columns " + fromCol + "-" + toCol + ", rows " + fromRow + "-" + toRow
                + (timeDimension == null ? "" : ", timesteps " + fromTimestep + "-" + (toTimestep - 1)) + " (" + cols() + " x "
                + rows() + " cells" + (timeDimension == null ? "" : " x " + timesteps() + " timesteps") + ")";
    }
}
