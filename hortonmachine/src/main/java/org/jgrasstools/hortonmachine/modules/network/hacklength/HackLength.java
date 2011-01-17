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
package org.jgrasstools.hortonmachine.modules.network.hacklength;

import static java.lang.Math.abs;
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
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

/**
 * <p>
 * The openmi compliant representation of the flow model. It calculates the Hack
 * quantities, namely, assigned a point in a basin, the projection on the plane
 * of the distance from the watershed measured along the net (until it exists)
 * and then, proceeding again from valley upriver, along the maximal slope
 * lines. For each net confluence, the direction of the tributary with maximal
 * contributing area is chosen. If the tributaries have the same area, one of
 * the two directions is chosen at random.
 * </p>
 * <dt><strong>Inputs: </strong></dt>
 * <ol>
 * <li>the map containing the drainage directions (-flow);</li>
 * <li>the map containing the contributing areas (-tca);</li>
 * </ol>
 * <dt><strong>Returns:<br>
 * </strong></dt> <dd>
 * <ol>
 * <li>the map of the Hack distances (-hackl)</li>
 * </ol>
 * <p></dd>
 * <p>
 * Usage: h.hacklength --igrass-flow flow --igrass-tca tca --ograss-hackl hackl
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini
 *         Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo
 */
@Description("calculates the Hack" + " quantities, namely, assigned a point in a basin, the projection on the plane"
        + " of the distance from the watershed measured along the net (until it exists)"
        + " and then, proceeding again from valley upriver, along the maximal slope" + " lines.")
@Author(name = "Erica Ghesla, Andrea Antonello, Franceschi Silvia", contact = "www.hydrologis.com")
@Keywords("Network, Hack")
@Label(JGTConstants.NETWORK)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class HackLength extends JGTModel {

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of tca.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of hack lengths.")
    @Out
    public GridCoverage2D outHacklength = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();
    

    private int nCols;

    private int nRows;

    private double xRes;

    private double yRes;

    private HashMap<String, Double> regionMap;

    @Execute
    public void process() {
        if (!concatOr(outHacklength == null, doReset)) {
            return;
        }

        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage flowRI = inFlow.getRenderedImage();
        RenderedImage tcaRI = inTca.getRenderedImage();
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);
        RandomIter tcaIter = RandomIterFactory.create(tcaRI, null);

        hacklength(flowIter, tcaIter);

    }

    private void hacklength( RandomIter flowIter, RandomIter tcaIter ) {

        int[] flow = new int[2];
        double oldir;
        double count = 0.0, maz;

        WritableRaster hacklengthWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter hacklengthIter = RandomIterFactory.createWritable(hacklengthWR, null);

        double[] grid = new double[11];
        grid[0] = grid[9] = grid[10] = 0;
        grid[1] = grid[5] = abs(xRes);
        grid[3] = grid[7] = abs(yRes);
        grid[2] = grid[4] = grid[6] = grid[8] = sqrt(xRes * xRes + yRes * yRes);

        pm.beginTask(msg.message("hacklength.calculating"), nRows); //$NON-NLS-1$
        for( int j = 0; j < nRows; j++ ) {
            for( int i = 0; i < nCols; i++ ) {
                if (isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    hacklengthIter.setSample(i, j, 0, doubleNovalue);
                } else {
                    flow[0] = i;
                    flow[1] = j;
                    if (ModelsEngine.isSourcePixel(flowIter, flow[0], flow[1])) {
                        count = 0;
                        maz = 1;
                        hacklengthIter.setSample(flow[0], flow[1], 0, count);
                        oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return;
                        while( (!isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0)) && flowIter.getSampleDouble(flow[0],
                                flow[1], 0) != 10.0)
                                && ModelsEngine.tcaMax(flowIter, tcaIter, hacklengthIter, flow, maz, count) ) {
                            count += grid[(int) oldir];
                            hacklengthIter.setSample(flow[0], flow[1], 0, count);
                            maz = tcaIter.getSampleDouble(flow[0], flow[1], 0);
                            oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                            if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return;
                        }
                        if (flowIter.getSampleDouble(flow[0], flow[1], 0) == 10) {
                            if (ModelsEngine.tcaMax(flowIter, tcaIter, hacklengthIter, flow, maz, count)) {
                                count += grid[(int) oldir];
                                hacklengthIter.setSample(flow[0], flow[1], 0, count);
                            }
                        }

                    }

                }
            }
            pm.worked(1);
        }
        pm.done();

        outHacklength = CoverageUtilities.buildCoverage("Hacklength", hacklengthWR, regionMap, inFlow
                .getCoordinateReferenceSystem());
    }

}
