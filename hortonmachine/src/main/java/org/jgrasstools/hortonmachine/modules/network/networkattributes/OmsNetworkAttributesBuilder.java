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
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inDem_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inNet_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inTca_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_outHack_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_outNet_DESCRIPTION;

import java.awt.geom.Point2D;
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
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.Node;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

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

    @Description(OMSNETWORKATTRIBUTESBUILDER_inDem_DESCRIPTION)
    @In
    public GridCoverage2D inDem = null;

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

    private List<NetworkChannel> channels;

    @Execute
    public void process() throws Exception {
        checkNull(inFlow, inNet, inTca);
        if (!concatOr(outNet == null, doReset)) {
            return;
        }
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        cols = regionMap.getCols();
        rows = regionMap.getRows();
        gridGeometry = inFlow.getGridGeometry();

        RandomIter flowIter = CoverageUtilities.getRandomIterator(inFlow);
        tcaIter = CoverageUtilities.getRandomIterator(inTca);
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
        
        if (exitsList.size()==0) {
            throw new ModelsIllegalargumentException("No outlet has been found in the network. Check your data.", this);
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("net");
        b.setCRS(inFlow.getCoordinateReferenceSystem());
        b.add("the_geom", LineString.class);
        String hackName = NetworkChannel.HACKNAME;
        b.add(hackName, Integer.class);
        String strahlerName = NetworkChannel.STRAHLERNAME;
        b.add(strahlerName, Integer.class);
        b.add(NetworkChannel.PFAFNAME, String.class);
        if (inDem != null) {
            b.add(NetworkChannel.STARTELEVNAME, Double.class);
            b.add(NetworkChannel.ENDELEVNAME, Double.class);
        }
        SimpleFeatureType type = b.buildFeatureType();
        networkBuilder = new SimpleFeatureBuilder(type);

        pm.beginTask("Extract vectors...", exitsList.size());
        for( FlowNode exitNode : exitsList ) {
            /*
             * - first hack order is 1
             */
            handleTrail(exitNode, null, 1);
            pm.worked(1);
        }
        pm.done();

        outNet = new DefaultFeatureCollection();
        ((DefaultFeatureCollection) outNet).addAll(networkList);

        /*
         * connect channels
         */
        channels = new ArrayList<NetworkChannel>();
        for( SimpleFeature network : networkList ) {
            channels.add(new NetworkChannel(network));
        }
        pm.beginTask("Connect channels...", channels.size());
        for( NetworkChannel channel : channels ) {
            for( NetworkChannel checkChannel : channels ) {
                if (channel.equals(checkChannel)) {
                    continue;
                }
                channel.checkAndAdd(checkChannel);
            }
            pm.worked(1);
        }
        pm.done();

        /*
         * calculate strahler
         */
        pm.beginTask("Calculate Strahler...", IJGTProgressMonitor.UNKNOWN);
        calculateStrahler();
        pm.done();

        /*
         * calculate pfaf
         */
        pm.beginTask("Calculate Pfafstetter...", IJGTProgressMonitor.UNKNOWN);
        calculatePfafstetter();
        pm.done();

        if (hackWIter != null) {
            outHack = CoverageUtilities.buildCoverage("hack", hackWR, regionMap, inFlow.getCoordinateReferenceSystem());
        }
    }

    private void calculatePfafstetter() {
        for( int i = 1; i <= maxHack; i++ ) {
            // find a channel of that order
            List<NetworkChannel> startChannels = new ArrayList<NetworkChannel>();
            for( NetworkChannel channel : channels ) {
                int hack = channel.getHack();
                NetworkChannel nextChannel = channel.getNextChannel();
                if (hack == i && (nextChannel == null || nextChannel.getHack() != i)) {
                    startChannels.add(channel);
                }
            }

            for( NetworkChannel startChannel : startChannels ) {
                NetworkChannel nextChannel = startChannel.getNextChannel();
                String base = "";
                if (nextChannel != null) {
                    base = nextChannel.getPfaf();
                    int lastDot = base.lastIndexOf('.');
                    if (lastDot == -1) {
                        int lastInt = Integer.parseInt(base);
                        lastInt = lastInt + 1;
                        base = lastInt + ".";
                    } else {
                        String prefix = base.substring(0, lastDot + 1);
                        String last = base.substring(lastDot + 1);
                        int lastInt = Integer.parseInt(last);
                        lastInt = lastInt + 1;
                        base = prefix + lastInt + ".";
                    }
                }
                int index = 1;
                startChannel.setPfafstetter(base + index);
                index = index + 2;
                while( startChannel.getPreviousChannels().size() > 0 ) {
                    List<NetworkChannel> previousChannels = startChannel.getPreviousChannels();
                    for( NetworkChannel networkChannel : previousChannels ) {
                        if (networkChannel.getHack() == i) {
                            startChannel = networkChannel;
                            startChannel.setPfafstetter(base + index);
                            index = index + 2;
                            break;
                        }
                    }
                }

            }
        }
    }

    private void calculateStrahler() {
        // calculate Strahler
        List<NetworkChannel> sourceChannels = new ArrayList<NetworkChannel>();
        for( NetworkChannel channel : channels ) {
            if (channel.isSource()) {
                sourceChannels.add(channel);
                // set start
                channel.setStrahler(1);
            }
        }

        List<NetworkChannel> nextsList = new ArrayList<NetworkChannel>();
        List<NetworkChannel> toKeepList = new ArrayList<NetworkChannel>();
        while( true ) {
            sourceChannels.addAll(nextsList);
            nextsList.clear();
            sourceChannels.addAll(toKeepList);
            toKeepList.clear();
            for( NetworkChannel sourceChannel : sourceChannels ) {
                NetworkChannel nextChannel = sourceChannel.getNextChannel();
                if (nextChannel != null) {
                    if (!nextsList.contains(nextChannel))
                        nextsList.add(nextChannel);
                }
            }
            if (nextsList.size() == 0) {
                break;
            }

            for( NetworkChannel networkChannel : nextsList ) {
                List<NetworkChannel> previousChannels = networkChannel.getPreviousChannels();
                if (previousChannels.size() == 0) {
                    throw new RuntimeException();
                }
                int maxStrahler = 0;
                boolean allEqual = true;
                int previousStrahler = -1;
                boolean doContinue = false;
                for( NetworkChannel channel : previousChannels ) {
                    int strahler = channel.getStrahler();
                    if (strahler < 0) {
                        // has not been set yet, keep it
                        toKeepList.add(channel);
                        doContinue = true;
                        break;
                    } else {
                        if (strahler > maxStrahler)
                            maxStrahler = strahler;
                        if (previousStrahler > 0) {
                            // was set already
                            if (previousStrahler != strahler) {
                                allEqual = false;
                            }
                        }
                        previousStrahler = strahler;
                    }
                }
                if (doContinue) {
                    continue;
                }
                if (allEqual) {
                    networkChannel.setStrahler(maxStrahler + 1);
                } else {
                    networkChannel.setStrahler(maxStrahler);
                }
            }

            sourceChannels.clear();
        }
    }

    private void handleTrail( FlowNode runningNode, Coordinate startCoordinate, int hackIndex ) {
        List<Coordinate> lineCoordinatesList = new ArrayList<Coordinate>();
        if (startCoordinate != null) {
            lineCoordinatesList.add(startCoordinate);
            // write hack if needed
            if (doHack)
                runningNode.setValueInMap(hackWIter, hackIndex);
        }
        // if there are entering nodes
        while( runningNode.getEnteringNodes().size() > 0 ) {
            int col = runningNode.col;
            int row = runningNode.row;
            Coordinate coord = CoverageUtilities.coordinateFromColRow(col, row, gridGeometry);

            double netValue = netIter.getSampleDouble(col, row, 0);
            if (!isNovalue(netValue)) {
                // if a net value is available, then it needs to be vector net
                lineCoordinatesList.add(coord);
                // write hack if needed
                if (doHack)
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
        newNetLine = (LineString) newNetLine.reverse();
        Object[] values;
        if (inDem == null) {
            values = new Object[]{newNetLine, hackindex, 0, "-"};
        } else {
            Point startPoint = newNetLine.getStartPoint();
            Point endPoint = newNetLine.getEndPoint();
            Point2D p = new Point2D.Double();
            double[] value = new double[1];
            p.setLocation(startPoint.getX(), startPoint.getY());
            inDem.evaluate(p, value);
            double startElev = value[0];
            p.setLocation(endPoint.getX(), endPoint.getY());
            inDem.evaluate(p, value);
            double endElev = value[0];
            values = new Object[]{newNetLine, hackindex, 0, "-", startElev, endElev};
        }
        networkBuilder.addAll(values);
        SimpleFeature netFeature = networkBuilder.buildFeature(null);
        synchronized (networkList) {
            networkList.add(netFeature);
        }
    }

    public static void main( String[] args ) throws Exception {

        OmsNetworkAttributesBuilder networkattributesbuilder = new OmsNetworkAttributesBuilder();
        networkattributesbuilder.inNet = getRaster("/media/lacntfs/unibz_ana/utm_aa/test_ana/cell/ana_net_null");
        networkattributesbuilder.inFlow = getRaster("/media/lacntfs/unibz_ana/utm_aa/test_ana/cell/ana_flow_small");
        networkattributesbuilder.inTca = getRaster("/media/lacntfs/unibz_ana/utm_aa/test_ana/cell/ana_tca_small");
        networkattributesbuilder.doHack = false;
        networkattributesbuilder.process();
        dumpVector(networkattributesbuilder.outNet, "/media/lacntfs/unibz_ana/shape_net/ana_net.shp");
        // dumpRaster(networkattributesbuilder.outHack,
        // "/media/lacntfs/unibz_ana/utm_aa/test_ana/fcell/ana_hack_small");
    }

}