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
package org.jgrasstools.hortonmachine.modules.network.networkattributes;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_doHack_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inNet_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inTca_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_outHack_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_outNet_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
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
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.Node;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

@Description(OMSNETWORKATTRIBUTESBUILDER_DESCRIPTION)
@Author(name = OMSNETWORKATTRIBUTESBUILDER_AUTHORNAMES, contact = OMSNETWORKATTRIBUTESBUILDER_AUTHORCONTACTS)
@Keywords(OMSNETWORKATTRIBUTESBUILDER_KEYWORDS)
@Label(OMSNETWORKATTRIBUTESBUILDER_LABEL)
@Name(OMSNETWORKATTRIBUTESBUILDER_NAME)
@Status(OMSNETWORKATTRIBUTESBUILDER_STATUS)
@License(OMSNETWORKATTRIBUTESBUILDER_LICENSE)
public class OmsNetworkAttributesBuilder extends JGTModel {

    @Description(OMSNETWORKATTRIBUTESBUILDER_inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(OMSNETWORKATTRIBUTESBUILDER_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSNETWORKATTRIBUTESBUILDER_inTca_DESCRIPTION)
    @In
    public GridCoverage2D inTca = null;

    @Description(OMSNETWORKATTRIBUTESBUILDER_doHack_DESCRIPTION)
    @In
    public boolean doHack = false;

    @Description(OMSNETWORKATTRIBUTESBUILDER_outNet_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outNet = null;

    @Description(OMSNETWORKATTRIBUTESBUILDER_outHack_DESCRIPTION)
    @Out
    public GridCoverage2D outHack = null;

    private int cols;
    private int rows;

    private List<SimpleFeature> networkList = new ArrayList<SimpleFeature>();

    private GridGeometry2D gridGeometry;

    private RandomIter netIter;

    private GeometryFactory gf = GeometryUtilities.gf();

    private SimpleFeatureBuilder networkBuilder;

    private RandomIter tcaIter;
    private int maxHack = 0;

    private WritableRandomIter hackWIter;

    @Execute
    public void process() throws Exception {
        checkNull(inFlow, inNet);
        if (!concatOr(outNet == null, doReset)) {
            return;
        }
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        cols = regionMap.getCols();
        rows = regionMap.getRows();
        gridGeometry = inFlow.getGridGeometry();

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlow);
        if (inTca != null) {
            tcaIter = CoverageUtilities.getRandomIterator(inTca);
        }
        netIter = CoverageUtilities.getRandomIterator(inNet);

        WritableRaster hackWR = null;
        if (doHack) {
            hackWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
            hackWIter = CoverageUtilities.getWritableRandomIterator(hackWR);
        }

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
                if (flowNode.isMarkedAsOutlet()) {
                    exitsList.add(flowNode);
                } else if (flowNode.touchesBound() && flowNode.isValid()) {
                    // check if the flow exits
                    Node goDownstream = flowNode.goDownstream();
                    if (goDownstream == null) {
                        // flowNode is exit
                        exitsList.add(flowNode);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("net");
        b.setCRS(inFlow.getCoordinateReferenceSystem());
        b.add("the_geom", LineString.class);
        b.add("hack", Integer.class);
        b.add("strahler", Integer.class);
        b.add("pfaf", String.class);
        SimpleFeatureType type = b.buildFeatureType();
        networkBuilder = new SimpleFeatureBuilder(type);

        /*
         * now start from every exit and run up the flowdirections
         */
        pm.beginTask("Extract vectors...", exitsList.size());
        for( FlowNode exitNode : exitsList ) {
            /*
             * - first hack order is 1
             * 
             */
            handleTrail(exitNode, null, 1);
            pm.worked(1);
        }
        pm.done();

        outNet = FeatureCollections.newCollection();
        outNet.addAll(networkList);

        if (hackWIter != null) {
            outHack = CoverageUtilities.buildCoverage("hack", hackWR, regionMap, inFlow.getCoordinateReferenceSystem());
        }
    }

    private void handleTrail( FlowNode runningNode, Coordinate startCoordinate, int hackIndex ) {
        List<Coordinate> lineCoordinatesList = new ArrayList<Coordinate>();
        if (startCoordinate != null) {
            lineCoordinatesList.add(startCoordinate);
            // write hack if needed
            runningNode.setValueInMap(hackWIter, hackIndex);
        }
        while( runningNode.getEnteringNodes().size() > 0 ) {
            int col = runningNode.col;
            int row = runningNode.row;
            Coordinate coord = CoverageUtilities.coordinateFromColRow(col, row, gridGeometry);

            double netValue = netIter.getSampleDouble(col, row, 0);
            if (!isNovalue(netValue)) {
                // if a net value is available, then it needs to be vector net
                lineCoordinatesList.add(coord);
                // write hack if needed
                runningNode.setValueInMap(hackWIter, hackIndex);
            } else {
                /*
                 * the line is finished 
                 */
                if (lineCoordinatesList.size() < 2) {
                    throw new RuntimeException();
                }
                // create a line and finish this trail
                createLine(lineCoordinatesList, hackIndex);
                break;
            }

            List<FlowNode> enteringNodes = runningNode.getEnteringNodes();
            List<FlowNode> checkedNodes = new ArrayList<FlowNode>();
            // we need to check which ones are really net nodes
            for( FlowNode tmpNode : enteringNodes ) {
                int tmpCol = tmpNode.col;
                int tmpRow = tmpNode.row;
                double tmpNetValue = netIter.getSampleDouble(tmpCol, tmpRow, 0);
                if (!isNovalue(tmpNetValue)) {
                    checkedNodes.add(tmpNode);
                }
            }
            if (checkedNodes.size() == 1) {
                // normal, get the next upstream node and go on
                runningNode = checkedNodes.get(0);
            } else if (checkedNodes.size() == 0) {
                // it was an exit
                createLine(lineCoordinatesList, hackIndex);
                break;
            } else if (checkedNodes.size() > 1) {

                createLine(lineCoordinatesList, hackIndex);

                if (tcaIter == null) {
                    // we just extract the vector line
                    for( FlowNode flowNode : checkedNodes ) {
                        handleTrail(flowNode, coord, hackIndex + 1);
                    }
                } else {
                    // we want also hack numbering and friends
                    FlowNode mainUpstream = runningNode.getUpstreamTcaBased(tcaIter, null);
                    // the main channel keeps the same index
                    handleTrail(mainUpstream, coord, hackIndex);
                    // the others jump up one
                    for( FlowNode flowNode : checkedNodes ) {
                        if (!flowNode.equals(mainUpstream)) {
                            handleTrail(flowNode, coord, hackIndex + 1);
                        }
                    }
                }
                break;
            } else {
                throw new RuntimeException();
            }
        }
    }

    private void createLine( List<Coordinate> lineCoordinatesList, int hackindex ) {
        if (lineCoordinatesList.size() < 2) {
            return;
        }
        if (hackindex > maxHack) {
            maxHack = hackindex;
        }
        LineString newNetLine = gf.createLineString(lineCoordinatesList.toArray(new Coordinate[0]));
        Object[] values = new Object[]{newNetLine, hackindex, 0, "-"};
        networkBuilder.addAll(values);
        SimpleFeature netFeature = networkBuilder.buildFeature(null);
        synchronized (networkList) {
            networkList.add(netFeature);
        }
    }

}