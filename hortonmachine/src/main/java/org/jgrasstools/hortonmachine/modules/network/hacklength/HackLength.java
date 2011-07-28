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
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
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

@Description("Assigned a point in a basin calculates"
        + " the distance from the watershed measured along the net (until it exists)"
        + " and then, again from valley upriver, along the maximal slope.")
@Documentation("HackLength.html")
@Author(name = "Daniele Andreis, Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Network, HackLength3D, HackStream")
@Label(JGTConstants.NETWORK)
@Name("hacklength")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
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
        checkNull(inFlow, inTca);
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
                                flow[1], 0) != 10.0) && ModelsEngine.tcaMax(flowIter, tcaIter, hacklengthIter, flow, maz, count) ) {
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

        outHacklength = CoverageUtilities.buildCoverage("Hacklength", hacklengthWR, regionMap,
                inFlow.getCoordinateReferenceSystem());
    }

}
