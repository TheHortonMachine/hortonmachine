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
package org.hortonmachine.llm.script;

/**
 * The idioms of HortonMachine scripts, given to the model with every request: what the examples
 * and the module references don't say, and what models get wrong without being told.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ScriptNotes {

    /**
     * The notes.
     */
    public static final String TEXT = """
            How HortonMachine scripts work:
            - Scripts are Groovy and need their imports: the modules are in org.hortonmachine.modules,
              the HM helper class in org.hortonmachine.
            - A module is used by creating it, setting its fields and calling process():
                def pitfiller = new Pitfiller()
                pitfiller.inElev = "/data/dtm.tif"
                pitfiller.outPit = "/data/pit.tif"
                pitfiller.process()
              Inputs and outputs are file paths as strings; the output of one module is the input of the next.
              Set only fields listed for the module; fields with a default can be left out.
              Results (fields marked result) are read after process().
            - Rasters are read and written by the modules from paths, best as GeoTIFF (.tif).
              For vectors prefer GeoPackage (.gpkg) over shapefiles.
            - For raster algebra use the Mapcalc module (pFunction with the expression, the maps known
              by their file names without extension) instead of loops over cells.
            - When cells must be read in a script, use org.hortonmachine.gears.libs.modules.HMRaster:
                def raster = HMRaster.fromFile(path)   // or HM.readHMRaster(path)
                raster.getCols(), raster.getRows(), raster.getValue(col, row), raster.isNovalue(value)
                new HMRaster.HMRasterWritableBuilder().setTemplate(raster).setName("out").build()
                writable.setValue(col, row, value); HM.dumpHMRaster(writable, outPath); raster.close()
            - For reference systems use org.hortonmachine.gears.utils.crs.HMCrsRegistry
              (HM.getCrsRegistry()) and HM.getCrsTransformer("EPSG:4326", "EPSG:32632").
            - Coordinates given to modules (pNorth, pEast of an outlet, ...) are in the projection of
              the maps, not longitude and latitude. If the user gives longitude and latitude, transform them:
                import org.hortonmachine.gears.utils.crs.*
                import org.hortonmachine.gears.libs.modules.HMRaster
                import org.locationtech.jts.geom.Coordinate
                def lonLatCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true) // longitude first
                def mapCrs = HMRaster.fromFile(dem).getCrs() // prj files often have no EPSG code
                def point = new HMCrsTransformer(lonLatCrs, mapCrs).transform(new Coordinate(lon, lat))
                // then pEast = point.x, pNorth = point.y
            - Print what the user needs to see with println. The script runs in the console, which
              shows its output.
            - Paths the user did not give are placeholders: declare them as variables at the top, as
              def dtm = "/path/to/dtm.tif", so the user can change them in one place.
            """;

    private ScriptNotes() {
    }
}
