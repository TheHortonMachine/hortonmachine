/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules.r.rastervaluerounder;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.WritableRaster;
import java.text.DecimalFormat;

import javax.media.jai.iterator.RandomIter;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Module that rounds the values of a raster map.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Round, Raster")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.CERTIFIED)
@Name("rround")
@License("General Public License Version 3 (GPLv3)")
public class RasterValueRounder extends JGTModel {

    @Description("The raster of which to round the values.")
    @In
    public GridCoverage2D inRaster;

    @Description("The rounding pattern.")
    @In
    public String pPattern = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The rounded raster.")
    @Out
    public GridCoverage2D outRaster;

    private DecimalFormat formatter = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        checkNull(inRaster, pPattern);

        formatter = new DecimalFormat(pPattern);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);
        RandomIter inRasterIter = CoverageUtilities.getRandomIterator(inRaster);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        pm.beginTask("Rounding data...", cols);
        for( int c = 0; c < cols; c++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int r = 0; r < rows; r++ ) {
                double value = inRasterIter.getSampleDouble(c, r, 0);
                if (!isNovalue(value)) {
                    String formatted = formatter.format(value);
                    value = Double.parseDouble(formatted);
                    outIter.setSample(c, r, 0, value);
                } else {
                    outIter.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outIter.done();

        outRaster = CoverageUtilities.buildCoverage("rounded", outWR, regionMap, inRaster.getCoordinateReferenceSystem());
    }

}
