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
package org.jgrasstools.gears.libs.modules.multiprocessing;

/**
 * A multiprocessing class for grid loops (nested for over matrix).
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class GridMultiProcessing extends MultiProcessing {

    /**
     * Loops through all rows and cols of the given grid.
     */
    protected void processGrid( int cols, int rows, Calculator calculator ) throws Exception {
        ExecutionPlanner planner = createDefaultPlanner();
        planner.setNumberOfTasks(rows * cols);

        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                int _c = c, _r = r;
                planner.submit(() -> {
                    if (!pm.isCanceled()) {
                        calculator.calculate(_c, _r);
                    }
                });
            }
        }
        planner.join();
    }

    @FunctionalInterface
    protected interface Calculator {
        void calculate( int col, int row ) throws Exception;
    }

}
