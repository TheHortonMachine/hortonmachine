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
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.List;

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
import org.jgrasstools.gears.libs.modules.Direction;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

@Description("Assigned a point in a basin calculates"
        + " the distance from the watershed measured along the net (until it exists)"
        + " and then, again from valley upriver, along the maximal slope.")
@Documentation("HackLength.html")
@Author(name = "Antonello Andrea, Franceschi Silvia, Daniele Andreis,  Erica Ghesla, Cozzini Andrea,  Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com")
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

    @Description("The optional map of the elevation to work in 3D mode.")
    @In
    public GridCoverage2D inElevation = null;

    @Description("The map of hack lengths.")
    @Out
    public GridCoverage2D outHacklength = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int nCols;

    private int nRows;

    private double xRes;

    private double yRes;

    private RegionMap regionMap;

    @Execute
    public void process() {
        if (!concatOr(outHacklength == null, doReset)) {
            return;
        }
        checkNull(inFlow, inTca);

        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        nCols = regionMap.getCols();
        nRows = regionMap.getRows();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();

        RandomIter tcaIter = CoverageUtilities.getRandomIterator(inTca);

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);

        // if inElevation isn't null then work in 3d.
        RandomIter elevIter = null;
        if (inElevation != null) {
            elevIter = CoverageUtilities.getRandomIterator(inElevation);
        }

        hacklength(flowIter, tcaIter, elevIter);

        tcaIter.done();
        flowIter.done();
        if (elevIter != null) {
            elevIter.done();
        }

    }

    private void hacklength( RandomIter flowIter, RandomIter tcaIter, RandomIter elevIter ) {

        int[] flow = new int[2];
        int[] flow_p = new int[2];
        double oldir;
        double runningDistance = 0.0, maxTca;

        WritableRaster hacklengthWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter hacklengthIter = RandomIterFactory.createWritable(hacklengthWR, null);

        double[] grid = new double[11];
        grid[0] = grid[9] = grid[10] = 0;
        grid[1] = grid[5] = abs(xRes);
        grid[3] = grid[7] = abs(yRes);
        grid[2] = grid[4] = grid[6] = grid[8] = sqrt(xRes * xRes + yRes * yRes);

        pm.beginTask(msg.message("hacklength.calculating"), nRows); //$NON-NLS-1$
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                FlowNode flowNode = new FlowNode(flowIter, nCols, nRows, c, r);
                if (flowNode.isSource() && !flowNode.isHeadingOutside()) {
                    runningDistance = 0;
                    flowNode.setValueInMap(hacklengthIter, runningDistance);

                    FlowNode oldNode = flowNode;

                    maxTca = 1;
                    // hacklengthIter.setSample(flow[0], flow[1], 0, count);
                    // oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);

                    FlowNode runningNode = oldNode.goDownstream();
                    while( runningNode != null && runningNode.isValid() && !runningNode.isMarkedAsOutlet() ) {
                        flow[0] = runningNode.col;
                        flow[1] = runningNode.row;
                        boolean isMax = ModelsEngine.tcaMax(flowIter, tcaIter, hacklengthIter, flow, maxTca, runningDistance);
                        if (isMax) {

                            double distance = Direction.forFlow((int) oldNode.flow).getDistance(xRes, yRes);
                            if (elevIter != null) {
                                double d1 = oldNode.getValueFromMap(elevIter);
                                double d2 = runningNode.getValueFromMap(elevIter);
                                double dz = d1 - d2;
                                runningDistance += sqrt(pow(distance, 2) + pow(dz, 2));
                            } else {
                                runningDistance += distance;
                            }
                            runningNode.setValueInMap(hacklengthIter, runningDistance);
                            // hacklengthIter.setSample(flow[0], flow[1], 0, count);

                            maxTca = runningNode.getValueFromMap(tcaIter);
                            // maz = tcaIter.getSampleDouble(flow[0], flow[1], 0);
                        }
                        oldNode = runningNode;
                        runningNode = runningNode.goDownstream();
                        // oldir = flowIter.getSampleDouble(flow[0], flow[1], 0);
                        // flow_p[0] = flow[0];
                        // flow_p[1] = flow[1];
                        // if (!ModelsEngine.go_downstream(flow, flowIter.getSampleDouble(flow[0],
                        // flow[1], 0)))
                        // return;
                    }

                    if (runningNode != null && runningNode.isMarkedAsOutlet()) {
                        // flowIter.getSampleDouble(flow[0], flow[1], 0) == 10) {
                        if (ModelsEngine.tcaMax(flowIter, tcaIter, hacklengthIter, flow, maxTca, runningDistance)) {
                            double distance = Direction.forFlow((int) oldNode.flow).getDistance(xRes, yRes);
                            if (elevIter != null) {
                                double d1 = oldNode.getValueFromMap(elevIter);
                                double d2 = runningNode.getValueFromMap(elevIter);
                                double dz = d1 - d2;
                                runningDistance += sqrt(pow(distance, 2) + pow(dz, 2));
                            } else {
                                runningDistance += distance;
                            }
                            runningNode.setValueInMap(hacklengthIter, runningDistance);
                            // hacklengthIter.setSample(flow[0], flow[1], 0, count);
                        }
                    }

                }

            }
            pm.worked(1);
        }
        pm.done();
        hacklengthIter.done();
        outHacklength = CoverageUtilities.buildCoverage("Hacklength", hacklengthWR, regionMap,
                inFlow.getCoordinateReferenceSystem());
    }
}
