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
package org.hortonmachine.gears.modules.r.normalizer;

import static org.hortonmachine.gears.libs.modules.HMConstants.GPL3_LICENSE;
import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

@Description("Normalizes a raster.")
@Documentation("")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("normalize, raster")
@Label(RASTERPROCESSING)
@Name("rnormalizer")
@Status(Status.EXPERIMENTAL)
@License(GPL3_LICENSE)
public class OmsRasterNormalizer extends HMModel {

    @Description("The raster to be normalized.")
    @In
    public GridCoverage2D inRaster;

    @Description("The value to normalize to (default is 256).")
    @In
    public double pNValue = 256.0;

    @Description("If true, then novalues are set to 0.")
    @In
    public boolean doSetnovalues = true;

    @Description("The normalized raster")
    @Out
    public GridCoverage2D outRaster = null;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        double[] minMax = OmsRasterSummary.getMinMax(inRaster);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RandomIter rasterIter = CoverageUtilities.getRandomIterator(inRaster);

        WritableRaster outWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        pm.beginTask("Normalizing...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int c = 0; c < nCols; c++ ) {
                double value = rasterIter.getSampleDouble(c, r, 0);
                if (isNovalue(value)) {
                    if (doSetnovalues) {
                        outIter.setSample(c, r, 0, 0.0);
                    }
                    continue;
                }
                double normalizedValue = NumericsUtilities.normalize(minMax[1], minMax[0], value, pNValue);
                outIter.setSample(c, r, 0, normalizedValue);
            }
            pm.worked(1);
        }
        pm.done();

        outRaster = CoverageUtilities.buildCoverage("normalized", outWR, regionMap, inRaster.getCoordinateReferenceSystem());

    }

}
