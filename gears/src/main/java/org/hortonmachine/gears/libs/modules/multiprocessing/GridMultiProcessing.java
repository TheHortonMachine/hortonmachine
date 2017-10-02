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
package org.hortonmachine.gears.libs.modules.multiprocessing;

/**
 * A multiprocessing class for grid loops (nested for over matrix).
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class GridMultiProcessing extends MultiProcessing {

    protected void processGrid( int cols, int rows, Calculator calculator ) throws Exception {
        processGrid(cols, rows, false, calculator);
    }
    /**
     * Loops through all rows and cols of the given grid.
     */
    protected void processGrid( int cols, int rows, boolean ignoreBorder, Calculator calculator ) throws Exception {
        ExecutionPlanner planner = createDefaultPlanner();
        planner.setNumberOfTasks(rows * cols);

        int startC = 0;
        int startR = 0;
        int endC = cols;
        int endR = rows;
        if (ignoreBorder) {
            startC = 1;
            startR = 1;
            endC = cols - 1;
            endR = rows - 1;
        }
        for( int r = startR; r < endR; r++ ) {
            for( int c = startC; c < endC; c++ ) {
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
