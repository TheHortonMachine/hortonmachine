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
package org.jgrasstools.hortonmachine.modules.network.extractnetwork;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

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
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

@Description("Extracts the vector network based on a raster network.")
@Author(name = "Andrea Antonello, Franceschi Silvia", contact = "http://www.hydrologis.com")
@Keywords("Network, Vector, FlowDirectionsTC, GC, DrainDir, Gradient, Slope")
@Label(JGTConstants.NETWORK)
@Name("extractvectornet")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class ExtractVectorNetwork extends JGTModel {

    @Description("The network raster map.")
    @In
    public GridCoverage2D inNet = null;

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The vector of the network.")
    @Out
    public SimpleFeatureCollection outNet = null;

    private int cols;
    private int rows;

    private List<LineString> networkList = new ArrayList<LineString>();

    private GridGeometry2D gridGeometry;

    private RandomIter netIter;

    private GeometryFactory gf = GeometryUtilities.gf();

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
        netIter = CoverageUtilities.getRandomIterator(inNet);

        pm.beginTask("Find outlets...", rows); //$NON-NLS-1$
        List<FlowNode> exitsList = new ArrayList<FlowNode>();
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                FlowNode flowNode = new FlowNode(flowIter, cols, rows, c, r);
                if (flowNode.isOutlet()) {
                    exitsList.add(flowNode);
                } else if (flowNode.touchesBound() && flowNode.isValid()) {
                    // check if the flow exits
                    FlowNode goDownstream = flowNode.goDownstream();
                    if (goDownstream == null) {
                        // flowNode is exit
                        exitsList.add(flowNode);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        /*
         * now start from every exit and run up the flowdirections
         */

        for( FlowNode exitNode : exitsList ) {
            FlowNode runningNode = exitNode;
            handleTrail(runningNode);
        }

        // outNet = ModelsEngine.net2ShapeOnly(flowRI, netNumWR, inFlow.getGridGeometry(), nstream,
        // pm);
    }

    private void handleTrail( FlowNode runningNode ) {
        List<Coordinate> lineCoordinatesList = new ArrayList<Coordinate>();
        while( runningNode.getEnteringNodes().size() > 0 ) {
            int col = runningNode.col;
            int row = runningNode.row;

            double netValue = netIter.getSampleDouble(col, row, 0);
            if (!isNovalue(netValue)) {
                Coordinate coord = CoverageUtilities.coordinateFromColRow(col, row, gridGeometry);
                lineCoordinatesList.add(coord);
            } else {
                LineString newNetLine = gf.createLineString(lineCoordinatesList.toArray(new Coordinate[0]));
                synchronized (networkList) {
                    networkList.add(newNetLine);
                }
                break;
            }

            List<FlowNode> enteringNodes = runningNode.getEnteringNodes();
            if (enteringNodes.size() == 1) {
                // normal, get the next upstream node and go on
                runningNode = enteringNodes.get(0);
            } else if (enteringNodes.size() == 0) {
                // this is an exit?
                throw new RuntimeException();
            } else if (enteringNodes.size() > 1) {
                LineString newNetLine = gf.createLineString(lineCoordinatesList.toArray(new Coordinate[0]));
                synchronized (networkList) {
                    networkList.add(newNetLine);
                }

                // multiple nodes, we need to start new lines
                for( FlowNode flowNode : enteringNodes ) {
                    handleTrail(flowNode);
                }
            } else {
                throw new RuntimeException();
            }

        }
    }

}