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

/**
 * The possible flowdirections.
 *
 * <p>
 * The normal (exiting) flowdirection schema defines in which cell the drop flows
 * if it has a particular value.
 * <pre>
 *        -1     0     1 
 *      +-----+-----+-----+
 * -1   |  4  |  3  |  2  |
 *      +-----+-----+-----+
 *  0   |  5  |  x  |  1  |
 *      +-----+-----+-----+
 *  1   |  6  |  7  |  8  |
 *      +-----+-----+-----+
 * </pre>
 * </p>
 * The entering flowdirection schema defines what value the neighbour cell 
 * has to have to flow into the current (center) cell.
 * <pre>
 *        -1     0     1 
 *      +-----+-----+-----+
 * -1   |  8  |  7  |  6  |
 *      +-----+-----+-----+
 *  0   |  1  |  x  |  5  |
 *      +-----+-----+-----+
 *  1   |  2  |  3  |  4  |
 *      +-----+-----+-----+
 * </pre>     
 * <p>
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum Direction {
    E(1, 0, 1, 5), //
    EN(1, -1, 2, 6), //
    N(0, -1, 3, 7), //
    NW(-1, -1, 4, 8), //
    W(-1, 0, 5, 1), //
    WS(-1, 1, 6, 2), //
    S(-1, 0, 7, 3), //
    SE(1, 1, 8, 4);

    private int col;
    private int row;
    private int exiting;
    private int entering;

    /**
     * Constructor.
     * 
     * @param col column of the current direction inside the schema.
     * @param row row of the current direction inside the schema.
     * @param exiting value of the flow, in normal (or exiting from the center/flow to) mode.
     * @param entering value of the flow in entering mode.
     */
    private Direction( int col, int row, int exiting, int entering ) {
        this.col = col;
        this.row = row;
        this.exiting = exiting;
        this.entering = entering;
    }
    
    public int getFlow() {
        return exiting;
    }

    public int getEnteringFlow() {
        return exiting;
    }
    
}
