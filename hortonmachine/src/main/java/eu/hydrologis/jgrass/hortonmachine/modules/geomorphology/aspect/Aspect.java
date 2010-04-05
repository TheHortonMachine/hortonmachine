/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.hortonmachine.modules.geomorphology.aspect;

import static eu.hydrologis.jgrass.hortonmachine.libs.models.HMConstants.doubleNovalue;
import static eu.hydrologis.jgrass.hortonmachine.libs.models.HMConstants.isNovalue;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

import org.geotools.coverage.grid.GridCoverage2D;

import eu.hydrologis.jgrass.hortonmachine.i18n.MessageHandler;
import eu.hydrologis.jgrass.hortonmachine.libs.models.HMModel;
import eu.hydrologis.jgrass.hortonmachine.libs.monitor.DummyProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.libs.monitor.IHMProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.utils.coverage.CoverageUtilities;

/**
 * <p>
 * The openmi compliant representation of the aspect model. Generates raster map layers of aspect
 * from a raster map layer of true elevation values. The value of aspect is calculated
 * counterclockwise from north.
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG><BR>
 * </DT>
 * <OL>
 * <LI>the depitted map (-pit)</LI>
 * </OL>
 * <P>
 * </DD>
 * <DT><STRONG>Returns:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>the map with the aspect (-aspect)</LI>
 * </OL>
 * <P></DD>
 * </p>
 * <p>
 * Usage: h.aspect --igrass-pit pit --ograss-aspect aspect
 * </p>
 * <p>
 * With color table: h.aspect --igrass-pit pit --ograss-aspect aspect --ocolor-color aspect
 * </p>
 * <p>
 * Note: Due to the difficult existing calculating the aspect on the borders of the region, in this
 * cases the direction of the gradient is assumed to be the maximum slope gradient.
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini Andrea, Franceschi
 *         Silvia, Pisoni Silvano, Rigon Riccardo
 */
public class Aspect extends HMModel {
    @Description("The digital elevation model (DEM).")
    @In
    public GridCoverage2D inDem = null;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Description("Switch to define whether create the output map in degrees (default) or radiants.")
    @In
    public boolean doRadiants = false;

    @Description("Switch to define whether the output map values should be rounded (might make sense in the case of degree maps).")
    @In
    public boolean doRound = false;

    @Description("The map of aspect.")
    @Out
    public GridCoverage2D outAspect = null;

    private MessageHandler msg = MessageHandler.getInstance();

    private double radtodeg = 360.0 / (2 * PI);

    @Execute
    public void process() throws Exception {
        if (!concatOr(outAspect == null, doReset)) {
            return;
        }
        if (doRadiants) {
            radtodeg = 1.0;
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDem);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);
        double yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage elevationRI = inDem.getRenderedImage();
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
        outAspect = CoverageUtilities.buildCoverage("aspect", aspectWR, regionMap, inDem.getCoordinateReferenceSystem());
    }

}
