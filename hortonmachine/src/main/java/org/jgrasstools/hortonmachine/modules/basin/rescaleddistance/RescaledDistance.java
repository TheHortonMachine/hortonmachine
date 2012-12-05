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
package org.jgrasstools.hortonmachine.modules.basin.rescaleddistance;

import static java.lang.Math.abs;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
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
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;

@Description("Calculates the rescaled distance of each pixel from the outlet.")
@Documentation("RescaledDistance.html")
@Author(name = "Antonello Andrea, Franceschi Silvia, Daniele Andreis,  Erica Ghesla, Cozzini Andrea, Pisoni Silvano, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Basin, Geomorphology, D2O")
@Label(JGTConstants.BASIN)
@Name("rescdist")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class RescaledDistance extends JGTModel {

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The map of the network.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The optional map of elevation for 3D.")
    @In
    public GridCoverage2D inElev = null;

    @Description("Ratio between the velocity in the channel and in the hillslope.")
    @In
    public double pRatio = 0;

    @Description("The map of the rescaled distances.")
    @Out
    public GridCoverage2D outRescaled = null;

    private WritableRandomIter rescaledIter;

    private double xRes;

    private double yRes;

    private RandomIter netIter;

    private RandomIter elevIter;

    @Execute
    public void process() {
        if (!concatOr(outRescaled == null, doReset)) {
            return;
        }
        checkNull(inFlow, inNet);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();

        RenderedImage flowRI = inFlow.getRenderedImage();
        RandomIter flowIter = RandomIterFactory.create(flowRI, null);

        RenderedImage netRI = inNet.getRenderedImage();
        netIter = RandomIterFactory.create(netRI, null);

        if (inElev != null) {
            RenderedImage elevRI = inElev.getRenderedImage();
            elevIter = RandomIterFactory.create(elevRI, null);
        }

        WritableRaster rescaledWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, doubleNovalue);
        rescaledIter = RandomIterFactory.createWritable(rescaledWR, null);

        pm.beginTask("Find outlets...", rows); //$NON-NLS-1$
        List<FlowNode> exitsList = new ArrayList<FlowNode>();
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double netValue = netIter.getSampleDouble(c, r, 0);
                if (isNovalue(netValue)) {
                    // we make sure that we pick only outlets that are on the net
                    continue;
                }
                FlowNode flowNode = new FlowNode(flowIter, cols, rows, c, r);
                if (flowNode.isHeadingOutside()) {
                    exitsList.add(flowNode);
                }
            }
            pm.worked(1);
        }
        pm.done();

        pm.beginTask("Calculate rescaled distance...", exitsList.size());
        for( FlowNode exitNode : exitsList ) {
            calculateRescaledDistance(exitNode, xRes);
            pm.worked(1);
        }
        pm.done();

        outRescaled = CoverageUtilities.buildCoverage("RescaledDistance", rescaledWR, regionMap,
                inFlow.getCoordinateReferenceSystem());
    }

    private void calculateRescaledDistance( FlowNode runningNode, double distance ) {
        runningNode.setValueInMap(rescaledIter, distance);
        if (runningNode.getEnteringNodes().size() > 0) {
            List<FlowNode> enteringNodes = runningNode.getEnteringNodes();
            for( FlowNode enteringNode : enteringNodes ) {
                double tmpDistance = Direction.forFlow((int) enteringNode.flow).getDistance(xRes, yRes);
                if (elevIter != null) {
                    double fromElev = enteringNode.getValueFromMap(elevIter);
                    double toElev = runningNode.getValueFromMap(elevIter);
                    tmpDistance = NumericsUtilities.pythagoras(tmpDistance, abs(toElev - fromElev));
                }

                double netValue = enteringNode.getValueFromMap(netIter);
                double newDistance = 0.0;
                if (isNovalue(netValue)) {
                    newDistance = distance + tmpDistance * pRatio;
                } else {
                    newDistance = distance + tmpDistance;
                }
                calculateRescaledDistance(enteringNode, newDistance);
            }
        }

    }
}
