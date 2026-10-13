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
package org.hortonmachine.gears.modules.r.rastergenerator;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.util.Random;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OmsRasterGenerator.DESCRIPTION)
@Documentation(OmsRasterGenerator.DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsRasterGenerator.KEYWORDS)
@Label(HMConstants.RASTERPROCESSING)
@Name("_" + OmsRasterGenerator.NAME)
@Status(OmsRasterGenerator.STATUS)
@License(OMSHYDRO_LICENSE)
public class OmsRasterGenerator extends HMModel {

    @Description(pNorth_DESCRIPTION)
    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(pSouth_DESCRIPTION)
    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(pWest_DESCRIPTION)
    @UI(HMConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(pEast_DESCRIPTION)
    @UI(HMConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(pXres_DESCRIPTION)
    @UI(HMConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description(pYres_DESCRIPTION)
    @UI(HMConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description(pCode_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(pCrs_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public CoordinateReferenceSystem inCrs;

    @Description(pValue_DESCRIPTION)
    @In
    public double pValue = 0.0;

    @Description(doRandom_DESCRIPTION)
    @In
    public boolean doRandom = false;

    @Description(pOffset_DESCRIPTION)
    @In
    public double pOffset = 0.0;

    @Description(pScale_DESCRIPTION)
    @In
    public double pScale = 1.0;

    @Description(pSeed_DESCRIPTION)
    @In
    public Long pSeed = null;

    @Description(outRaster_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String DESCRIPTION = "Creates a new raster on a grid defined by its bounds, cell size and reference system, with a constant or random value.";
    public static final String DOCUMENTATION = """
            It is the module that makes a grid from nothing: Mapcalc, SurfaceInterpolator and the \
            rasterizers need a raster that gives them the grid, and this one gives it where there \
            is no data yet, as for a synthetic terrain or the interpolation of points over a new \
            area. The grid starts at the north-west corner: the number of columns and rows is the \
            extent divided by the cell size, rounded, and the east and south bounds are moved so \
            that the cells have exactly the given size.

            The cells get the constant value, or with the random switch a value drawn uniformly \
            between the offset and the offset plus the scale. The seed makes the random raster \
            the same at every run; without it, every run gives a different one.
            """;
    public static final String NAME = "rastergenerator";
    public static final String KEYWORDS = "Raster, Generator, Grid, Random";
    public static final int STATUS = 40;
    public static final String pNorth_DESCRIPTION = "The north bound of the grid.";
    public static final String pSouth_DESCRIPTION = "The south bound of the grid.";
    public static final String pWest_DESCRIPTION = "The west bound of the grid.";
    public static final String pEast_DESCRIPTION = "The east bound of the grid.";
    public static final String pXres_DESCRIPTION = "The cell size in x.";
    public static final String pYres_DESCRIPTION = "The cell size in y.";
    public static final String pCode_DESCRIPTION = "The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:32632).";
    public static final String pCrs_DESCRIPTION = "The coordinate reference system (in case pCode is not supplied).";
    public static final String outRaster_DESCRIPTION = "The generated raster.";
    public static final String pScale_DESCRIPTION = "The width of the range of the random values.";
    public static final String pOffset_DESCRIPTION = "The lowest random value.";
    public static final String doRandom_DESCRIPTION = "If true, the cells get random values instead of the constant one.";
    public static final String pSeed_DESCRIPTION = "An optional seed, to get the same random values at every run.";
    public static final String pValue_DESCRIPTION = "The value of the cells.";

    @Execute
    public void process() throws Exception {
        checkNull(pNorth, pSouth, pEast, pWest, pXres, pYres);
        if (pCode == null && inCrs == null) {
            throw new ModelsIllegalargumentException("A reference system is necessary, as code or as object.", this, pm);
        }

        CoordinateReferenceSystem crs = inCrs;
        if (crs == null) {
            crs = HMCrsRegistry.INSTANCE.getCrs(pCode);
        }

        // the cells have exactly the given size, the grid is anchored at the north-west corner
        int cols = (int) Math.max(1, Math.round((pEast - pWest) / pXres));
        int rows = (int) Math.max(1, Math.round((pNorth - pSouth) / pYres));
        RegionMap regionMap = RegionMap.fromBoundsAndGrid(pWest, pWest + cols * pXres, pNorth - rows * pYres, pNorth, cols,
                rows);

        try (HMRaster outData = new HMRasterWritableBuilder().setName("generated").setRegion(regionMap).setCrs(crs)
                .setNoValue(HMConstants.doubleNovalue).setInitialValue(pValue).build()) {
            if (doRandom) {
                Random random = pSeed != null ? new Random(pSeed) : new Random();
                pm.beginTask("Generating raster...", rows);
                for( int r = 0; r < rows; r++ ) {
                    if (pm.isCanceled()) {
                        return;
                    }
                    for( int c = 0; c < cols; c++ ) {
                        outData.setValue(c, r, pOffset + pScale * random.nextDouble());
                    }
                    pm.worked(1);
                }
                pm.done();
            }
            outRaster = outData.buildCoverage();
        }
    }

}
