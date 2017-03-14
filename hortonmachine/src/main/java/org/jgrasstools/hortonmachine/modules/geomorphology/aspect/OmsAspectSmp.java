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
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_doRadiants_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_doRound_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSASPECT_outAspect_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.stream.Stream;

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
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.GridNodeMultiProcessing;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.StreamUtils;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description(OMSASPECT_DESCRIPTION)
@Documentation(OMSASPECT_DOCUMENTATION)
@Author(name = OMSASPECT_AUTHORNAMES, contact = OMSASPECT_AUTHORCONTACTS)
@Keywords(OMSASPECT_KEYWORDS)
@Label(OMSASPECT_LABEL)
@Name(OMSASPECT_NAME)
@Status(OMSASPECT_STATUS)
@License(OMSASPECT_LICENSE)
public class OmsAspectSmp extends GridNodeMultiProcessing {
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
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RandomIter elevationIter = CoverageUtilities.getRandomIterator(inElev);

        WritableRaster aspectWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);

        long t0 = System.currentTimeMillis();
        long t1 = doNormal(cols, rows, xRes, yRes, elevationIter, aspectWR, t0);

        long t2 = doInStream(cols, rows, xRes, yRes, elevationIter, aspectWR, t1);

        doFalko(cols, rows, aspectWR, t2);

        CoverageUtilities.setNovalueBorder(aspectWR);
        outAspect = CoverageUtilities.buildCoverage("aspect", aspectWR, regionMap, inElev.getCoordinateReferenceSystem());
    }

    private void doFalko( int cols, int rows, WritableRaster aspectWR, long t2 ) throws Exception {
        WritableRandomIter aspectIter11 = RandomIterFactory.createWritable(aspectWR, null);
        // pm.beginTask(msg.message("aspect.calculating"), rows * cols);
        processGridNodes(inElev, gridNode -> {
            double aspect = calculate(gridNode, radtodeg, doRound);
            aspectIter11.setSample(gridNode.col, gridNode.row, 0, aspect);
            // pm.worked(1);
        });
        // pm.done();
        long t3 = System.currentTimeMillis();
        System.out.println("FALKO = " + (t3 - t2));
    }

    private long doNormal( int cols, int rows, double xRes, double yRes, RandomIter elevationIter, WritableRaster aspectWR,
            long t0 ) {
        WritableRandomIter aspectIter1 = RandomIterFactory.createWritable(aspectWR, null);
        // pm.beginTask(msg.message("aspect.calculating"), rows);
        // Cycling into the valid region.
        for( int r = 1; r < rows - 1; r++ ) {
            for( int c = 1; c < cols - 1; c++ ) {
                GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, c, r);
                double aspect = calculate(node, radtodeg, doRound);
                aspectIter1.setSample(c, r, 0, aspect);
            }
            // pm.worked(1);
        }
        // pm.done();
        long t1 = System.currentTimeMillis();
        System.out.println("NORMAL = " + (t1 - t0));
        return t1;
    }

    private long doInStream( int cols, int rows, double xRes, double yRes, RandomIter elevationIter, WritableRaster aspectWR,
            long t1 ) {
        int procNum = Runtime.getRuntime().availableProcessors();
        int step = (int) (rows / (double) procNum);
        double[] range2Bins = NumericsUtilities.range2Bins(1, rows - 1, step, false);
        double[][] runs = new double[range2Bins.length - 1][2];
        for( int i = 0; i < range2Bins.length - 1; i++ ) {
            runs[i][0] = range2Bins[i];
            runs[i][1] = range2Bins[i + 1];
        }

        // pm.beginTask(msg.message("aspect.calculating"), rows);

        WritableRandomIter aspectIter = RandomIterFactory.createWritable(aspectWR, null);
        Stream<double[]> boundsArray = StreamUtils.fromArray(runs);
        boundsArray.parallel().forEach(bound -> {
            pm.message("WORKING BETWEEN: " + (int) bound[0] + " and " + (int) (bound[1] - 1));
            for( int r = (int) bound[0]; r < bound[1] - 1; r++ ) {
                for( int c = 1; c < cols - 1; c++ ) {
                    GridNode node = new GridNode(elevationIter, cols, rows, xRes, yRes, c, r);
                    double aspect = calculate(node, radtodeg, doRound);
                    aspectIter.setSample(c, r, 0, aspect);
                }
                // pm.worked(1);
            }
        });
        // pm.done();
        long t2 = System.currentTimeMillis();
        System.out.println("STREAM = " + (t2 - t1));
        return t2;
    }

    public static void main( String[] args ) throws Exception {
        OmsAspectSmp s = new OmsAspectSmp();
        s.pm = new LogProgressMonitor();
        s.inElev = OmsRasterReader.readRaster("/media/hydrologis/Samsung_T3/DATI/DTM_calvello/dtm_all.asc");
        s.process();

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
    public static double calculate( GridNode node, double radtodeg, boolean doRound ) {
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
