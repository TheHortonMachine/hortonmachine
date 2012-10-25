package org.jgrasstools.gears;

import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.Direction;
import org.jgrasstools.gears.libs.modules.FlowNode;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.GridNodeElevationToLeastComparator;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test FileIterator.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TestFlowUtils extends HMTestCase {

    private int nCols;
    private int nRows;
    private double xRes;
    private double yRes;
    private RandomIter elevationIter;
    private RandomIter flowIter;

    protected void setUp() throws Exception {
        double[][] mapData = HMTestMaps.mapData;
        double[][] flowData = HMTestMaps.flowData;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        GridCoverage2D inElev = CoverageUtilities.buildCoverage("elevation", mapData, envelopeParams, crs, true);
        GridCoverage2D inFlow = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        nCols = regionMap.getCols();
        nRows = regionMap.getRows();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();

        elevationIter = CoverageUtilities.getRandomIterator(inElev);
        flowIter = CoverageUtilities.getRandomIterator(inFlow);
    }

    public void testSlopeTo() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 0, 0);
        GridNode node2 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 0, 1);
        double slopeTo = node1.getSlopeTo(node2);
        assertEquals(slopeTo, 6.666666666666667, DELTA);
    }

    public void testSurroundingCells() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 0, 0);
        List<GridNode> surroundingNodes = node1.getSurroundingNodes();
        assertEquals(8, surroundingNodes.size());

        int count = 0;
        for( GridNode flowNode : surroundingNodes ) {
            if (flowNode != null) {
                count++;
            }
        }
        assertEquals(2, count);
    }

    public void testNextSteepestNode() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2);
        GridNode nextDownstreamNode = node1.goDownstreamSP();
        assertEquals(nextDownstreamNode.col, 1);
        assertEquals(nextDownstreamNode.row, 3);
        while( nextDownstreamNode != null ) {
            GridNode tmpNode = nextDownstreamNode.goDownstreamSP();
            if (tmpNode == null) {
                assertTrue(nextDownstreamNode.isOutlet());
            }
            nextDownstreamNode = tmpNode;
        }
    }

    public void testEnteringCells() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2);

        List<GridNode> enteringNodes = node1.getEnteringNodesSP();
        for( GridNode flowNode : enteringNodes ) {
            assertEquals(flowNode.col, 3);
            assertEquals(flowNode.row, 1);
        }
    }

    public void testSurroundingCellValues() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2);

        assertEquals(node1.getElevationAt(Direction.E), 750, DELTA);
        assertEquals(node1.getElevationAt(Direction.EN), 850, DELTA);
        assertEquals(node1.getElevationAt(Direction.N), 750, DELTA);
        assertTrue(JGTConstants.isNovalue(node1.getElevationAt(Direction.NW)));
        assertEquals(node1.getElevationAt(Direction.W), 550, DELTA);
        assertEquals(node1.getElevationAt(Direction.WS), 410, DELTA);
        assertEquals(node1.getElevationAt(Direction.S), 650, DELTA);
        assertEquals(node1.getElevationAt(Direction.SE), 700, DELTA);
    }

    public void testNonEnteringCells() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2);

        List<GridNode> nonEnteringNodes = node1.getNonEnteringNodesSP();
        assertEquals(6, nonEnteringNodes.size());

        GridNode node = nonEnteringNodes.get(0);
        assertEquals(node.col, 3);
        assertEquals(node.row, 2);
    }

    public void testTouchesBound() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2);
        assertTrue(node1.touchesBound());

        node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 1, 5);
        assertFalse(node1.touchesBound());
    }

    public void testElevationSort() throws Exception {
        TreeSet<GridNode> set = new TreeSet<GridNode>(new GridNodeElevationToLeastComparator());
        for( int c = 0; c < nCols; c++ ) {
            for( int r = 0; r < nRows; r++ ) {
                GridNode node = new GridNode(elevationIter, nCols, nRows, xRes, yRes, c, r);
                if (node.isValid()) {
                    boolean added = set.add(node);
                    assertTrue(added);
                }
            }
        }

        GridNode first = set.first();
        assertEquals(first.col, 0);
        assertEquals(first.row, 3);
        GridNode last = set.last();
        assertEquals(last.col, 9);
        assertEquals(last.row, 0);

    }

    public void testEnteringFlowCells() throws Exception {
        FlowNode node = new FlowNode(flowIter, nCols, nRows, 2, 2);

        List<FlowNode> enteringNodes = node.getEnteringNodes();
        FlowNode flowNode = enteringNodes.get(0);
        assertEquals(flowNode.col, 3);
        assertEquals(flowNode.row, 1);

        node = new FlowNode(flowIter, nCols, nRows, 5, 4);
        enteringNodes = node.getEnteringNodes();
        flowNode = enteringNodes.get(0);
        assertEquals(flowNode.col, 6);
        assertEquals(flowNode.row, 4);
        flowNode = enteringNodes.get(1);
        assertEquals(flowNode.col, 6);
        assertEquals(flowNode.row, 3);
        flowNode = enteringNodes.get(2);
        assertEquals(flowNode.col, 6);
        assertEquals(flowNode.row, 5);
    }

    public void testDownstreamFlowCells() throws Exception {
        FlowNode node = new FlowNode(flowIter, nCols, nRows, 4, 1);

        FlowNode n = node.goDownstream();
        assertEquals(n.col, 3);
        assertEquals(n.row, 2);
        n = n.goDownstream();
        assertEquals(n.col, 2);
        assertEquals(n.row, 3);
        n = n.goDownstream();
        assertEquals(n.col, 1);
        assertEquals(n.row, 3);
        n = n.goDownstream();
        assertNull(n);
    }

}
