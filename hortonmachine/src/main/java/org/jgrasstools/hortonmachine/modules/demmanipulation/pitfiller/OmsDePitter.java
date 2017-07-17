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
package org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller;

import static org.jgrasstools.gears.libs.modules.JGTConstants.DEMMANIPULATION;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.BitMatrix;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.geomorphology.draindir.OmsDrainDir;

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

@Description(OmsDePitter.OMSDEPITTER_DESCRIPTION)
@Author(name = OmsDePitter.OMSDEPITTER_AUTHORNAMES, contact = OmsDePitter.OMSDEPITTER_AUTHORCONTACTS)
@Keywords(OmsDePitter.OMSDEPITTER_KEYWORDS)
@Label(OmsDePitter.OMSDEPITTER_LABEL)
@Name(OmsDePitter.OMSDEPITTER_NAME)
@Status(OmsDePitter.OMSDEPITTER_STATUS)
@License(OmsDePitter.OMSDEPITTER_LICENSE)
public class OmsDePitter extends JGTModel {
    @Description(OMSDEPITTER_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev;

    @Description(OMSDEPITTER_outPit_DESCRIPTION)
    @Out
    public GridCoverage2D outPit = null;

    @Description(OMSDEPITTER_outFlow_DESCRIPTION)
    @Out
    public GridCoverage2D outFlow = null;

    // @Description(OMSDEPITTER_outPitPoints_DESCRIPTION)
    // @Out
    // public SimpleFeatureCollection outPitPoints = null;

    public static final String OMSDEPITTER_DESCRIPTION = "The module fills the depression points present within a DEM and generates a map of flowdirections that also handles flat areas.";
    public static final String OMSDEPITTER_DOCUMENTATION = "";
    public static final String OMSDEPITTER_KEYWORDS = "Dem manipulation, Geomorphology";
    public static final String OMSDEPITTER_LABEL = DEMMANIPULATION;
    public static final String OMSDEPITTER_NAME = "depit";
    public static final int OMSDEPITTER_STATUS = Status.EXPERIMENTAL;
    public static final String OMSDEPITTER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSDEPITTER_AUTHORNAMES = "Andrea Antonello, Silvia Franceschi";
    public static final String OMSDEPITTER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSDEPITTER_inElev_DESCRIPTION = "The map of digital elevation model (DEM).";
    public static final String OMSDEPITTER_outPit_DESCRIPTION = "The depitted elevation map.";
    public static final String OMSDEPITTER_outPitPoints_DESCRIPTION = "The shapefile of handled pits.";
    public static final String OMSDEPITTER_outFlow_DESCRIPTION = "The map of D8 flowdirections.";

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private final float delta = 2E-6f;
    private boolean verbose = true;

    @Execute
    public void process() throws Exception {
        checkNull(inElev);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        // output raster
        WritableRaster pitRaster = CoverageUtilities.renderedImage2DoubleWritableRaster(inElev.getRenderedImage(), false);
        WritableRandomIter pitIter = CoverageUtilities.getWritableRandomIterator(pitRaster);
        try {

            ConcurrentLinkedQueue<GridNode> pitsList = getPitsList(nCols, nRows, xRes, yRes, pitIter);
            // ConcurrentLinkedQueue<PitInfo> flatsList = getFlatsList(nCols, nRows, xRes, yRes,
            // pitIter, allPitsPositions, allFlatsPositions);

            int count = 0;
            int iteration = 1;
            while( pitsList.size() > 0 ) {// || flatsList.size() > 0 ) {
                if (pm.isCanceled()) {
                    return;
                }
                BitMatrix allPitsPositions = new BitMatrix(nCols, nRows);
                // BitMatrix allFlatsPositions = new BitMatrix(nCols, nRows);

                int pitCount = pitsList.size();

                List<GridNode> allNodesInPit = new ArrayList<>();
                List<PitInfo> pitInfoList = new ArrayList<>();

                int shownCount = pitCount;
                if (!verbose) {
                    shownCount = IJGTProgressMonitor.UNKNOWN;
                }
                pm.beginTask("Processing " + pitCount + " pits (iteration N." + iteration++ + ")... ", shownCount);
                for( GridNode originalPitNode : pitsList ) {
                    if (allNodesInPit.contains(originalPitNode)) {
                        if (verbose)
                            pm.worked(1);
                        continue;
                    }
                    count++;
                    if (pm.isCanceled()) {
                        return;
                    }

                    List<GridNode> nodesInPit = new ArrayList<>();
                    nodesInPit.add(originalPitNode);
                    allPitsPositions.mark(originalPitNode.col, originalPitNode.row);

                    double maxValue = Double.NEGATIVE_INFINITY;
                    GridNode maxValueNode = null;
                    int workingIndex = 0;
                    while( workingIndex < nodesInPit.size() ) {
                        if (pm.isCanceled()) {
                            return;
                        }
                        GridNode currentPitNode = nodesInPit.get(workingIndex);

                        List<GridNode> surroundingNodes = new ArrayList<>(currentPitNode.getValidSurroundingNodes());
                        removeExistingPits(allPitsPositions, surroundingNodes);
                        // surroundingNodes.removeAll(nodesInPit);

                        GridNode minNode = getMinElevNode(surroundingNodes, allPitsPositions);
                        if (minNode == null) {
                            workingIndex++;
                            continue;
                        }

                        List<GridNode> minElevSurroundingNodes = new ArrayList<>(minNode.getValidSurroundingNodes());
                        removeExistingPits(allPitsPositions, minElevSurroundingNodes);
                        // minElevSurroundingNodes.removeAll(nodesInPit);
                        if (!minNode.isPitFor(minElevSurroundingNodes)) {
                            nodesInPit.add(minNode);
                            allPitsPositions.mark(minNode.col, minNode.row);

                            maxValue = minNode.elevation;
                            maxValueNode = minNode;
                            break;
                        }

                        for( GridNode tmpNode : surroundingNodes ) {
                            if (tmpNode.touchesBound() || !tmpNode.isValid()) {
                                continue;
                            }
                            List<GridNode> subSurroundingNodes = new ArrayList<>(tmpNode.getValidSurroundingNodes());
                            // subSurroundingNodes.removeAll(nodesInPit);
                            removeExistingPits(allPitsPositions, subSurroundingNodes);

                            if (tmpNode.isPitFor(subSurroundingNodes)) {
                                nodesInPit.add(tmpNode);
                                allPitsPositions.mark(tmpNode.col, tmpNode.row);

                                GridNode subMinNode = getMinElevNode(subSurroundingNodes, allPitsPositions);
                                if (subMinNode != null && subMinNode.elevation > maxValue) {
                                    maxValue = subMinNode.elevation;
                                    maxValueNode = subMinNode;
                                }
                            }
                        }
                        workingIndex++;
                    }

                    if (Double.isInfinite(maxValue)) {
                        for( GridNode gridNode : nodesInPit ) {
                            if (gridNode.elevation > maxValue) {
                                maxValue = gridNode.elevation;
                                maxValueNode = gridNode;
                            }
                        }

                        // GridNode gridNode = nodesInPit.get(0);
                        // List<GridNode> validSurroundingNodes =
                        // gridNode.getValidSurroundingNodes();
                        // GridNode minNode = getMinElevNode(validSurroundingNodes,
                        // allPitsPositions);
                        // if (minNode != null && minNode.elevation > maxValue) {
                        // maxValue = minNode.elevation;
                        // maxValueNode = minNode;
                        // } else {
                        // throw new RuntimeException();
                        // }
                    }

                    if (Double.isInfinite(maxValue) || Double.isNaN(maxValue)) {
                        throw new RuntimeException("Found invalid value at: " + count);
                    }

                    allPitsPositions.mark(maxValueNode.col, maxValueNode.row);
                    // add connected that have same value of the future flooded pit
                    findConnectedOfSameHeight(maxValueNode, allPitsPositions, nodesInPit);

                    PitInfo info = new PitInfo();
                    // info.originalPitNode = originalPitNode;
                    info.pitfillExitNode = maxValueNode;
                    info.nodes = nodesInPit;
                    pitInfoList.add(info);
                    allNodesInPit.addAll(nodesInPit);

                    if (verbose)
                        pm.worked(1);
                }
                pm.done();

                // BitMatrix markedPositions = new BitMatrix(nCols, nRows);
                if (verbose)
                    pm.beginTask("Flood pits...", pitInfoList.size());
                pitInfoList.stream().forEach(pitInfo -> {
                    if (pm.isCanceled()) {
                        return;
                    }
                    GridNode pitfillExitNode = pitInfo.pitfillExitNode;
                    double exitElevation = pitfillExitNode.elevation;
                    List<GridNode> allPitsOfCurrent = pitInfo.nodes;
                    for( GridNode gridNode : allPitsOfCurrent ) {
                        gridNode.setValueInMap(pitIter, exitElevation);
                        // markedPositions.mark(gridNode.col, gridNode.row);
                        allPitsPositions.mark(gridNode.col, gridNode.row);
                    }

                    List<GridNode> cellsToMakeFlowReady = new ArrayList<>();
                    GridNode startNode = new GridNode(pitIter, nCols, nRows, xRes, yRes, pitfillExitNode.col,
                            pitfillExitNode.row);
                    cellsToMakeFlowReady.add(startNode);
                    allPitsPositions.mark(startNode.col, startNode.row);
                    makeCellsFlowReady(0, startNode, cellsToMakeFlowReady, allPitsPositions, pitIter, delta);
                    if (verbose)
                        pm.worked(1);
                });
                if (verbose)
                    pm.done();

                // for( int row = 0; row < nRows; row++ ) {
                // for( int col = 0; col < nCols; col++ ) {
                // System.out.print(pitIter.getSampleDouble(col, row, 0) + " ");
                // }
                // System.out.println();
                // }

                // flatsList = getFlatsList(nCols, nRows, xRes, yRes, pitIter, allPitsPositions,
                // allFlatsPositions);
                // pm.beginTask("Handle flats...", flatsList.size());
                // flatsList.stream().forEach(pitInfo -> {
                // if (pm.isCanceled()) {
                // return;
                // }
                // GridNode lowestNode = pitInfo.pitfillExitNode;
                // List<GridNode> cellsToMakeFlowReady = new ArrayList<>();
                // GridNode startNode = new GridNode(pitIter, nCols, nRows, xRes, yRes,
                // lowestNode.col, lowestNode.row);
                // cellsToMakeFlowReady.add(startNode);
                // makeFlatsFlowReady(0, startNode, cellsToMakeFlowReady, allPitsPositions,
                // allFlatsPositions, pitIter, delta);
                // pm.worked(1);
                // });
                // pm.done();

                // for( int row = 0; row < nRows; row++ ) {
                // for( int col = 0; col < nCols; col++ ) {
                // System.out.print(pitIter.getSampleDouble(col, row, 0) + " ");
                // }
                // System.out.println();
                // }

                // allPitsPositions = new BitMatrix(nCols, nRows);
                pitsList = getPitsList(nCols, nRows, xRes, yRes, pitIter, allPitsPositions);
                // flatsList = getFlatsList(nCols, nRows, xRes, yRes, pitIter, allPitsPositions,
                // allFlatsPositions);

                // List<GridNode> updatedPitsPositions = new ArrayList<>(allNodesInPit.size());
                // for( GridNode gridNode : allNodesInPit ) {
                // GridNode updated = new GridNode(pitIter, nCols, nRows, xRes, yRes, gridNode.col,
                // gridNode.row);
                // updatedPitsPositions.add(updated);
                // }
                // // only re-check the cells that are adiacent to what has been modified
                // pitsList = getPitsList(updatedPitsPositions, allPitsPositions);

                int size = pitsList.size();
                pm.message("Left pits: " + size);
                pm.message("---------------------------------------------------------------------");
                if (size < 10000) {
                    verbose = false;
                }

            }

            outPit = CoverageUtilities.buildCoverage("pitfiller", pitRaster, regionMap, inElev.getCoordinateReferenceSystem());
            // GridGeometry2D gridGeometry = inElev.getGridGeometry();
            //
            // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            // b.setName("pitpoints");
            // b.setCRS(inElev.getCoordinateReferenceSystem());
            // b.add("the_geom", Point.class);
            // b.add("type", String.class);
            // b.add("origelev", Double.class);
            // b.add("newelev", Double.class);
            // SimpleFeatureType type = b.buildFeatureType();
            // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            //
            // outPitPoints = new DefaultFeatureCollection();
            //
            // pm.beginTask("Generating pitpoints...", nRows);
            // for( int r = 0; r < nRows; r++ ) {
            // if (pm.isCanceled()) {
            // return;
            // }
            // for( int c = 0; c < nCols; c++ ) {
            // if (allPitsPositions.isMarked(c, r)) {
            // Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
            // Point point = gf.createPoint(coordinate);
            // double origValue = CoverageUtilities.getValue(inElev, c, r);
            // double newValue = pitIter.getSampleDouble(c, r, 0);
            //
            // Object[] values = new Object[]{point, "pit", origValue, newValue};
            // builder.addAll(values);
            // SimpleFeature feature = builder.buildFeature(null);
            // ((DefaultFeatureCollection) outPitPoints).add(feature);
            // } else if (allFlatsPositions.isMarked(c, r)) {
            // Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
            // Point point = gf.createPoint(coordinate);
            // double origValue = CoverageUtilities.getValue(inElev, c, r);
            // double newValue = pitIter.getSampleDouble(c, r, 0);
            //
            // Object[] values = new Object[]{point, "flat", origValue, newValue};
            // builder.addAll(values);
            // SimpleFeature feature = builder.buildFeature(null);
            // ((DefaultFeatureCollection) outPitPoints).add(feature);
            // }
            // }
            // }

            WritableRaster flowRaster = CoverageUtilities.createWritableRaster(nCols, nRows, Integer.class, null, null);
            WritableRandomIter flowIter = CoverageUtilities.getWritableRandomIterator(flowRaster);
            try {
                pm.beginTask("Calculating flowdirections...", nRows);
                for( int r = 0; r < nRows; r++ ) {
                    if (pm.isCanceled()) {
                        return;
                    }
                    for( int c = 0; c < nCols; c++ ) {
                        GridNode node = new GridNode(pitIter, nCols, nRows, xRes, yRes, c, r);
                        boolean isValid = node.isValid();
                        if (!isValid || node.touchesBound() || node.touchesNovalue()) {
                            flowIter.setSample(c, r, 0, JGTConstants.intNovalue);
                        } else {
                            GridNode nextDown = node.goDownstreamSP();
                            if (nextDown == null) {// || nextDown.touchesNovalue() ||
                                                   // nextDown.touchesBound()) {
                                flowIter.setSample(c, r, 0, JGTConstants.intNovalue);
                                // flowIter.setSample(c, r, 0, FlowNode.OUTLET);
                            } else {
                                int newFlow = node.getFlow();
                                flowIter.setSample(c, r, 0, newFlow);
                            }
                        }
                    }
                    pm.worked(1);
                }
                pm.done();

                outFlow = CoverageUtilities.buildCoverage("flow", flowRaster, regionMap, inElev.getCoordinateReferenceSystem());
            } finally {
                flowIter.done();
            }

        } finally {
            pitIter.done();
        }
    }

    private void removeExistingPits( BitMatrix allPitsPositions, List<GridNode> surroundingNodes ) {
        Iterator<GridNode> nodesIter = surroundingNodes.iterator();
        while( nodesIter.hasNext() ) {
            GridNode gridNode = (GridNode) nodesIter.next();
            if (allPitsPositions.isMarked(gridNode.col, gridNode.row)) {
                nodesIter.remove();
            }
        }
    }

    private GridNode getMinElevNode( List<GridNode> surroundingNodes, BitMatrix allPitsPositions ) {
        double minElev = Double.POSITIVE_INFINITY;
        GridNode minNode = null;
        for( GridNode gridNode : surroundingNodes ) {
            if (gridNode.elevation < minElev && !allPitsPositions.isMarked(gridNode.col, gridNode.row)) {
                minElev = gridNode.elevation;
                minNode = gridNode;
            }
        }
        return minNode;
    }

    /**
     * Make cells flow ready by creating a slope starting from the output cell.
     * 
     * @param iteration the iteration.
     * @param pitfillExitNode the exit node.
     * @param cellsToMakeFlowReady the cells to check and change at each iteration.
     * @param allPitsPositions the marked positions of all existing pits. Necessary to pick only those that 
     *              really are part of the pit pool.
     * @param pitIter elevation data.
     * @param delta the elevation delta to add to the cells to create the slope.
     */
    private void makeCellsFlowReady( int iteration, GridNode pitfillExitNode, List<GridNode> cellsToMakeFlowReady,
            BitMatrix allPitsPositions, WritableRandomIter pitIter, float delta ) {
        iteration++;

        double exitElevation = pitfillExitNode.elevation;
        List<GridNode> connected = new ArrayList<>();
        for( GridNode checkNode : cellsToMakeFlowReady ) {
            List<GridNode> validSurroundingNodes = checkNode.getValidSurroundingNodes();
            for( GridNode gridNode : validSurroundingNodes ) {
                if (!pitfillExitNode.equals(gridNode) && allPitsPositions.isMarked(gridNode.col, gridNode.row)
                        && gridNode.elevation == exitElevation) {
                    if (!connected.contains(gridNode))
                        connected.add(gridNode);
                }
            }
        }
        if (connected.size() == 0) {
            return;
        }

        for( GridNode gridNode : connected ) {
            double newElev = (double) (gridNode.elevation + delta * (double) iteration);
            gridNode.setValueInMap(pitIter, newElev);
        }
        List<GridNode> updatedConnected = new ArrayList<>();
        for( GridNode gridNode : connected ) {
            GridNode updatedNode = new GridNode(pitIter, gridNode.cols, gridNode.rows, gridNode.xRes, gridNode.yRes, gridNode.col,
                    gridNode.row);
            updatedConnected.add(updatedNode);
        }
        makeCellsFlowReady(iteration, pitfillExitNode, updatedConnected, allPitsPositions, pitIter, delta);
    }

    private void makeFlatsFlowReady( int iteration, GridNode pitfillExitNode, List<GridNode> cellsToMakeFlowReady,
            BitMatrix allPitsPositions, BitMatrix allFlatsPositions, WritableRandomIter pitIter, float delta ) {
        iteration++;

        double exitElevation = pitfillExitNode.elevation;
        List<GridNode> connected = new ArrayList<>();
        for( GridNode checkNode : cellsToMakeFlowReady ) {
            List<GridNode> validSurroundingNodes = checkNode.getValidSurroundingNodes();
            for( GridNode gridNode : validSurroundingNodes ) {
                if (!pitfillExitNode.equals(gridNode) && allFlatsPositions.isMarked(gridNode.col, gridNode.row)
                        && !allPitsPositions.isMarked(gridNode.col, gridNode.row) && gridNode.elevation == exitElevation) {
                    if (!connected.contains(gridNode))
                        connected.add(gridNode);
                }
            }
        }
        if (connected.size() == 0) {
            return;
        }

        for( GridNode gridNode : connected ) {
            double newElev = (double) (gridNode.elevation + delta * (double) iteration);
            gridNode.setValueInMap(pitIter, newElev);
        }
        List<GridNode> updatedConnected = new ArrayList<>();
        for( GridNode gridNode : connected ) {
            GridNode updatedNode = new GridNode(pitIter, gridNode.cols, gridNode.rows, gridNode.xRes, gridNode.yRes, gridNode.col,
                    gridNode.row);
            updatedConnected.add(updatedNode);
        }
        makeFlatsFlowReady(iteration, pitfillExitNode, updatedConnected, allPitsPositions, allFlatsPositions, pitIter, delta);
    }

    private ConcurrentLinkedQueue<GridNode> getPitsList( int nCols, int nRows, double xRes, double yRes,
            WritableRandomIter pitIter ) {
        ConcurrentLinkedQueue<GridNode> pitsList = new ConcurrentLinkedQueue<>();
        if (verbose)
            pm.beginTask("Extract pits from DTM...", nRows);
        for( int row = 0; row < nRows; row++ ) {
            for( int col = 0; col < nCols; col++ ) {
                GridNode node = new GridNode(pitIter, nCols, nRows, xRes, yRes, col, row);
                if (node.isPit()) {
                    double surroundingMin = node.getSurroundingMin();
                    if (Double.isInfinite(surroundingMin)) {
                        continue;
                    }
                    pitsList.add(node);
                }
            }
            if (verbose)
                pm.worked(1);
        }
        if (verbose)
            pm.done();
        return pitsList;
    }

    private ConcurrentLinkedQueue<GridNode> getPitsList( int nCols, int nRows, double xRes, double yRes,
            WritableRandomIter pitIter, BitMatrix allPitsPositions ) {
        ConcurrentLinkedQueue<GridNode> pitsList = new ConcurrentLinkedQueue<>();

        BitMatrix currentMarked = new BitMatrix(nCols, nRows);
        if (verbose)
            pm.beginTask("Extract pits from DTM...", nRows);
        for( int row = 0; row < nRows; row++ ) {
            for( int col = 0; col < nCols; col++ ) {
                if (!allPitsPositions.isMarked(col, row)) {
                    continue;
                }

                GridNode node = new GridNode(pitIter, nCols, nRows, xRes, yRes, col, row);
                if (node.isPit()) {
                    double surroundingMin = node.getSurroundingMin();
                    if (Double.isInfinite(surroundingMin)) {
                        continue;
                    }
                    pitsList.add(node);
                }

                // check also border ones
                List<GridNode> validSurroundingNodes = node.getValidSurroundingNodes();
                for( GridNode gridNode : validSurroundingNodes ) {
                    if (allPitsPositions.isMarked(gridNode.col, gridNode.row)
                            || currentMarked.isMarked(gridNode.col, gridNode.row)) {
                        // we want only border
                        continue;
                    }
                    if (gridNode.isPit()) {
                        double surroundingMin = gridNode.getSurroundingMin();
                        if (Double.isInfinite(surroundingMin)) {
                            continue;
                        }
                        pitsList.add(gridNode);
                        currentMarked.mark(gridNode.col, gridNode.row);
                    }
                }

            }
            if (verbose)
                pm.worked(1);
        }
        if (verbose)
            pm.done();
        return pitsList;
    }

    // private ConcurrentLinkedQueue<PitInfo> getFlatsList( int nCols, int nRows, double xRes,
    // double yRes,
    // WritableRandomIter pitIter, BitMatrix allPitsPositions, BitMatrix allFlatsPositions ) {
    // ConcurrentLinkedQueue<PitInfo> flatsList = new ConcurrentLinkedQueue<>();
    // pm.beginTask("Extract flats from DTM...", nRows);
    // for( int row = 0; row < nRows; row++ ) {
    // for( int col = 0; col < nCols; col++ ) {
    // if (allFlatsPositions.isMarked(col, row)) {
    // continue;
    // }
    // GridNode node = new GridNode(pitIter, nCols, nRows, xRes, yRes, col, row);
    // if (node.isFlat() && !allPitsPositions.isMarked(col, row)) {
    // PitInfo lowestOfFlat = findLowest(node, allPitsPositions, allFlatsPositions);
    // if (lowestOfFlat != null) {
    // flatsList.add(lowestOfFlat);
    // }
    // }
    // }
    // pm.worked(1);
    // }
    // pm.done();
    // return flatsList;
    // }

    // private PitInfo findLowest( GridNode node, BitMatrix allPitsPositions, BitMatrix
    // allFlatsPositions ) {
    // if (allFlatsPositions.isMarked(node.col, node.row)) {
    // return null;
    // }
    // allFlatsPositions.mark(node.col, node.row);
    // double lowest = node.getSurroundingMin();
    // double elev = node.elevation;
    // GridNode nodeWithLowestsurrounding = node;
    //
    // List<GridNode> checkNodes = new ArrayList<>();
    // checkNodes.add(node);
    // boolean oneAdded = true;
    // int startIndex = 0;
    // while( oneAdded ) {
    // oneAdded = false;
    // int currentSize = checkNodes.size();
    // for( int i = startIndex; i < currentSize; i++ ) {
    // GridNode checkNode = checkNodes.get(i);
    // List<GridNode> tmpNodes = checkNode.getValidSurroundingNodes();
    // for( GridNode gridNode : tmpNodes ) {
    // if (allPitsPositions.isMarked(gridNode.col, gridNode.row)
    // || allFlatsPositions.isMarked(gridNode.col, gridNode.row)) {
    // continue;
    // }
    //
    // if (gridNode.elevation == elev) {
    // if (!checkNodes.contains(gridNode) && !gridNode.isPit()) {
    // checkNodes.add(gridNode);
    // allFlatsPositions.mark(gridNode.col, gridNode.row);
    // oneAdded = true;
    // // get the surrounding min that is not a pit
    // List<GridNode> validSurroundingNodes = gridNode.getValidSurroundingNodes();
    // double surroundingMin = Double.POSITIVE_INFINITY;// gridNode.getSurroundingMin();
    // for( GridNode tmpNode : validSurroundingNodes ) {
    // if (tmpNode.isValid() && !tmpNode.isPit() && tmpNode.elevation < surroundingMin) {
    // surroundingMin = tmpNode.elevation;
    // }
    // }
    // if (surroundingMin < lowest) {
    // nodeWithLowestsurrounding = gridNode;
    // lowest = surroundingMin;
    // }
    // }
    // }
    // }
    // }
    // startIndex = currentSize;
    // }
    //
    // PitInfo info = new PitInfo();
    // info.pitfillExitNode = nodeWithLowestsurrounding;
    // info.nodes = checkNodes;
    // return info;
    // }

    private void findConnectedOfSameHeight( GridNode node, BitMatrix allPitsPositions, List<GridNode> nodesInPit ) {
        double elev = node.elevation;
        List<GridNode> checkNodes = new ArrayList<>();
        checkNodes.add(node);
        boolean oneAdded = true;
        int startIndex = 0;
        while( oneAdded ) {
            oneAdded = false;
            int currentSize = checkNodes.size();
            for( int i = startIndex; i < currentSize; i++ ) {
                GridNode checkNode = checkNodes.get(i);
                List<GridNode> tmpNodes = checkNode.getValidSurroundingNodes();
                for( GridNode gridNode : tmpNodes ) {
                    if (allPitsPositions.isMarked(gridNode.col, gridNode.row)) {
                        continue;
                    }

                    if (gridNode.elevation == elev) {
                        checkNodes.add(gridNode);
                        nodesInPit.add(gridNode);
                        allPitsPositions.mark(gridNode.col, gridNode.row);
                        oneAdded = true;
                    }
                }
            }
            startIndex = currentSize;
        }

    }

    // private ConcurrentLinkedQueue<GridNode> getPitsList( List<GridNode> nodesToCheckForLeftPits,
    // BitMatrix allPitsPositions ) {
    // ConcurrentLinkedQueue<GridNode> pitsList = new ConcurrentLinkedQueue<>();
    // if (nodesToCheckForLeftPits.size() > 0) {
    // GridNode tmp = nodesToCheckForLeftPits.get(0);
    // BitMatrix existing = new BitMatrix(tmp.cols, tmp.rows);
    // pm.beginTask("Extract pits from the cells surrounding the pit pool...",
    // nodesToCheckForLeftPits.size());
    // nodesToCheckForLeftPits.stream().forEach(node -> {
    // List<GridNode> validSurroundingNodes = node.getValidSurroundingNodes();
    // for( GridNode gridNode : validSurroundingNodes ) {
    // if (allPitsPositions.isMarked(gridNode.col, gridNode.row)) {
    // // previously flattened pits
    // continue;
    // }
    // if (gridNode.isPit()) {
    // double surroundingMin = gridNode.getSurroundingMin();
    // if (Double.isInfinite(surroundingMin)) {
    // continue;
    // }
    // if (!existing.isMarked(gridNode.col, gridNode.row)) {
    // pitsList.add(gridNode);
    // existing.mark(gridNode.col, gridNode.row);
    // }
    // }
    // }
    // pm.worked(1);
    // });
    // pm.done();
    // }
    // return pitsList;
    // }

    private static class PitInfo {
        // GridNode originalPitNode;
        GridNode pitfillExitNode;
        List<GridNode> nodes = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Exit node: ").append(pitfillExitNode).append("\n");
            sb.append("Number of connected nodes: " + nodes.size() + "\n");
            for( GridNode gridNode : nodes ) {
                sb.append("  -> col=" + gridNode.col + " row=" + gridNode.row + "\n");
            }
            return sb.toString();
        }
    }

