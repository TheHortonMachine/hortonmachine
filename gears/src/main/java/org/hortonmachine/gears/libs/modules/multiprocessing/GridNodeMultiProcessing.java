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

import java.util.HashMap;
import java.util.Map;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class GridNodeMultiProcessing extends MultiProcessing {

    /** The cache of {@link #regionMap()} */
    private Map<Integer, RegionMap> regionMaps = new HashMap<>();

    /**
     * Calculates the {@link RegionMap} for the given grid by calling
     * {@link CoverageUtilities#getRegionParamsFromGridCoverage(GridCoverage2D)}. The
     * result is cached and re-used.
     */
    public RegionMap regionMap( GridCoverage2D grid ) {
        return regionMaps.computeIfAbsent(grid.hashCode(), key -> {
            return CoverageUtilities.getRegionParamsFromGridCoverage(grid);
        });
    }

    /**
     * Loops through all rows and cols of the given grid and calls the given
     * calculator for each {@link GridNode}.
     */
    protected void processGridNodes( GridCoverage2D inElev, Calculator<GridNode> calculator ) throws Exception {
        RegionMap regionMap = regionMap(inElev);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();
        
        RandomIter elevationIter = CoverageUtilities.getRandomIterator(inElev);
        
        double novalue = HMConstants.getNovalue(inElev);

        ExecutionPlanner planner = createDefaultPlanner();
        planner.setNumberOfTasks(rows * cols);

        // Cycling into the valid region.
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                int _c = c, _r = r;
                planner.submit(() -> {
                    if (!pm.isCanceled()) {
                        GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, _c, _r, novalue);
                        calculator.calculate(node);
                    }
                });
            }
        }
        planner.join();
    }

    @FunctionalInterface
    protected interface Calculator<T> {
        void calculate( T input ) throws Exception;
    }

}
