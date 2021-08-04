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
package org.hortonmachine.hmachine.modules.network.hacklength;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_inElevation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_inTca_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHACKLENGTH_outHacklength_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.Node;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

@Description(OMSHACKLENGTH_DESCRIPTION)
@Author(name = OMSHACKLENGTH_AUTHORNAMES, contact = OMSHACKLENGTH_AUTHORCONTACTS)
@Keywords(OMSHACKLENGTH_KEYWORDS)
@Label(OMSHACKLENGTH_LABEL)
@Name(OMSHACKLENGTH_NAME)
@Status(OMSHACKLENGTH_STATUS)
@License(OMSHACKLENGTH_LICENSE)
public class OmsHackLength extends HMModel {

    @Description(OMSHACKLENGTH_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSHACKLENGTH_inTca_DESCRIPTION)
    @In
    public GridCoverage2D inTca = null;

    @Description(OMSHACKLENGTH_inElevation_DESCRIPTION)
    @In
    public GridCoverage2D inElevation = null;

    @Description(OMSHACKLENGTH_outHacklength_DESCRIPTION)
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

        int novalue = HMConstants.getIntNovalue(inFlow);
        hacklength(flowIter, novalue, tcaIter, elevIter);

        tcaIter.done();
        flowIter.done();
        if (elevIter != null) {
            elevIter.done();
        }

    }

    private void hacklength( RandomIter flowIter, int novalue, RandomIter tcaIter, RandomIter elevIter ) {

        double runningDistance = 0.0;
        double maxTca = 0.0;

        WritableRaster hacklengthWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter hacklengthIter = RandomIterFactory.createWritable(hacklengthWR, null);

        pm.beginTask(msg.message("hacklength.calculating"), nRows); //$NON-NLS-1$
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                FlowNode flowNode = new FlowNode(flowIter, nCols, nRows, c, r, novalue);
                if (flowNode.isSource() && !flowNode.isHeadingOutside()) {
                    runningDistance = 0;
                    flowNode.setValueInMap(hacklengthIter, runningDistance);

                    maxTca = 1;

                    FlowNode oldNode = flowNode;
                    FlowNode runningNode = oldNode.goDownstream();
                    while( runningNode != null && runningNode.isValid() && !runningNode.isMarkedAsOutlet() ) {

                        boolean isMax = tcaMax(runningNode, tcaIter, hacklengthIter, maxTca, runningDistance);
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

                            maxTca = runningNode.getValueFromMap(tcaIter);
                        }
                        oldNode = runningNode;
                        runningNode = runningNode.goDownstream();
                    }

                    if (runningNode != null && runningNode.isMarkedAsOutlet()) {
                        if (tcaMax(runningNode, tcaIter, hacklengthIter, maxTca, runningDistance)) {
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

    /**
     * Compare two value of tca and distance.
     * 
     * <p>
     * It's used to evaluate some special distance (as hacklength). 
     * In these case, the value of the distance is a property of 
     * the path, and so when two pixel drain in a same pixel the 
     * actual value is calculate from the pixel that have the 
     * maximum value. So this method evaluate if the distance is 
     * already evaluate, throghout another path, and 
     * if the value of the old path is greater than the next path.
     * </p>
     */
    public static boolean tcaMax( FlowNode flowNode, RandomIter tcaIter, RandomIter hacklengthIter, double maxTca,
            double maxDistance ) {

        List<FlowNode> enteringNodes = flowNode.getEnteringNodes();
        for( Node node : enteringNodes ) {
            double tca = node.getValueFromMap(tcaIter);
            if (tca >= maxTca) {
                if (NumericsUtilities.dEq(tca, maxTca)) {
                    if (node.getValueFromMap(hacklengthIter) > maxDistance)
                        return false;
                } else
                    return false;
            }

        }
        return true;
    }
}
