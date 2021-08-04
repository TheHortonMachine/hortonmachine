/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.hmachine.modules.basin.rescaleddistance;

import static java.lang.Math.abs;
import static org.hortonmachine.gears.libs.modules.HMConstants.*;
import static org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance.OMSRESCALEDDISTANCE_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance.OMSRESCALEDDISTANCE_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance.OMSRESCALEDDISTANCE_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance.OMSRESCALEDDISTANCE_KEYWORDS;
import static org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance.OMSRESCALEDDISTANCE_LABEL;
import static org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance.OMSRESCALEDDISTANCE_LICENSE;
import static org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance.OMSRESCALEDDISTANCE_NAME;
import static org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance.OMSRESCALEDDISTANCE_STATUS;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.multiprocessing.GridMultiProcessing;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OMSRESCALEDDISTANCE_DESCRIPTION)
@Author(name = OMSRESCALEDDISTANCE_AUTHORNAMES, contact = OMSRESCALEDDISTANCE_AUTHORCONTACTS)
@Keywords(OMSRESCALEDDISTANCE_KEYWORDS)
@Label(OMSRESCALEDDISTANCE_LABEL)
@Name(OMSRESCALEDDISTANCE_NAME)
@Status(OMSRESCALEDDISTANCE_STATUS)
@License(OMSRESCALEDDISTANCE_LICENSE)
public class OmsRescaledDistance extends GridMultiProcessing {

    @Description(OMSRESCALEDDISTANCE_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSRESCALEDDISTANCE_inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(OMSRESCALEDDISTANCE_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSRESCALEDDISTANCE_pRatio_DESCRIPTION)
    @In
    public double pRatio = 0;

    @Description(OMSRESCALEDDISTANCE_outRescaled_DESCRIPTION)
    @Out
    public GridCoverage2D outRescaled = null;

    public static final String OMSRESCALEDDISTANCE_DESCRIPTION = "Calculates the rescaled distance of each pixel from the outlet.";
    public static final String OMSRESCALEDDISTANCE_DOCUMENTATION = "OmsRescaledDistance.html";
    public static final String OMSRESCALEDDISTANCE_KEYWORDS = "Basin, Geomorphology, D2O";
    public static final String OMSRESCALEDDISTANCE_LABEL = BASIN;
    public static final String OMSRESCALEDDISTANCE_NAME = "rescdist";
    public static final int OMSRESCALEDDISTANCE_STATUS = 40;
    public static final String OMSRESCALEDDISTANCE_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRESCALEDDISTANCE_AUTHORNAMES = "Antonello Andrea, Franceschi Silvia, Daniele Andreis,  Erica Ghesla, Cozzini Andrea, Pisoni Silvano, Rigon Riccardo";
    public static final String OMSRESCALEDDISTANCE_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSRESCALEDDISTANCE_inFlow_DESCRIPTION = "The map of flowdirections.";
    public static final String OMSRESCALEDDISTANCE_inNet_DESCRIPTION = "The map of the network.";
    public static final String OMSRESCALEDDISTANCE_inElev_DESCRIPTION = "The optional map of elevation for 3D.";
    public static final String OMSRESCALEDDISTANCE_pRatio_DESCRIPTION = "Ratio between the velocity in the channel and in the hillslope.";
    public static final String OMSRESCALEDDISTANCE_outRescaled_DESCRIPTION = "The map of the rescaled distances.";

    private double xRes;
    private double yRes;
    private RandomIter elevIter = null;

    @Execute
    public void process() throws Exception {
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
        int novalue = HMConstants.getIntNovalue(inFlow);

        RenderedImage netRI = inNet.getRenderedImage();
        RandomIter netIter = RandomIterFactory.create(netRI, null);

        if (inElev != null) {
            RenderedImage elevRI = inElev.getRenderedImage();
            elevIter = RandomIterFactory.create(elevRI, null);
        }

        WritableRaster rescaledWR = CoverageUtilities.createWritableRaster(cols, rows, Float.class, null, floatNovalue);
        WritableRandomIter rescaledIter = RandomIterFactory.createWritable(rescaledWR, null);

        try {
            pm.beginTask("Find outlets...", rows * cols); //$NON-NLS-1$
            ConcurrentLinkedQueue<FlowNode> exitsList = new ConcurrentLinkedQueue<>();
            processGrid(cols, rows, ( c, r ) -> {
                if (pm.isCanceled())
                    return;
                int netValue = netIter.getSample(c, r, 0);
                if (isNovalue(netValue)) {
                    // we make sure that we pick only outlets that are on the net
                    return;
                }
                FlowNode flowNode = new FlowNode(flowIter, cols, rows, c, r, novalue);
                if (flowNode.isHeadingOutside()) {
                    exitsList.add(flowNode);
                }
                pm.worked(1);
            });
            pm.done();

            if (exitsList.size() == 0) {
                throw new ModelsIllegalargumentException("No exits found in the map of flowdirections.", this);
            }

            pm.beginTask("Calculate rescaled distance...", exitsList.size());
            exitsList.parallelStream().forEach(exitNode -> {
                if (pm.isCanceled())
                    return;
                calculateRescaledDistance(exitNode, (float) xRes, rescaledIter, elevIter, netIter);
                pm.worked(1);
            });
            pm.done();
        } finally {
            rescaledIter.done();
            netIter.done();
            if (elevIter != null)
                elevIter.done();
        }

        outRescaled = CoverageUtilities.buildCoverage("OmsRescaledDistance", rescaledWR, regionMap,
                inFlow.getCoordinateReferenceSystem());
    }

    private void calculateRescaledDistance( FlowNode runningNode, float distance, WritableRandomIter rescaledIter,
            RandomIter elevIter, RandomIter netIter ) {
        runningNode.setFloatValueInMap(rescaledIter, distance);
        if (runningNode.getEnteringNodes().size() > 0) {
            List<FlowNode> enteringNodes = runningNode.getEnteringNodes();
            for( FlowNode enteringNode : enteringNodes ) {
                double tmpDistance = Direction.forFlow((int) enteringNode.flow).getDistance(xRes, yRes);
                if (elevIter != null) {
                    double fromElev = enteringNode.getValueFromMap(elevIter);
                    double toElev = runningNode.getValueFromMap(elevIter);
                    tmpDistance = NumericsUtilities.pythagoras(tmpDistance, abs(toElev - fromElev));
                }

                int netValue = enteringNode.getIntValueFromMap(netIter);
                double newDistance = 0.0;
                if (isNovalue(netValue)) {
                    newDistance = distance + tmpDistance * pRatio;
                } else {
                    newDistance = distance + tmpDistance;
                }
                calculateRescaledDistance(enteringNode, (float) newDistance, rescaledIter, elevIter, netIter);
            }
        }

    }
}
