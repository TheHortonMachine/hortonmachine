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
package org.hortonmachine.gears.modules.r.filter;

import static java.lang.Math.abs;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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

@Description("A biased sigma filter.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("raster, filter, biased sigma")
@Label(HMConstants.RASTERPROCESSING)
@Name("sigmafilter")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsBiasedSigmaFilter extends HMModel {

    @Description("The input raster")
    @In
    public GridCoverage2D inGeodata;

    @Description("The filter windows in cells.")
    @In
    public int pWindow = 3;

    @Description("The output raster")
    @Out
    public GridCoverage2D outGeodata;

    @Execute
    public void process() throws Exception {
        checkNull(inGeodata);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inGeodata);

        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xres = regionMap.getXres();
        double yres = regionMap.getYres();

        RandomIter inIter = CoverageUtilities.getRandomIterator(inGeodata);
        double novalue = HMConstants.getNovalue(inGeodata);
        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, novalue);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        int part = (pWindow - 1) / 2;

        pm.beginTask("Processing filter...", cols - (pWindow - 1));
        for( int r = part; r < rows - part; r++ ) {
            for( int c = part; c < cols - part; c++ ) {
                GridNode node = new GridNode(inIter, cols, rows, xres, yres, c, r, novalue);
                if (node.isValid() && !node.touchesBound()) {
                    double[][] window = node.getWindow(pWindow, false);

                    double elevation = node.elevation;
                    double sumUpper = 0;
                    double countUpper = 0;
                    double sumLower = 0;
                    double countLower = 0;

                    for( int i = 0; i < window.length; i++ ) {
                        for( int j = 0; j < window[0].length; j++ ) {
                            if (i == 1 && j == 1) {
                                continue;
                            }
                            if (window[i][j] >= elevation) {
                                sumUpper = sumUpper + window[i][j];
                                countUpper++;
                            } else {
                                sumLower = sumLower + window[i][j];
                                countLower++;
                            }
                        }
                    }

                    double avgUpper = sumUpper / countUpper;
                    double avgLower = sumLower / countLower;
                    double value = 0;
                    if (countUpper == 0) {
                        value = avgLower;
                    } else if (countLower == 0) {
                        value = avgUpper;
                    } else {
                        double deltaUpper = abs(elevation - avgUpper);
                        double deltaLower = abs(elevation - avgLower);
                        if (deltaUpper < deltaLower) {
                            value = avgUpper;
                        } else {
                            value = avgLower;
                        }
                    }
                    if (!isNovalue(value)) {
                        outIter.setSample(c, r, 0, value);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
        outIter.done();

        outGeodata = CoverageUtilities.buildCoverage("sigma", outWR, regionMap, inGeodata.getCoordinateReferenceSystem());
    }

}
