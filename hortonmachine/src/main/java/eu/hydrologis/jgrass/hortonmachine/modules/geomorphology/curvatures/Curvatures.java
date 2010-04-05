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
package eu.hydrologis.jgrass.hortonmachine.modules.geomorphology.curvatures;

import static eu.hydrologis.jgrass.hortonmachine.libs.models.HMConstants.doubleNovalue;
import static eu.hydrologis.jgrass.hortonmachine.libs.models.HMConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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
 * The openmi compliant representation of the aspect model. It estimates the longitudinal, normal
 * and planar curvatures for each site through a finite difference schema.
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>the map of elevations (-pit);</LI>
 * </OL>
 * <P></DD>
 * <DT><STRONG>Returns:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>the map of longitudinal curvatures (-prof);</LI>
 * <LI>the map of normal (or tangent) curvatures (-tang);</LI>
 * <LI>the file containing the matrix of planar curvatures (--plan);</LI>
 * </OL>
 * <P></DD> Usage: h.curvatures --igrass-pit pit --ograss-prof prof --ograss-plan plan --ograss-tang
 * tang
 * </p>
 * <p>
 * Note: The planar and normal (or tangent) curvatures are proportional to each other. To function,
 * the program uses a matrix in input with a NOVALUE boundary and as a rule it places the curve
 * equal to zero on the catchment boundary.<BR>
 * <BR>
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini Andrea, Franceschi
 *         Silvia, Pisoni Silvano, Rigon Riccardo,
 */
public class Curvatures extends HMModel {
    /*
     * EXTERNAL VARIABLES
     */
    // input
    @Description("The digital elevation model (DEM).")
    @In
    public GridCoverage2D inDem = null;
    
    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

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

    /*
     * INTERNAL VARIABLES
     */
    private MessageHandler msg = MessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outProf == null, doReset)) {
            return;
        }
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDem);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);
        double yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage elevationRI = inDem.getRenderedImage();
        RandomIter elevationIter = RandomIterFactory.create(elevationRI, null);

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
                    sxData.setSample(x, y, 0, 0.5 * (elevationIter.getSampleDouble(x, y + 1, 0) - elevationIter.getSampleDouble(x, y - 1, 0)) / xRes);
                    syData.setSample(x, y, 0, 0.5 * (elevationIter.getSampleDouble(x + 1, y, 0) - elevationIter.getSampleDouble(x - 1, y, 0)) / yRes);
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
                    sxxData.setSample(i, j, 0, ((elevationIter.getSampleDouble(i, j + 1, 0) - 2 * elevationIter.getSampleDouble(i, j, 0) + elevationIter.getSampleDouble(i, j - 1, 0)) / disXX));
                    syyData.setSample(i, j, 0, ((elevationIter.getSampleDouble(i + 1, j, 0) - 2 * elevationIter.getSampleDouble(i, j, 0) + elevationIter.getSampleDouble(i - 1, j, 0)) / disYY));
                    sxyData.setSample(i, j, 0, 0.25 * ((elevationIter.getSampleDouble(i + 1, j + 1, 0) - elevationIter.getSampleDouble(i + 1, j - 1, 0)
                            - elevationIter.getSampleDouble(i - 1, j + 1, 0) + elevationIter.getSampleDouble(i - 1, j - 1, 0)) / (xRes * yRes)));
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
                        plan = (sxxSample * Math.pow(sySample, 2.0) - 2 * sxySample * sxSample * sySample + syySample * Math.pow(sxSample, 2.0)) / (Math.pow(p, 1.5));
                        tang = (sxxSample * Math.pow(sySample, 2.0) - 2 * sxySample * sxSample * sySample + syySample * Math.pow(sxSample, 2.0)) / (p * Math.pow(q, 0.5));
                        prof = (sxxSample * Math.pow(sxSample, 2.0) + 2 * sxySample * sxSample * sySample + syySample * Math.pow(sySample, 2.0)) / (p * Math.pow(q, 1.5));
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
        outProf = CoverageUtilities.buildCoverage("prof_curvature", profWR, regionMap, inDem.getCoordinateReferenceSystem());
        outPlan = CoverageUtilities.buildCoverage("plan_curvature", planWR, regionMap, inDem.getCoordinateReferenceSystem());
        outTang = CoverageUtilities.buildCoverage("tang_curvature", tangWR, regionMap, inDem.getCoordinateReferenceSystem());
    }
}
