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
package org.hortonmachine.hmachine.modules.geomorphology.aspect;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.hortonmachine.gears.libs.modules.HMConstants.GEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.multiprocessing.GridNodeMultiProcessing;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

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

@Description(OmsAspect.OMSASPECT_DESCRIPTION)
@Documentation(OmsAspect.OMSASPECT_DOCUMENTATION)
@Author(name = OmsAspect.OMSASPECT_AUTHORNAMES, contact = OmsAspect.OMSASPECT_AUTHORCONTACTS)
@Keywords(OmsAspect.OMSASPECT_KEYWORDS)
@Label(OmsAspect.OMSASPECT_LABEL)
@Name(OmsAspect.OMSASPECT_NAME)
@Status(OmsAspect.OMSASPECT_STATUS)
@License(OmsAspect.OMSASPECT_LICENSE)
public class OmsAspect extends GridNodeMultiProcessing {
    @Description(OMSASPECT_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSASPECT_doRadiants_DESCRIPTION)
    @In
    public boolean doRadiants = false;

    @Description(OMSASPECT_doRound_DESCRIPTION)
    @In
    public boolean doRound = false;

    @Description(OMSASPECT_outAspect_DESCRIPTION)
    @Out
    public GridCoverage2D outAspect = null;

    public static final String OMSASPECT_DESCRIPTION = "Calculates the aspect considering zero towards the north and counterclockwise rotation.";
    public static final String OMSASPECT_DOCUMENTATION = "OmsAspect.html";
    public static final String OMSASPECT_KEYWORDS = "Geomorphology, OmsDrainDir, OmsFlowDirections";
    public static final String OMSASPECT_LABEL = GEOMORPHOLOGY;
    public static final String OMSASPECT_NAME = "aspect";
    public static final int OMSASPECT_STATUS = 40;
    public static final String OMSASPECT_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSASPECT_AUTHORNAMES = "Andrea Antonello, Erica Ghesla, Rigon Riccardo, Pisoni Silvano, Andrea Cozzini";
    public static final String OMSASPECT_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSASPECT_inElev_DESCRIPTION = "The map of the digital elevation model (DEM).";
    public static final String OMSASPECT_doRadiants_DESCRIPTION = "Switch to define whether create the output map in degrees (default) or radiants.";
    public static final String OMSASPECT_doRound_DESCRIPTION = "Switch to define whether the output map values should be rounded (might make sense in the case of degree maps).";
    public static final String OMSASPECT_outAspect_DESCRIPTION = "The map of aspect.";

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private double radtodeg = NumericsUtilities.RADTODEG;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outAspect == null, doReset)) {
            return;
        }
        checkNull(inElev);
        if (doRadiants) {
            radtodeg = 1.0;
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        WritableRaster aspectWR;
        if (doRound) {
            aspectWR = CoverageUtilities.createWritableRaster(cols, rows, Short.class, null, null);
        } else {
            aspectWR = CoverageUtilities.createWritableRaster(cols, rows, Float.class, null, null);
        }
        WritableRandomIter aspectIter = RandomIterFactory.createWritable(aspectWR, null);
        try {
            pm.beginTask(msg.message("aspect.calculating"), rows * cols);
            processGridNodes(inElev, gridNode -> {
                if (pm.isCanceled()) {
                    return;
                }
                double aspect = calculateAspect(gridNode, radtodeg, doRound);
                int col = gridNode.col;
                int row = gridNode.row;
                if (col == 0 || row == 0 || col == cols - 1 || row == rows - 1) {
                    aspectIter.setSample(col, row, 0, HMConstants.shortNovalue);
                } else {
                    if (doRound) {
                        aspectIter.setSample(col, row, 0, (short) aspect);
                    } else {
                        aspectIter.setSample(col, row, 0, aspect);
                    }
                }
                pm.worked(1);
            });
            pm.done();
        } finally {
            aspectIter.done();
        }
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
