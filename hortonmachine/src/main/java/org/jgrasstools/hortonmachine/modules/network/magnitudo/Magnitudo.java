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
package org.jgrasstools.hortonmachine.modules.network.magnitudo;


import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.libs.modules.ModelsEngine.go_downstream;
import static org.jgrasstools.gears.libs.modules.ModelsEngine.isSourcePixel;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("It calculates the magnitude of a basin, defined as the number of sources upriver with respect to every point.")
@Author(name = "Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo")
@Status(Status.DRAFT)
@License("GPL3")
public class Magnitudo extends JGTModel {

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of magnitudo.")
    @Out
    public GridCoverage2D outMag = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMag == null, doReset)) {
            return;
        }
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);

        WritableRaster magWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 0.0);
        if (magWR == null) {
            return;
        } else {
            magnitudo(flowIter, cols, rows, magWR);
            outMag = CoverageUtilities.buildCoverage("mag", magWR, regionMap, inFlow.getCoordinateReferenceSystem());

        }
    }

    public void magnitudo( RandomIter flowIter, int width, int height, WritableRaster magWR ) {

        int[] flow = new int[2];
        // get rows and cols from the active region
        int cols = width;
        int rows = height;
        RandomIter magIter = RandomIterFactory.create(magWR, null);
        pm.beginTask(msg.message("magnitudo.workingon"), rows * 2); //$NON-NLS-1$

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                // looks for the source
                if (isSourcePixel(flowIter, flow[0], flow[1])) {
                    magWR.setSample(flow[0], flow[1], 0, magIter.getSampleDouble(flow[0], flow[1], 0) + 1.0);
                    if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                        return;
                    while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                            && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10 ) {
                        magWR.setSample(flow[0], flow[1], 0, magIter.getSampleDouble(flow[0], flow[1], 0) + 1.0);
                        if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return;
                    }

                    if (flowIter.getSampleDouble(flow[0], flow[1], 0) == 10) {
                        magWR.setSample(flow[0], flow[1], 0, magIter.getSampleDouble(flow[0], flow[1], 0) + 1.0);
                    }
                }
            }
            pm.worked(1);
        }

        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                if (magIter.getSampleDouble(i, j, 0) == 0.0 && flowIter.getSampleDouble(i, j, 0) == 10.0) {
                    magWR.setSample(i, j, 0, 1.0);
                } else if (magIter.getSampleDouble(i, j, 0) == 0.0 && isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    magWR.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

}