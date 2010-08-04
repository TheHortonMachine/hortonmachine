/* JGrass - Free Open Source Java GIS http://www.jgrass.org 
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
package org.jgrasstools.hortonmachine.modules.demmanipulation.markoutlets;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
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
@Description("Marks all the outlets of the considered region on the drainage directions map with the conventional value 10.")
@Author(name = "Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com")
@Keywords("Outlets, Dem, Raster")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Markoutlets extends JGTModel {
    /*
     * EXTERNAL VARIABLES
     */
    @Description("The map of flow direction.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of markoutlet.")
    @Out
    public GridCoverage2D outFlow = null;

    /*
     * INTERNAL VARIABLES
     */
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() {
        if (!concatOr(outFlow == null, doReset)) {
            return;
        }

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        CoverageUtilities.setNovalueBorder(flowWR);
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);

        WritableRaster mflowWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("markoutlets.working"), 2 * nRows); //$NON-NLS-1$

        int[] punto = new int[2];
        int[] oldpunto = new int[2];
        for( int i = 0; i < nRows; i++ ) {
            for( int j = 0; j < nCols; j++ ) {
                double value = flowIter.getSampleDouble(j, i, 0);
                if (!isNovalue(value)) {
                    mflowWR.setSample(j, i, 0, value);
                } else {
                    mflowWR.setSample(j, i, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }

        for( int i = 0; i < nRows; i++ ) {
            for( int j = 0; j < nCols; j++ ) {
                punto[0] = j;
                punto[1] = i;

                double flowSample = flowIter.getSampleDouble(punto[0], punto[1], 0);
                ModelsEngine prova = new ModelsEngine();
                ModelsEngine prova1 = new ModelsEngine();

                if (prova.isSourcePixel(flowIter, punto[0], punto[1])) {
                    oldpunto[0] = punto[0];
                    oldpunto[1] = punto[1];

                    while( flowSample < 9.0 && (!isNovalue(flowSample)) ) {
                        oldpunto[0] = punto[0];
                        oldpunto[1] = punto[1];
                        if (!prova1.go_downstream(punto, flowSample)) {
                            return;
                        }
                        flowSample = flowIter.getSampleDouble(punto[0], punto[1], 0);

                    }
                    if (flowSample != 10.0)
                        mflowWR.setSample(oldpunto[0], oldpunto[1], 0, 10.0);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outFlow = CoverageUtilities.buildCoverage("markoutlet", mflowWR, regionMap, inFlow.getCoordinateReferenceSystem());
    }

}
