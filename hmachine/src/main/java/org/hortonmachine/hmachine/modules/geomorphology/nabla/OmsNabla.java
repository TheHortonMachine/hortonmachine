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
package org.hortonmachine.hmachine.modules.geomorphology.nabla;

import static org.hortonmachine.gears.libs.modules.HMConstants.*;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_DOCUMENTATION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_outNabla_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_pThres_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

@Description(OMSNABLA_DESCRIPTION)
@Documentation(OMSNABLA_DOCUMENTATION)
@Author(name = OMSNABLA_AUTHORNAMES, contact = OMSNABLA_AUTHORCONTACTS)
@Keywords(OMSNABLA_KEYWORDS)
@Label(OMSNABLA_LABEL)
@Name(OMSNABLA_NAME)
@Status(OMSNABLA_STATUS)
@License(OMSNABLA_LICENSE)
public class OmsNabla extends HMModel {
    @Description(OMSNABLA_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSNABLA_pThres_DESCRIPTION)
    @In
    public Double pThreshold = null;

    @Description(OMSNABLA_outNabla_DESCRIPTION)
    @Out
    public GridCoverage2D outNabla = null;

    private int nCols;

    private double xRes;

    private int nRows;

    private double yRes;

    @Execute
    public void process() {
        if (!concatOr(outNabla == null, doReset)) {
            return;
        }
        checkNull(inElev);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage elevationRI = inElev.getRenderedImage();
        RandomIter elevationIter = RandomIterFactory.create(elevationRI, null);

        WritableRaster gradientWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        if (pThreshold == null) {
            nabla(elevationIter, gradientWR);
        } else {
            nabla_mask(elevationIter, gradientWR, pThreshold);
        }

        outNabla = CoverageUtilities.buildCoverage("nabla", gradientWR, regionMap, inElev.getCoordinateReferenceSystem());
    }

    private void nabla( RandomIter elevationIter, WritableRaster nablaRaster ) {
        int y;
        double nablaT, n;
        double[] z = new double[9];
        int[][] v = ModelsEngine.DIR;

        WritableRaster segnWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);

        // grid contains the dimension of pixels according with flow directions
        double[] grid = new double[9];
        grid[0] = 0;
        grid[1] = grid[5] = xRes;
        grid[3] = grid[7] = yRes;
        grid[2] = grid[4] = grid[6] = grid[8] = Math.sqrt(grid[1] * grid[1] + grid[3] * grid[3]);

        pm.beginTask("Processing nabla...", nCols * 2);
        for( int r = 1; r < nRows - 1; r++ ) {
            for( int c = 1; c < nCols - 1; c++ ) {
                z[0] = elevationIter.getSampleDouble(c, r, 0);
                if (!isNovalue((z[0]))) {
                    y = 1;
                    for( int h = 1; h <= 8; h++ ) {
                        z[h] = elevationIter.getSample(c + v[h][0], r + v[h][1], 0);
                        if (isNovalue(z[h])) {
                            y = 0;
                            segnWR.setSample(c, r, 0, 1);
                            break;
                        }
                    }
                    if (y == 0) {
                        nablaRaster.setSample(c, r, 0, doubleNovalue);
                    } else {
                        double derivata = 0.5 * ((z[1] + z[5] - 2 * z[0]) / (grid[1] * grid[1])
                                + (z[3] + z[7] - 2 * z[0]) / (grid[3] * grid[3]));
                        double derivata2 = derivata + 0.5 * ((z[2] + z[4] + z[6] + z[8] - 4 * z[0]) / (grid[6] * grid[6]));
                        nablaRaster.setSample(c, r, 0, derivata2);
                    }
                } else {
                    nablaRaster.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }

        for( int r = 1; r < nRows - 1; r++ ) {
            for( int c = 1; c < nCols - 1; c++ ) {
                if (segnWR.getSampleDouble(c, r, 0) == 1) {
                    n = 0.0;
                    nablaT = 0.0;
                    y = 0;
                    for( int h = 1; h <= 8; h++ ) {
                        z[h] = elevationIter.getSampleDouble(c + v[h][0], r + v[h][1], 0);
                        y = 0;
                        double nablaSample = nablaRaster.getSampleDouble(c + v[h][0], r + v[h][1], 0);
                        if (isNovalue(z[h]) || !isNovalue(nablaSample))
                            y = 1;
                        if (y == 0) {
                            n += 1;
                            nablaT += nablaSample;
                        }
                    }
                    if (n == 0)
                        n = 1;
                    nablaRaster.setSample(c, r, 0, nablaT / (float) n);
                }
            }
            pm.worked(1);
        }
        pm.done();

    }

    /**
     * Computes the nabla algorithm.
     * <p>
     * This is the 0 mode which returns a "mask" so the value of the nablaRaster is equal to 1 of
     * the nabla*nabla is <=threshold
     * </p>
     * 
     * @param elevationIter holding the elevation data.
     * @param nablaRaster the to which the Nabla values are written
     * @param pThreshold2 
     */
    private void nabla_mask( RandomIter elevationIter, WritableRaster nablaRaster, double thNabla ) {
        int y;
        double[] z = new double[9];
        double derivate2;
        int[][] v = ModelsEngine.DIR;

        // grid contains the dimension of pixels according with flow directions
        double[] grid = new double[9];
        grid[0] = 0;
        grid[1] = grid[5] = xRes;
        grid[3] = grid[7] = yRes;
        grid[2] = grid[4] = grid[6] = grid[8] = Math.sqrt(grid[1] * grid[1] + grid[3] * grid[3]);

        pm.beginTask("Processing nabla...", nCols * 2);
        for( int r = 1; r < nRows - 1; r++ ) {
            for( int c = 1; c < nCols - 1; c++ ) {
                z[0] = elevationIter.getSampleDouble(c, r, 0);
                if (!isNovalue(z[0])) {
                    y = 1;
                    // if there is a no value around the current pixel then do nothing.
                    for( int h = 1; h <= 8; h++ ) {
                        z[h] = elevationIter.getSampleDouble(c + v[h][0], r + v[h][1], 0);
                        if (isNovalue(z[h])) {
                            y = 0;
                            break;
                        }
                    }
                    if (y == 0) {
                        nablaRaster.setSample(c, r, 0, 1);
                    } else {
                        derivate2 = 0.5 * ((z[1] + z[5] - 2 * z[0]) / (grid[1] * grid[1])
                                + (z[3] + z[7] - 2 * z[0]) / (grid[3] * grid[3]));
                        derivate2 = derivate2 + 0.5 * ((z[2] + z[4] + z[6] + z[8] - 4 * z[0]) / (grid[6] * grid[6]));

                        if (Math.abs(derivate2) <= thNabla || derivate2 > thNabla) {
                            nablaRaster.setSample(c, r, 0, 0);
                        } else {
                            nablaRaster.setSample(c, r, 0, 1);
                        }
                    }
                } else {
                    nablaRaster.setSample(c, r, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

}
