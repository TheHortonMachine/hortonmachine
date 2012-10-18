package org.jgrasstools.gears;

import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.GridNode;
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

    private static final double delta = 0.000000001;
    private int nCols;
    private int nRows;
    private double xRes;
    private double yRes;
    private RandomIter elevationIter;

    protected void setUp() throws Exception {
        double[][] mapData = HMTestMaps.mapData;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        GridCoverage2D inElev = CoverageUtilities.buildCoverage("elevation", mapData, envelopeParams, crs, true);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        nCols = regionMap.getCols();
        nRows = regionMap.getRows();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();

        elevationIter = CoverageUtilities.getRandomIterator(inElev);
    }

    public void testSlopeTo() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 0, 0);
        GridNode node2 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 0, 1);
        double slopeTo = node1.getSlopeTo(node2);
        assertEquals(slopeTo, 6.666666666666667, delta);
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

    public void testNonEnteringCells() throws Exception {
        GridNode node1 = new GridNode(elevationIter, nCols, nRows, xRes, yRes, 2, 2);

        List<GridNode> nonEnteringNodes = node1.getNonEnteringNodesSP();
        assertEquals(6, nonEnteringNodes.size());

        GridNode node = nonEnteringNodes.get(0);
        assertEquals(node.col, 3);
        assertEquals(node.row, 2);
    }

}