    public static void main( String[] args ) throws Exception {

//        String dtm = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/dtm_flanginec.tiff";
//        String pit = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/pit_flanginec.tiff";
//        String flow = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flow_flanginec.tiff";
//        String drain = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/drain_flanginec.tiff";
//        String tca = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/tca_flanginec.tiff";

         String dtm = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/dtm_toblino.tiff";
         String pit = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/pit.tiff";
         String flow = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/flow.tiff";
         String drain = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/drain.tiff";
         String tca = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/tca.tiff";

        // String dtm =
        // "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/dtm_all_float.tiff";
        // String pit = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/pit.tiff";
        // String flow = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/flow.tiff";
        // String drain = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/drain.tiff";
        // String tca = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/tca.tiff";

        OmsDePitter pitfiller = new OmsDePitter();
        pitfiller.inElev = OmsRasterReader.readRaster(dtm);
        pitfiller.process();
        OmsRasterWriter.writeRaster(pit, pitfiller.outPit);
        OmsRasterWriter.writeRaster(flow, pitfiller.outFlow);
        // OmsVectorWriter.writeVector(pitPoints, pitfiller.outPitPoints);

        OmsDrainDir draindir = new OmsDrainDir();
        draindir.inPit = OmsRasterReader.readRaster(pit);
        draindir.inFlow = OmsRasterReader.readRaster(flow);
        // draindir.inFlownet = OmsRasterReader.readRaster(inFlownet);
        draindir.pLambda = 1f;
        draindir.process();
        OmsRasterWriter.writeRaster(drain, draindir.outFlow);
        OmsRasterWriter.writeRaster(tca, draindir.outTca);
    }

}
