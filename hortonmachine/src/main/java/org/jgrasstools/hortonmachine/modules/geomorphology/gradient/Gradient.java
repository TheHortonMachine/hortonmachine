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
package org.jgrasstools.hortonmachine.modules.geomorphology.gradient;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import org.jgrasstools.gears.i18n.MessageHandler;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
/**
 * <p>
 * The openmi compliant representation of the gradient model. Calculates the
 * gradient in each point of the map,
 * </p>
 * <p>
 * It estimate the gradient with a finite difference formula:
 * 
 * <pre>
 *  p=&radic{f<sub>x</sub>&sup2;+f<sub>y</sub>&sup2;}
 * f<sub>x</sub>=(f(x+1,y)-f(x-1,y))/(2 &#916 x) 
 * f<sub>y</sub>=(f(x,y+1)-f(x,y-1))/(2 &#916 y)
 * </pre>
 * 
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>the matrix of elevations (-pit);</LI>
 * </OL>
 * <P></DD>
 * <DT><STRONG>Returns:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>matrix of the gradients (-gradient);</LI>
 * </OL>
 * <P></DD> Usage: h.gradient --igrass-pit pit --ograss-gradient gradient
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini
 *         Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo
 */
public class Gradient extends JGTModel {
    /*
     * EXTERNAL VARIABLES
     */
    // input
    @Description("The digital elevation model (DEM).")
    @In
    public GridCoverage2D inDem = null;
    
    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of gradient.")
    @Out
    public GridCoverage2D outSlope = null;

    /*
     * INTERNAL VARIABLES
     */
    private MessageHandler msg = MessageHandler.getInstance();

    /**
     * Computes the gradient algoritm. p=f_{x}^{2}+f_{y}^{2}
     */
    @Execute
    public void process() {
        if (!concatOr(outSlope == null, doReset)) {
            return;
        }
        
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDem);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);
        double yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage elevationRI = inDem.getRenderedImage();
        RandomIter elevationIter = RandomIterFactory.create(elevationRI, null);

        WritableRaster gradientWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("gradient.working"), nRows);
        for( int y = 1; y < nRows - 1; y++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int x = 1; x < nCols - 1; x++ ) {
                // extract the value to use for the algoritm. It is the finite difference approach.
                double elevIJ = elevationIter.getSampleDouble(x, y, 0);
                double elevIJipre = elevationIter.getSampleDouble(x - 1, y, 0);
                double elevIJipost = elevationIter.getSampleDouble(x + 1, y, 0);
                double elevIJjpre = elevationIter.getSampleDouble(x, y - 1, 0);
                double elevIJjpost = elevationIter.getSampleDouble(x, y + 1, 0);
                if (isNovalue(elevIJ) || isNovalue(elevIJipre) || isNovalue(elevIJipost) || isNovalue(elevIJjpre) || isNovalue(elevIJjpost)) {
                    gradientWR.setSample(x, y, 0, doubleNovalue);
                } else if (!isNovalue(elevIJ) && !isNovalue(elevIJipre) && !isNovalue(elevIJipost) && !isNovalue(elevIJjpre) && !isNovalue(elevIJjpost)) {
                    double xGrad = 0.5 * (elevIJipost - elevIJipre) / xRes;
                    double yGrad = 0.5 * (elevIJjpre - elevIJjpost) / yRes;
                    double grad = Math.sqrt(Math.pow(xGrad, 2) + Math.pow(yGrad, 2));
                    gradientWR.setSample(x, y, 0, grad);
                } else {
                    throw new IllegalArgumentException("Error in gradient");
                }
            }
            pm.worked(1);
        }
        pm.done();

        outSlope = CoverageUtilities.buildCoverage("gradient", gradientWR, regionMap, inDem.getCoordinateReferenceSystem());
    }

}
