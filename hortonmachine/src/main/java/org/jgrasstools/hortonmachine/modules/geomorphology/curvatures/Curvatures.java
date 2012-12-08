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
package org.jgrasstools.hortonmachine.modules.geomorphology.curvatures;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("It estimates the longitudinal, normal and planar curvatures.")
@Documentation("Curvatures.html")
@Author(name = "Daniele Andreis, Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Geomorphology")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("curvatures")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Curvatures extends JGTModel {
    @Description("The map of the digital elevation model (DEM or pit).")
    @In
    public GridCoverage2D inElev = null;

    // output
    @Description("The map of profile curvatures.")
    @Out
    public GridCoverage2D outProf = null;

    @Description("The map of planar curvatures.")
    @Out
    public GridCoverage2D outPlan = null;

    @Description("The map of tangential curvatures.")
    @Out
    public GridCoverage2D outTang = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outProf == null, doReset)) {
            return;
        }
        checkNull(inElev);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter elevationIter =CoverageUtilities.getRandomIterator(inElev);

        WritableRaster profWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRaster planWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRaster tangWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        // first derivate
        WritableRaster sxData = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRaster syData = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        // second derivative
        WritableRaster sxxData = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRaster syyData = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRaster sxyData = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        double plan = 0.0;
        double tang = 0.0;
        double prof = 0.0;

        /*
         * calculate first derivative 
         */
        pm.beginTask(msg.message("curvatures.firstderivate"), nCols - 2);
        for( int x = 1; x < nCols - 1; x++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int y = 1; y < nRows - 1; y++ ) {
                if (isNovalue(elevationIter.getSampleDouble(x, y, 0))) {
                    sxData.setSample(x, y, 0, doubleNovalue);
                    syData.setSample(x, y, 0, doubleNovalue);
                } else {
                    sxData.setSample(x, y, 0,
                            0.5 * (elevationIter.getSampleDouble(x, y + 1, 0) - elevationIter.getSampleDouble(x, y - 1, 0))
                                    / xRes);
                    syData.setSample(x, y, 0,
                            0.5 * (elevationIter.getSampleDouble(x + 1, y, 0) - elevationIter.getSampleDouble(x - 1, y, 0))
                                    / yRes);
                }
            }
            pm.worked(1);
        }
        pm.done();

        /*
         * calculate second derivative
         */
        pm.beginTask(msg.message("curvatures.secondderivate"), nRows - 2);
        double disXX = Math.pow(xRes, 2.0);
        double disYY = Math.pow(yRes, 2.0);
        // calculate the second order derivative
        for( int j = 1; j < nRows - 1; j++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int i = 1; i < nCols - 1; i++ ) {
                if (isNovalue(elevationIter.getSampleDouble(i, j, 0))) {
                    sxxData.setSample(i, j, 0, doubleNovalue);
                    syyData.setSample(i, j, 0, doubleNovalue);
                    sxyData.setSample(i, j, 0, doubleNovalue);
                } else {
                    sxxData.setSample(
                            i,
                            j,
                            0,
                            ((elevationIter.getSampleDouble(i, j + 1, 0) - 2 * elevationIter.getSampleDouble(i, j, 0) + elevationIter
                                    .getSampleDouble(i, j - 1, 0)) / disXX));
                    syyData.setSample(
                            i,
                            j,
                            0,
                            ((elevationIter.getSampleDouble(i + 1, j, 0) - 2 * elevationIter.getSampleDouble(i, j, 0) + elevationIter
                                    .getSampleDouble(i - 1, j, 0)) / disYY));
                    sxyData.setSample(
                            i,
                            j,
                            0,
                            0.25 * ((elevationIter.getSampleDouble(i + 1, j + 1, 0)
                                    - elevationIter.getSampleDouble(i + 1, j - 1, 0)
                                    - elevationIter.getSampleDouble(i - 1, j + 1, 0) + elevationIter.getSampleDouble(i - 1,
                                    j - 1, 0)) / (xRes * yRes)));
                }
            }
            pm.worked(1);
        }
        pm.done();

        /*
         * calculate curvatures
         */
        pm.beginTask(msg.message("curvatures.calculating"), nRows - 2);
        for( int j = 1; j < nRows - 1; j++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int i = 1; i < nCols - 1; i++ ) {
                if (isNovalue(elevationIter.getSampleDouble(i, j, 0))) {
                    plan = doubleNovalue;
                    tang = doubleNovalue;
                    prof = doubleNovalue;

                } else {
                    double sxSample = sxData.getSampleDouble(i, j, 0);
                    double sySample = syData.getSampleDouble(i, j, 0);
                    double p = Math.pow(sxSample, 2.0) + Math.pow(sySample, 2.0);
                    double q = p + 1;
                    if (p == 0.0) {
                        plan = 0.0;
                        tang = 0.0;
                        prof = 0.0;
                    } else {
                        double sxxSample = sxxData.getSampleDouble(i, j, 0);
                        double sxySample = sxyData.getSampleDouble(i, j, 0);
                        double syySample = syyData.getSampleDouble(i, j, 0);
                        plan = (sxxSample * Math.pow(sySample, 2.0) - 2 * sxySample * sxSample * sySample + syySample
                                * Math.pow(sxSample, 2.0))
                                / (Math.pow(p, 1.5));
                        tang = (sxxSample * Math.pow(sySample, 2.0) - 2 * sxySample * sxSample * sySample + syySample
                                * Math.pow(sxSample, 2.0))
                                / (p * Math.pow(q, 0.5));
                        prof = (sxxSample * Math.pow(sxSample, 2.0) + 2 * sxySample * sxSample * sySample + syySample
                                * Math.pow(sySample, 2.0))
                                / (p * Math.pow(q, 1.5));
                    }

                }
                profWR.setSample(i, j, 0, prof);
                tangWR.setSample(i, j, 0, tang);
                planWR.setSample(i, j, 0, plan);
            }
            pm.worked(1);
        }
        pm.done();

        if (isCanceled(pm)) {
            return;
        }
        outProf = CoverageUtilities.buildCoverage("prof_curvature", profWR, regionMap, inElev.getCoordinateReferenceSystem());
        outPlan = CoverageUtilities.buildCoverage("plan_curvature", planWR, regionMap, inElev.getCoordinateReferenceSystem());
        outTang = CoverageUtilities.buildCoverage("tang_curvature", tangWR, regionMap, inElev.getCoordinateReferenceSystem());
    }
}
