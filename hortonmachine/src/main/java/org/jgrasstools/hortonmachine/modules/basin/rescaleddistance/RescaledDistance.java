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
package org.jgrasstools.hortonmachine.modules.basin.rescaleddistance;

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
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.i18n.MessageHandler;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IHMProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
/**
 * <p>
 * The openmi compliant representation of the rescaleddistance model. It
 * calculates the rescaled distance of each pixel from the outlet. *
 * <p>
 * x' = x_c + r x_h
 * </p>
 * <p>
 * where: x_c is the distance along the channels,
 * </p>
 * <p>
 * r = c / c_h
 * </p>
 * <p>
 * the ratio between the speed in the channel state, and the speed in the
 * hillslopes, and x_h the distance along the hillslopes.
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>the map of the drainage directions (-flow);</LI>
 * <LI>the file containing the net (-net);
 * <LI>the channel-overland ratio (-number);</LI>
 * </LI>
 * </OL>
 * <P></DD>
 * <DT><STRONG>Returns:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>the map of the rescaled distances (-rdist);</LI>
 * </OL>
 * <P></DD>
 * Usage: h.rescaleddistance --igrass-net net --igrass-flow flow --ograss-rdist
 * rdist --number number
 * </p>
 * <p>
 * Note: The program requests also the ratio between speed in the channel and
 * speed in hillslopes. The speed in channels is always greater than that in
 * hillslopes.
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini
 *         Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo
 */
@Description("Calculates the rescaled distance of each pixel from the outlet")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Basin, Geomorphology")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class RescaledDistance extends JGTModel {

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of the network.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Role(Role.PARAMETER)
    @Description("Ratio between the velocity in the channel and in the hillslope.")
    @In
    public double pRatio = 0;

    @Description("The map of the rescaled distances.")
    @Out
    public GridCoverage2D outRescaled = null;

    private MessageHandler msg = MessageHandler.getInstance();
    private ModelsEngine modelsEngine = new ModelsEngine();

    @Execute
    public void process() {
        if (!concatOr(outRescaled == null, doReset)) {
            return;
        }

        HashMap<String, Double> regionMap = CoverageUtilities
                .getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);
        double yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage flowRI = inFlow.getRenderedImage();
        RenderedImage netRI = inNet.getRenderedImage();
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);
        RandomIter netIter = RandomIterFactory.create(netRI, null);

        WritableRaster rescaledWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows,
                null, null, null);
        WritableRandomIter rescaledIter = RandomIterFactory.createWritable(rescaledWR, null);

        int[] flow = new int[2];
        double count = 0.0, a = 0.0;

        // grid contains the dimension of pixels according with flow directions
        double[] grid = new double[11];

        grid[0] = grid[9] = grid[10] = 0.0;
        grid[1] = grid[5] = Math.abs(xRes);
        grid[3] = grid[7] = Math.abs(yRes);
        grid[2] = grid[4] = grid[6] = grid[8] = Math.sqrt(xRes * xRes + yRes * yRes);

        // FluidUtils.setNovalueBorder(flowData);

        pm.beginTask(msg.message("rescaleddistance.calculating"), nRows);
        for( int j = 0; j < nRows; j++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int i = 0; i < nCols; i++ ) {
                count = 0.0;
                flow[0] = i;
                flow[1] = j;
                if (!isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    while( netIter.getSampleDouble(flow[0], flow[1], 0) != 2.0
                            && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                            && !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0)) ) {
                        a = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        count += grid[(int) a] * pRatio;
                        if (!modelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0],
                                flow[1], 0)))
                            throw new ModelsIllegalargumentException(
                                    "Error while going downstream!", this.getClass()
                                            .getSimpleName());
                    }
                    while( flowIter.getSampleDouble(flow[0], flow[1], 0) != 10.0
                            && !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0)) ) {
                        a = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        count = count + grid[(int) a];
                        if (!modelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0],
                                flow[1], 0)))
                            throw new ModelsIllegalargumentException(
                                    "Error while going downstream!", this.getClass()
                                            .getSimpleName());
                    }
                    rescaledIter.setSample(i, j, 0, count);

                } else if (isNovalue(flowIter.getSampleDouble(i, j, 0))) {
                    rescaledIter.setSample(i, j, 0, doubleNovalue);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outRescaled = CoverageUtilities.buildCoverage("RescaledDistance", rescaledWR, regionMap,
                inFlow.getCoordinateReferenceSystem());
    }
}
