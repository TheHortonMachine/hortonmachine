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
package org.hortonmachine.gears.modules.r.rasternull;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_LABEL;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_LICENSE;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_NAME;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull.OMSRASTERNULL_STATUS;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.multiprocessing.GridMultiProcessing;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

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

@Description(OMSRASTERNULL_DESCRIPTION)
@Documentation(OMSRASTERNULL_DOCUMENTATION)
@Author(name = OMSRASTERNULL_AUTHORNAMES, contact = OMSRASTERNULL_AUTHORCONTACTS)
@Keywords(OMSRASTERNULL_KEYWORDS)
@Label(OMSRASTERNULL_LABEL)
@Name(OMSRASTERNULL_NAME)
@Status(OMSRASTERNULL_STATUS)
@License(OMSRASTERNULL_LICENSE)
public class OmsRasterNull extends GridMultiProcessing {

    @Description(OMSRASTERNULL_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERNULL_P_VALUE_DESCRIPTION)
    @In
    public Double pValue = null;

    @Description(OMSRASTERNULL_doInverse_DESCRIPTION)
    @In
    public boolean doInverse = false;

    @Description(OMSRASTERNULL_P_NULL_DESCRIPTION)
    @In
    public Double pNull = null;

    @Description(OMSRASTERNULL_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERNULL_DESCRIPTION = "Module that puts a certain value of the raster to null.";
    public static final String OMSRASTERNULL_DOCUMENTATION = "";
    public static final String OMSRASTERNULL_KEYWORDS = "Null, Raster";
    public static final String OMSRASTERNULL_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERNULL_NAME = "rnull";
    public static final int OMSRASTERNULL_STATUS = 40;
    public static final String OMSRASTERNULL_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERNULL_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERNULL_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERNULL_IN_RASTER_DESCRIPTION = "The raster to modify.";
    public static final String OMSRASTERNULL_P_VALUE_DESCRIPTION = "The value to set to null.";
    public static final String OMSRASTERNULL_doInverse_DESCRIPTION = "If true, sets everything else to null.";
    public static final String OMSRASTERNULL_P_NULL_DESCRIPTION = "The the null value to set (else it is guessed).";
    public static final String OMSRASTERNULL_OUT_RASTER_DESCRIPTION = "The new raster.";

    private double nullValue;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, pValue);

        double replaceValue = pValue;
        nullValue = HMConstants.doubleNovalue;
        if (pNull != null) {
            nullValue = pNull;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        WritableRaster outWR = CoverageUtilities.renderedImage2DoubleWritableRaster(inRaster.getRenderedImage(), false);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        pm.beginTask("Nulling data...", cols * rows);
        processGrid(cols, rows, ( c, r ) -> {
            if (pm.isCanceled()) {
                return;
            }
            double value = outIter.getSampleDouble(c, r, 0);
            if (!isNovalue(value)) {
                if (doInverse) {
                    if (!NumericsUtilities.dEq(value, replaceValue)) {
                        value = doubleNovalue;
                    }
                } else {
                    if (NumericsUtilities.dEq(value, replaceValue)) {
                        value = nullValue;
                    }
                }
                outIter.setSample(c, r, 0, value);
            } else {
                outIter.setSample(c, r, 0, doubleNovalue);
            }
            pm.worked(1);
        });
        pm.done();

        outIter.done();

        outRaster = CoverageUtilities.buildCoverage("nulled", outWR, regionMap, inRaster.getCoordinateReferenceSystem());
    }

}
