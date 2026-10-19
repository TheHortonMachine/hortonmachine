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
package org.hortonmachine.server;

import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

/**
 * The data of the tests.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class SampleData {
    /** The folder of the shared data of the tests, a collection of the catalog. */
    public static final String COLLECTION = "flanginec";
    /** The path in the workspace of the elevation written by {@link #writeElevation(WorkspacePaths)}. */
    public static final String ELEVATION = WorkspacePaths.DATA + "/" + WorkspacePaths.SHARED + "/" + COLLECTION + "/elevation.tif";

    private SampleData() {
    }

    /**
     * Write a small elevation with a pit in the middle, in the shared area.
     */
    public static void writeElevation( WorkspacePaths paths ) throws Exception {
        double[][] elevation = { //
                {10, 10, 10, 10, 10}, //
                {10, 9, 9, 9, 10}, //
                {10, 9, 5, 9, 10}, //
                {10, 9, 9, 9, 10}, //
                {10, 10, 10, 10, 8}};
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 50, 0, 50, 5, 5);
        GridCoverage2D coverage = CoverageUtilities.buildCoverage("elevation", elevation, region,
                HMCrsRegistry.INSTANCE.getCrs("EPSG:32632"), true);
        File file = new File(paths.getWorkspace(), ELEVATION);
        file.getParentFile().mkdirs();
        OmsRasterWriter.writeRaster(file.getAbsolutePath(), coverage);
    }
}
