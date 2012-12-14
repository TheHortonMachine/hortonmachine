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
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
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

    @Execute
    public void process() throws Exception {
        if (!concatOr(outAspect == null, doReset)) {
            return;
        }
        checkNull(inElev);
        double radtodeg = NumericsUtilities.RADTODEG;
        if (doRadiants) {
            radtodeg = 1.0;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter elevationIter = CoverageUtilities.getRandomIterator(inElev);

        WritableRaster aspectWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);
        WritableRandomIter aspectIter = RandomIterFactory.createWritable(aspectWR, null);

        pm.beginTask(msg.message("aspect.calculating"), rows);

        // Cycling into the valid region.
        for( int r = 1; r < rows - 1; r++ ) {
            for( int c = 1; c < cols - 1; c++ ) {
                GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, c, r);
                double aspect = calculateAspect(node, radtodeg, doRound);
                aspectIter.setSample(c, r, 0, aspect);
            }
            pm.worked(1);
        }
        pm.done();

        CoverageUtilities.setNovalueBorder(aspectWR);
        outAspect = CoverageUtilities.buildCoverage("aspect", aspectWR, regionMap, inElev.getCoordinateReferenceSystem());
    }

    /**
     * Calculates the aspect in a given {@link GridNode}.
     * 
     * @param node the current grid node.
     * @param radtodeg radiants to degrees conversion factor. Use {@link NumericsUtilities#RADTODEG} if you 
     *                 want degrees, use 1 if you want radiants. 
     * @param doRound if <code>true</code>, values are round to integer.
     * @return the value of aspect.
     */
    public static double calculateAspect( GridNode node, double radtodeg, boolean doRound ) {
        double aspect = doubleNovalue;
        // the value of the x and y derivative
        double aData = 0.0;
        double bData = 0.0;
        double xRes = node.xRes;
        double yRes = node.yRes;
        double centralValue = node.elevation;
        double nValue = node.getNorthElev();
        double sValue = node.getSouthElev();
        double wValue = node.getWestElev();
        double eValue = node.getEastElev();

        if (!isNovalue(centralValue)) {
            boolean sIsNovalue = isNovalue(sValue);
            boolean nIsNovalue = isNovalue(nValue);
            boolean wIsNovalue = isNovalue(wValue);
            boolean eIsNovalue = isNovalue(eValue);

            if (!sIsNovalue && !nIsNovalue) {
                aData = atan((nValue - sValue) / (2 * yRes));
            } else if (nIsNovalue && !sIsNovalue) {
                aData = atan((centralValue - sValue) / (yRes));
            } else if (!nIsNovalue && sIsNovalue) {
                aData = atan((nValue - centralValue) / (yRes));
            } else if (nIsNovalue && sIsNovalue) {
                aData = doubleNovalue;
            } else {
                // can't happen
                throw new RuntimeException();
            }
            if (!wIsNovalue && !eIsNovalue) {
                bData = atan((wValue - eValue) / (2 * xRes));
            } else if (wIsNovalue && !eIsNovalue) {
                bData = atan((centralValue - eValue) / (xRes));
            } else if (!wIsNovalue && eIsNovalue) {
                bData = atan((wValue - centralValue) / (xRes));
            } else if (wIsNovalue && eIsNovalue) {
                bData = doubleNovalue;
            } else {
                // can't happen
                throw new RuntimeException();
            }

            double delta = 0.0;
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
            } else if (isNovalue(aData) || isNovalue(bData)) {
                aspect = doubleNovalue;
            } else {
                // can't happen
                throw new RuntimeException();
            }
            if (doRound) {
                aspect = round(aspect);
            }
        }
        return aspect;
    }

}
