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
package org.jgrasstools.hortonmachine.modules.geomorphology.ab;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import org.jgrasstools.gears.i18n.MessageHandler;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IHMProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
/**
 * <p>
 * The openmi compliant representation of the Ab model. It calculates the draining area per length
 * unit (A/b), where A is the total area and b is the length of the contour line which is assumed as
 * drained by the A area.
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>the map of planar curvatures (-plan);</LI>
 * <LI>the map with the total contributing areas (obtained with multitca or tca) (-tca);</LI>
 * </OL>
 * <P></DD>
 * <DT><STRONG>Returns:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>the map of the areas per length unit (-ab);</LI>
 * <LI>the map of the contour line (-b).</LI>
 * </OL>
 * <P></DD>
 * </p>
 * <p>
 * Usage: h.ab --igrass-plan plan --igrass-tca tca --ograss-ab ab --ograss-b b 0/1
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Rigon Riccardo
 */
public class Ab extends JGTModel {
    @Description("The map of the total contributing area.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The map of the planar curvatures.")
    @In
    public GridCoverage2D inPlan = null;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of alung.")
    @Out
    public GridCoverage2D outAb = null;
    
    @Description("The map of b.")
    @Out
    public GridCoverage2D outB = null;

    private MessageHandler msg = MessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outAb == null, doReset)) {
            return;
        }
        
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inTca);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);

        RenderedImage tcaRI = inTca.getRenderedImage();
        RandomIter tcaIter = RandomIterFactory.create(tcaRI, null);
        RenderedImage planRI = inPlan.getRenderedImage();
        RandomIter planIter = RandomIterFactory.create(planRI, null);

        WritableRaster alungWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, null);
        WritableRandomIter alungIter = RandomIterFactory.createWritable(alungWR, null);
        WritableRaster bWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, null);
        WritableRandomIter bIter = RandomIterFactory.createWritable(bWR, null);

        pm.beginTask(msg.message("ab.calculating"), nRows);
        for( int i = 0; i < nRows; i++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int j = 0; j < nCols; j++ ) {
                double planSample = planIter.getSampleDouble(j, i, 0);
                if (!isNovalue(planSample) && planSample != 0.0) {
                    if (xRes > 1 / planSample && planSample >= 0.0) {
                        bIter.setSample(j, i, 0, 0.1 * xRes);
                    } else if (xRes > Math.abs(1 / planSample) && planSample < 0.0) {
                        bIter.setSample(j, i, 0, xRes + 0.9 * xRes);
                    } else {
                        double bSample = 2 * Math.asin(xRes / (2 * (1 / planSample))) * (1 / planSample - xRes);
                        bIter.setSample(j, i, 0, bSample);
                        if (planSample >= 0.0 && bSample < 0.1 * xRes) {
                            bIter.setSample(j, i, 0, 0.1 * xRes);
                        }
                        if (planSample < 0.0 && bSample > (xRes + 0.9 * xRes)) {
                            bIter.setSample(j, i, 0, xRes + 0.9 * xRes);
                        }
                    }
                }
                if (planSample == 0.0) {
                    bIter.setSample(j, i, 0, xRes);
                }
                alungIter.setSample(j, i, 0, tcaIter.getSampleDouble(j, i, 0) * xRes * xRes / bIter.getSampleDouble(j, i, 0));
                if (isNovalue(planSample)) {
                    alungIter.setSample(j, i, 0, doubleNovalue);
                    bIter.setSample(j, i, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
        
        
        outAb = CoverageUtilities.buildCoverage("alung", alungWR, regionMap, inTca.getCoordinateReferenceSystem());
        outB = CoverageUtilities.buildCoverage("b", bWR, regionMap, inTca.getCoordinateReferenceSystem());
    }
}
