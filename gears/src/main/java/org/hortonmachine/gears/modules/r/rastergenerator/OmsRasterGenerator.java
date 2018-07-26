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

import static java.lang.Math.round;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.image.WritableRaster;
import java.util.Random;

import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OmsRasterGenerator.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsRasterGenerator.KEYWORDS)
@Label(HMConstants.RASTERPROCESSING)
@Name("_" + OmsRasterGenerator.NAME)
@Status(OMSHYDRO_DRAFT)
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

    @Description(outRaster_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String DESCRIPTION = "Generates a raster.";
    public static final String NAME = "rastergenerator";
    public static final String KEYWORDS = "generator, raster";
    public static final String pNorth_DESCRIPTION = "The boundary north coordinate.";
    public static final String pSouth_DESCRIPTION = "The boundary south coordinate.";
    public static final String pWest_DESCRIPTION = "The boundary west coordinate.";
    public static final String pEast_DESCRIPTION = "The boundary east coordinate.";
    public static final String pXres_DESCRIPTION = "The resolution in x.";
    public static final String pYres_DESCRIPTION = "The resolution in y.";
    public static final String pCode_DESCRIPTION = "The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:4328).";
    public static final String pCrs_DESCRIPTION = "The coordinate reference system (in case pCode is not supplied).";
    public static final String outRaster_DESCRIPTION = "The generated raster.";
    public static final String pScale_DESCRIPTION = "Optional random scale.";
    public static final String pOffset_DESCRIPTION = "Optional random offset.";
    public static final String doRandom_DESCRIPTION = "If true, uses a random generator.";
    public static final String pValue_DESCRIPTION = "The value to set the raster to.";

    @Execute
    public void process() throws Exception {
        checkNull(pNorth, pSouth, pEast, pWest, pXres, pYres);
        if (pCode == null && inCrs == null) {
            throw new ModelsIllegalargumentException("At lest one of the CRS definitions are necessary.", this);
        }

        CoordinateReferenceSystem crs = inCrs;
        if (crs == null)
            crs = CrsUtilities.getCrsFromEpsg(pCode, null);

        int rows = (int) round((pNorth - pSouth) / pYres);
        int cols = (int) round((pEast - pWest) / pXres);

        GridGeometry2D gridGeometryFromRegionValues = CoverageUtilities.gridGeometryFromRegionValues(pNorth, pSouth, pEast,
                pWest, cols, rows, crs);
        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(gridGeometryFromRegionValues);

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, null);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        Random random = new Random();
        pm.beginTask("Generating raster...", rows);
        for( int r = 0; r < rows; r++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int c = 0; c < cols; c++ ) {
                double value = pValue;
                if (doRandom) {
                    value = pOffset + pScale * random.nextDouble();
                }
                outIter.setSample(c, r, 0, value);
            }
            pm.worked(1);
        }
        pm.done();
        outIter.done();

        outRaster = CoverageUtilities.buildCoverage("generated", outWR, regionMap, crs);
    }

}
