package org.hortonmachine.gears;

import java.util.List;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.Direction;
import org.hortonmachine.gears.libs.modules.FlowNodeNG;
import org.hortonmachine.gears.libs.modules.GridNodeNG;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.NodeNG;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
/**
 * Test OmsFileIterator.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TestFlowNGUtils extends HMTestCase {
    private static final double NaN = HMConstants.doubleNovalue;
    private static final int intNaN = HMConstants.intNovalue;

    private HMRaster elevationIter;
    private HMRaster flowIter;

    protected void setUp() throws Exception {
        double[][] mapData = HMTestMaps.mapData;
        int[][] flowData = HMTestMaps.flowData;
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        GridCoverage2D inElev = CoverageUtilities.buildCoverageWithNovalue("elevation", mapData, envelopeParams, crs, true, NaN);
        GridCoverage2D inFlow = CoverageUtilities.buildCoverageWithNovalue("flow", flowData, envelopeParams, crs, true, intNaN);
        elevationIter = HMRaster.fromGridCoverage(inElev);
        flowIter = HMRaster.fromGridCoverage(inFlow);
    }

    public void testGridNodeWindow() throws Exception {
        GridNodeNG n = elevationIter.getGridNodeNG(0, 0);
        double[][] window = n.getWindow(4, false);

        double[][] expected = new double[][]{//
                /*    */{NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 800.0, 900.0, 1000}, //
                {NaN, NaN, 600.0, NaN, 750}, //
                {NaN, NaN, 500.0, 550.0, 700}//
        };
        checkMatrixEqual(expected, window, DELTA);

        n = elevationIter.getGridNodeNG(4, 5);
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
    	GridNodeNG node1 =  elevationIter.getGridNodeNG( 0, 0);
    	GridNodeNG node2 =  elevationIter.getGridNodeNG(0, 1);
        double slopeTo = node1.getSlopeTo(node2);
        assertEquals(slopeTo, 6.666666666666667, DELTA);
    }

    public void testSurroundingCells() throws Exception {
        GridNodeNG node1 = elevationIter.getGridNodeNG(0, 0);
        List<GridNodeNG> surroundingNodes = node1.getSurroundingNodes();
        assertEquals(8, surroundingNodes.size());

        int count = 0;
        for( GridNodeNG flowNode : surroundingNodes ) {
            if (flowNode != null) {
                count++;
            }
        }
        assertEquals(2, count);
    }

    public void testNextSteepestNode() throws Exception {
        GridNodeNG node1 = elevationIter.getGridNodeNG(2, 2);
        GridNodeNG nextDownstreamNode = node1.goDownstreamSP();
        assertEquals(nextDownstreamNode.col, 1);
        assertEquals(nextDownstreamNode.row, 3);
        while( nextDownstreamNode != null ) {
            GridNodeNG tmpNode = nextDownstreamNode.goDownstreamSP();
            if (tmpNode == null) {
                assertTrue(nextDownstreamNode.isOutlet());
            }
            nextDownstreamNode = tmpNode;
        }
    }

    public void testEnteringCells() throws Exception {
        GridNodeNG node1 = elevationIter.getGridNodeNG(2, 2);

        List<GridNodeNG> enteringNodes = node1.getEnteringNodesSP();
        for( GridNodeNG flowNode : enteringNodes ) {
            assertEquals(flowNode.col, 3);
            assertEquals(flowNode.row, 1);
        }
    }

    public void testSurroundingCellValues() throws Exception {
        GridNodeNG node1 = elevationIter.getGridNodeNG(2, 2);

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
        GridNodeNG node1 = elevationIter.getGridNodeNG(2, 2);

        List<GridNodeNG> nonEnteringNodes = node1.getNonEnteringNodesSP();
        assertEquals(6, nonEnteringNodes.size());

        GridNodeNG node = nonEnteringNodes.get(0);
        assertEquals(node.col, 3);
        assertEquals(node.row, 2);
    }

    public void testTouchesBound() throws Exception {
        GridNodeNG node1 = elevationIter.getGridNodeNG(2, 2);
        boolean touchesNovalue = node1.touchesNovalue();
        assertTrue(touchesNovalue);

        node1 = elevationIter.getGridNodeNG(1, 5);
        touchesNovalue = node1.touchesNovalue();
        assertFalse(touchesNovalue);
    }

    public void testEnteringFlowCells() throws Exception {
        FlowNodeNG node = new FlowNodeNG(flowIter, 2, 2);

        List<FlowNodeNG> enteringNodes = node.getEnteringNodes();
        NodeNG flowNode = enteringNodes.get(0);
        assertEquals(flowNode.col, 3);
        assertEquals(flowNode.row, 1);

        node = new FlowNodeNG(flowIter, 5, 4);
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
        FlowNodeNG node = new FlowNodeNG(flowIter, 4, 1);

        FlowNodeNG n = node.goDownstream();
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
