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
package org.hortonmachine.gears.modules.r.rasterdiff;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_LABEL;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_LICENSE;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_NAME;
import static org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff.OMSRASTERDIFF_STATUS;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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

@Description(OMSRASTERDIFF_DESCRIPTION)
@Documentation(OMSRASTERDIFF_DOCUMENTATION)
@Author(name = OMSRASTERDIFF_AUTHORNAMES, contact = OMSRASTERDIFF_AUTHORCONTACTS)
@Keywords(OMSRASTERDIFF_KEYWORDS)
@Label(OMSRASTERDIFF_LABEL)
@Name(OMSRASTERDIFF_NAME)
@Status(OMSRASTERDIFF_STATUS)
@License(OMSRASTERDIFF_LICENSE)
public class OmsRasterDiff extends HMModel {

    @Description(OMSRASTERDIFF_IN_RASTER1_DESCRIPTION)
    @In
    public GridCoverage2D inRaster1;

    @Description(OMSRASTERDIFF_IN_RASTER2_DESCRIPTION)
    @In
    public GridCoverage2D inRaster2;

    @Description(OMSRASTERDIFF_P_THRESHOLD_DESCRIPTION)
    @In
    public Double pThreshold;

    @Description(OMSRASTERDIFF_DO_NEGATIVES_DESCRIPTION)
    @In
    public boolean doNegatives = true;

    @Description(OMSRASTERDIFF_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERDIFF_DESCRIPTION = "Raster diff module.";
    public static final String OMSRASTERDIFF_DOCUMENTATION = "";
    public static final String OMSRASTERDIFF_KEYWORDS = "IO, Coverage, Raster, Correct, OmsRasterReader";
    public static final String OMSRASTERDIFF_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERDIFF_NAME = "rdiff";
    public static final int OMSRASTERDIFF_STATUS = 5;
    public static final String OMSRASTERDIFF_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERDIFF_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERDIFF_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERDIFF_IN_RASTER1_DESCRIPTION = "The input raster.";
    public static final String OMSRASTERDIFF_IN_RASTER2_DESCRIPTION = "The raster to subtract.";
    public static final String OMSRASTERDIFF_P_THRESHOLD_DESCRIPTION = "The threshold, under which to set novalue.";
    public static final String OMSRASTERDIFF_DO_NEGATIVES_DESCRIPTION = "Allow negative values.";
    public static final String OMSRASTERDIFF_OUT_RASTER_DESCRIPTION = "The output raster.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster1, inRaster2);

        double thres = 0.0;
        if (pThreshold != null) {
            thres = pThreshold;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster1);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        RandomIter r1Iter = CoverageUtilities.getRandomIterator(inRaster1);
        RandomIter r2Iter = CoverageUtilities.getRandomIterator(inRaster2);

        double r1Nv = HMConstants.getNovalue(inRaster1);
        double r2Nv = HMConstants.getNovalue(inRaster2);
        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, r1Nv);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        pm.beginTask("Subtracting raster...", cols);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double r1 = r1Iter.getSampleDouble(c, r, 0);
                double r2 = r2Iter.getSampleDouble(c, r, 0);
                double diff;
                if (isNovalue(r1, r1Nv) && isNovalue(r2, r2Nv)) {
                    continue;
                } else if (isNovalue(r1, r1Nv)) {
                    diff = r2;
                } else if (isNovalue(r2, r2Nv)) {
                    diff = r1;
                } else {
                    diff = r1 - r2;
                }
                if (!doNegatives && diff < 0) {
                    diff = 0.0;
//                    diff = doubleNovalue;
                }
                if (pThreshold != null && diff < thres) {
                    diff = r1Nv;
                }
                outIter.setSample(c, r, 0, diff);
            }
            pm.worked(1);
        }
        pm.done();

        r1Iter.done();
        r2Iter.done();
        outIter.done();

        outRaster = CoverageUtilities.buildCoverageWithNovalue("corrected", outWR, regionMap,
                inRaster1.getCoordinateReferenceSystem(), r1Nv);
    }

}
