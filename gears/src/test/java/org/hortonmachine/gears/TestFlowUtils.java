package org.hortonmachine.gears;

import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.GridNodeElevationToLeastComparator;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.Node;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test OmsFileIterator.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TestFlowUtils extends HMTestCase {
    private static final double NaN = HMConstants.doubleNovalue;
    private static final int intNaN = HMConstants.intNovalue;

    private int nCols;
    private int nRows;
    private double xRes;
    private double yRes;
    private RandomIter elevationIter;
    private RandomIter flowIter;

    protected void setUp() throws Exception {
        double[][] mapData = HMTestMaps.mapData;
        int[][] flowData = HMTestMaps.flowData;
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        GridCoverage2D inElev = CoverageUtilities.buildCoverageWithNovalue("elevation", mapData, envelopeParams, crs, true, NaN);
        GridCoverage2D inFlow = CoverageUtilities.buildCoverageWithNovalue("flow", flowData, envelopeParams, crs, true, intNaN);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        nCols = regionMap.getCols();
        nRows = regionMap.getRows();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();

        elevationIter = CoverageUtilities.getRandomIterator(inElev);
        flowIter = CoverageUtilities.getRandomIterator(inFlow);
    }

    public void testGridNodeWindow() throws Exception {
        GridNode n = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 0, 0, NaN);
        double[][] window = n.getWindow(4, false);

        double[][] expected = new double[][]{//
                /*    */{NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 800.0, 900.0, 1000}, //
                {NaN, NaN, 600.0, NaN, 750}, //
                {NaN, NaN, 500.0, 550.0, 700}//
        };
        checkMatrixEqual(expected, window, DELTA);

        n = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 4, 5, NaN);
        window = n.getWindow(5, false);
        expected = new double[][]{//
                /*    */{650, 700, 750, 800, 850}, //
                {430, 500, 600, 700, 800}, //
                {700, 750, 760, 770, 850}, //
                {750, 800, 780, 790, 1000}, //
                {980, 1001, 1150, 1200, 1250}//
        };
        checkMatrixEqual(expected, window, DELTA);

        window = n.getWindow(5, true);
        expected = new double[][]{//
                /*    */{NaN, NaN, 750, NaN, NaN}, //
                {NaN, 500, 600, 700, NaN}, //
                {700, 750, 760, 770, 850}, //
                {NaN, 800, 780, 790, NaN}, //
                {NaN, NaN, 1150, NaN, NaN}//
        };
        checkMatrixEqual(expected, window, DELTA);

    }

    public void testSlopeTo() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 0, 0, NaN);
        GridNode node2 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 0, 1, NaN);
        double slopeTo = node1.getSlopeTo(node2);
        assertEquals(slopeTo, 6.666666666666667, DELTA);
    }

    public void testSurroundingCells() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 0, 0, NaN);
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
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2, NaN);
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
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2, NaN);

        List<GridNode> enteringNodes = node1.getEnteringNodesSP();
        for( GridNode flowNode : enteringNodes ) {
            assertEquals(flowNode.col, 3);
            assertEquals(flowNode.row, 1);
        }
    }

    public void testSurroundingCellValues() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2, NaN);

        assertEquals(node1.getElevationAt(Direction.E), 750, DELTA);
        assertEquals(node1.getElevationAt(Direction.EN), 850, DELTA);
        assertEquals(node1.getElevationAt(Direction.N), 750, DELTA);
        assertTrue(HMConstants.isNovalue(node1.getElevationAt(Direction.NW)));
        assertEquals(node1.getElevationAt(Direction.W), 550, DELTA);
        assertEquals(node1.getElevationAt(Direction.WS), 410, DELTA);
        assertEquals(node1.getElevationAt(Direction.S), 650, DELTA);
        assertEquals(node1.getElevationAt(Direction.SE), 700, DELTA);
    }

    public void testNonEnteringCells() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2, NaN);

        List<GridNode> nonEnteringNodes = node1.getNonEnteringNodesSP();
        assertEquals(6, nonEnteringNodes.size());

        GridNode node = nonEnteringNodes.get(0);
        assertEquals(node.col, 3);
        assertEquals(node.row, 2);
    }

    public void testTouchesBound() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2, NaN);
        boolean touchesNovalue = node1.touchesNovalue();
        assertTrue(touchesNovalue);

        node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 1, 5, NaN);
        touchesNovalue = node1.touchesNovalue();
        assertFalse(touchesNovalue);
    }

    public void testElevationSort() throws Exception {
        TreeSet<GridNode> set = new TreeSet<GridNode>(new GridNodeElevationToLeastComparator());
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                GridNode node = new GridNode(elevationIter, nCols, nRows, xRes, yRes, c, r, NaN);
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
        FlowNode node = new FlowNode(flowIter, nCols, nRows, 2, 2, intNaN);

        List<FlowNode> enteringNodes = node.getEnteringNodes();
        Node flowNode = enteringNodes.get(0);
        assertEquals(flowNode.col, 3);
        assertEquals(flowNode.row, 1);

        node = new FlowNode(flowIter, nCols, nRows, 5, 4, intNaN);
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
        FlowNode node = new FlowNode(flowIter, nCols, nRows, 4, 1, intNaN);

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
