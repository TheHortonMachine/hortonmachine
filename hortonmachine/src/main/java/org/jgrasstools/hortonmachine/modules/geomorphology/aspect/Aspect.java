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
package org.jgrasstools.hortonmachine.modules.geomorphology.aspect;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Calculates the aspect considering the zero toward the north and the rotation angle counterclockwise.")
@Documentation("Aspect.html")
@Author(name = "Andrea Antonello, Erica Ghesla, Rigon Riccardo, Pisoni Silvano, Andrea Cozzini", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Geomorphology, DrainDir, FlowDirections")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("aspect")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Aspect extends JGTModel {
    @Description("The map of the digital elevation model (DEM).")
    @In
    public GridCoverage2D inElev = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("Switch to define whether create the output map in degrees (default) or radiants.")
    @In
    public boolean doRadiants = false;

    @Description("Switch to define whether the output map values should be rounded (might make sense in the case of degree maps).")
    @In
    public boolean doRound = false;

    @Description("The map of aspect.")
    @Out
    public GridCoverage2D outAspect = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private double radtodeg = 360.0 / (2 * PI);

    @Execute
    public void process() throws Exception {
        if (!concatOr(outAspect == null, doReset)) {
            return;
        }
        if (doRadiants) {
            radtodeg = 1.0;
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);
        double yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage elevationRI = inElev.getRenderedImage();
        RandomIter elevationIter = RandomIterFactory.create(elevationRI, null);

        WritableRaster aspectWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);
        WritableRandomIter aspectIter = RandomIterFactory.createWritable(aspectWR, null);

        // the value of the x and y derivative
        double aData = 0.0;
        double bData = 0.0;

        pm.beginTask(msg.message("aspect.calculating"), rows);

        // Cycling into the valid region.
        for( int j = 1; j < rows - 1; j++ ) {
            for( int i = 1; i < cols - 1; i++ ) {
                // calculate the y derivative
                double centralValue = elevationIter.getSampleDouble(i, j, 0);

                if (!isNovalue(centralValue)) {
                    double valuePostJ = elevationIter.getSampleDouble(i, j + 1, 0);
                    double valuePreJ = elevationIter.getSampleDouble(i, j - 1, 0);
                    if (!isNovalue(valuePostJ) && !isNovalue(valuePreJ)) {
                        aData = atan((valuePreJ - valuePostJ) / (2 * yRes));
                    }
                    if (isNovalue(valuePreJ) && (!isNovalue(valuePreJ))) {
                        aData = atan((centralValue - valuePostJ) / (yRes));
                    }
                    if (!isNovalue(valuePreJ) && isNovalue(valuePostJ)) {
                        aData = atan((valuePreJ - centralValue) / (yRes));
                    }
                    if (isNovalue(valuePreJ) && isNovalue(valuePostJ)) {
                        aData = doubleNovalue;
                    }
                    // calculate the x derivative
                    double valuePreI = elevationIter.getSampleDouble(i - 1, j, 0);
                    double valuePostI = elevationIter.getSampleDouble(i + 1, j, 0);
                    if (!isNovalue(valuePreI) && !isNovalue(valuePostI)) {
                        bData = atan((valuePreI - valuePostI) / (2 * xRes));
                    }
                    if (isNovalue(valuePreI) && !isNovalue(valuePostI)) {
                        bData = atan((centralValue - valuePostI) / (xRes));
                    }
                    if (!isNovalue(valuePreI) && isNovalue(valuePostI)) {
                        bData = atan((valuePreI - centralValue) / (xRes));
                    }
                    if (isNovalue(valuePreI) && isNovalue(valuePostI)) {
                        bData = doubleNovalue;
                    }

                    double delta = 0.0;
                    double aspect = doubleNovalue;
                    // calculate the aspect value
                    if (aData < 0 && bData > 0) {
                        delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                        aspect = delta * radtodeg;
                    } else if (aData > 0 && bData > 0) {
                        delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                        aspect = (PI - delta) * radtodeg;
                    } else if (aData > 0 && bData < 0) {
                        delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                        aspect = (PI + delta) * radtodeg;
                    } else if (aData < 0 && bData < 0) {
                        delta = acos(sin(abs(aData)) * cos(abs(bData)) / (sqrt(1 - pow(cos(aData), 2) * pow(cos(bData), 2))));
                        aspect = (2 * PI - delta) * radtodeg;
                    } else if (aData == 0 && bData > 0) {
                        aspect = (PI / 2.) * radtodeg;
                    } else if (aData == 0 && bData < 0) {
                        aspect = (PI * 3. / 2.) * radtodeg;
                    } else if (aData > 0 && bData == 0) {
                        aspect = PI * radtodeg;
                    } else if (aData < 0 && bData == 0) {
                        aspect = 2.0 * PI * radtodeg;
                    } else if (aData == 0 && bData == 0) {
                        aspect = 0.0;
                    }
                    if (doRound) {
                        aspect = round(aspect);
                    }
                    aspectIter.setSample(i, j, 0, aspect);
                } else {
                    aspectIter.setSample(i, j, 0, doubleNovalue);
                }

            }
            pm.worked(1);
        }
        pm.done();

        CoverageUtilities.setNovalueBorder(aspectWR);
        outAspect = CoverageUtilities.buildCoverage("aspect", aspectWR, regionMap, inElev.getCoordinateReferenceSystem());
    }

}
