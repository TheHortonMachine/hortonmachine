/* 
 * polymap.org
 * Copyright (C) 2017, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.jgrasstools.gears.libs.modules.multiprocessing;

import java.util.HashMap;
import java.util.Map;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public abstract class GridNodeMultiProcessing extends MultiProcessing {

    /** The cache of {@link #regionMap()} */
    private Map<Integer, RegionMap> regionMaps = new HashMap();

    /**
     * Calculates the {@link RegionMap} for teh given grid by calling
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

        ExecutionPlanner planner = createDefaultPlanner();
        planner.setNumberOfTasks(rows * cols);

        // Cycling into the valid region.
        for( int r = 1; r < rows - 1; r++ ) {
            for( int c = 1; c < cols - 1; c++ ) {
                int _c = c, _r = r;
                planner.submit(() -> {
                    if (!pm.isCanceled()) {
                        GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, _c, _r);
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
