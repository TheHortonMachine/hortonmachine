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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_outPit_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

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

@Description(OMSPITFILLER_DESCRIPTION)
@Author(name = OMSPITFILLER_AUTHORNAMES, contact = OMSPITFILLER_AUTHORCONTACTS)
@Keywords(OMSPITFILLER_KEYWORDS)
@Label(OMSPITFILLER_LABEL)
@Name(OMSPITFILLER_NAME)
@Status(OMSPITFILLER_STATUS)
@License(OMSPITFILLER_LICENSE)
public class OmsPitfiller2 extends JGTModel {
    @Description(OMSPITFILLER_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev;

    @Description(OMSPITFILLER_outPit_DESCRIPTION)
    @Out
    public GridCoverage2D outPit = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        checkNull(inElev);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        int nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        double xRes = regionMap.get(CoverageUtilities.XRES);
        double yRes = regionMap.get(CoverageUtilities.YRES);

        // output raster
        WritableRaster pitRaster = CoverageUtilities.renderedImage2WritableRaster(inElev.getRenderedImage(), false);
        WritableRandomIter pitIter = CoverageUtilities.getWritableRandomIterator(pitRaster);
        try {

            List<GridNode> pitsList = getPitsList(nCols, nRows, xRes, yRes, pitIter);
            int iteration = 1;
            while( pitsList.size() > 0 ) {
                pm.message("Iteration number: " + iteration++);

                pm.message(msg.message("pitfiller.numpit") + pitsList.size());

                List<GridNode> allNodesInPit = new ArrayList<>();
                List<PitInfo> pitInfoList = new ArrayList<>();
                pm.beginTask("Processing pits...", pitsList.size());
                int count = 0;
                for( GridNode originalPitNode : pitsList ) {
                    if (allNodesInPit.contains(originalPitNode)) {
                        pm.worked(1);
                        continue;
                    }
                    count++;

                    if (originalPitNode.row == 248 && originalPitNode.col == 29) {
                        System.out.println();
                    }

                    List<GridNode> nodesInPit = new ArrayList<>();
                    nodesInPit.add(originalPitNode);

                    double maxValue = Double.NEGATIVE_INFINITY;
                    GridNode maxValueNode = null;
                    int workingIndex = 0;
                    while( workingIndex < nodesInPit.size() ) {
                        GridNode currentPitNode = nodesInPit.get(workingIndex);

                        List<GridNode> surroundingNodes = currentPitNode.getValidSurroundingNodes();
                        surroundingNodes.removeAll(nodesInPit);
                        double minElev = Double.POSITIVE_INFINITY;
                        GridNode minElevNode = null;
                        for( GridNode gridNode : surroundingNodes ) {
                            if (gridNode.elevation < minElev) {
                                minElev = gridNode.elevation;
                                minElevNode = gridNode;
                            }
                        }
                        if (minElevNode == null) {
                            workingIndex++;
                            continue;
                        }
                        List<GridNode> minElevSurroundingNodes = minElevNode.getValidSurroundingNodes();
                        minElevSurroundingNodes.removeAll(nodesInPit);
                        if (!minElevNode.isPitFor(minElevSurroundingNodes)) {
                            break;
                        }

                        for( GridNode tmpNode : surroundingNodes ) {
                            if (tmpNode.touchesBound()) {
                                continue;
                            }
                            List<GridNode> subSurroundingNodes = tmpNode.getSurroundingNodes();
                            subSurroundingNodes.removeAll(nodesInPit);

                            if (tmpNode.isPitFor(subSurroundingNodes)) {
                                nodesInPit.add(tmpNode);

                                double surroundingMin = Double.POSITIVE_INFINITY;
                                GridNode surroundingMinNode = null;
                                boolean touched = false;
                                for( GridNode gridNode : subSurroundingNodes ) {
                                    if (gridNode != null && gridNode.isValid()) {
                                        if (surroundingMin > gridNode.elevation) {
                                            surroundingMin = gridNode.elevation;
                                            surroundingMinNode = gridNode;
                                            touched = true;
                                        }
                                    }
                                }
                                if (touched && surroundingMin > maxValue) {
                                    maxValue = surroundingMin;
                                    maxValueNode = surroundingMinNode;
                                }
                            }
                        }
                        workingIndex++;
                    }

                    if (nodesInPit.size() == 1) {
                        GridNode gridNode = nodesInPit.get(0);
                        List<GridNode> validSurroundingNodes = gridNode.getValidSurroundingNodes();
                        double surroundingMin = Double.POSITIVE_INFINITY;
                        GridNode surroundingMinNode = null;
                        boolean touched = false;
                        for( GridNode tmp : validSurroundingNodes ) {
                            if (tmp != null && tmp.isValid()) {
                                if (surroundingMin > tmp.elevation) {
                                    surroundingMin = tmp.elevation;
                                    surroundingMinNode = tmp;
                                    touched = true;
                                }
                            }
                        }
                        if (touched && surroundingMin > maxValue) {
                            maxValue = surroundingMin;
                            maxValueNode = surroundingMinNode;
                        } else {
                            throw new RuntimeException();
                        }
                    }

                    if (Double.isInfinite(maxValue) || Double.isNaN(maxValue)) {
                        throw new RuntimeException("Found invalid value at: " + count);
                    }

                    PitInfo info = new PitInfo();
                    info.originalPitNode = originalPitNode;
                    info.pitfillExitNode = maxValueNode;
                    info.nodes = nodesInPit;
                    pitInfoList.add(info);
                    allNodesInPit.addAll(nodesInPit);

                    pm.worked(1);
                }
                pm.done();

                List<GridNode> nodesToCheckForLeftPits = new ArrayList<>();
                for( PitInfo pitInfo : pitInfoList ) {
                    GridNode pitfillExitNode = pitInfo.pitfillExitNode;
                    double exitElevation = pitfillExitNode.elevation;
                    List<GridNode> allPitsOfCurrent = pitInfo.nodes;
                    // pm.message("****************************");
                    // pm.message("Originalpit node: " + pitInfo.originalPitNode);
                    // pm.message("Exit: " + pitfillExitNode);
                    // pm.message("Flooding with value: " + value + " cells num: " +
                    // allPitsOfCurrent.size());
                    for( GridNode gridNode : allPitsOfCurrent ) {
                        gridNode.setValueInMap(pitIter, exitElevation);
                        //
                        // List<GridNode> validSurroundingNodes =
                        // gridNode.getValidSurroundingNodes();
                        // for( GridNode tmp : validSurroundingNodes ) {
                        // if (tmp.elevation != exitElevation) {
                        // if (!nodesToCheckForLeftPits.contains(tmp)) {
                        // nodesToCheckForLeftPits.add(tmp);
                        // }
                        // }
                        // }
                    }

                    HashSet<String> rowColsSet = new HashSet<>();
                    for( GridNode tmp : allPitsOfCurrent ) {
                        rowColsSet.add(tmp.row + "_" + tmp.col);
                    }

                    double delta = 2E-6;

                    List<GridNode> toCheck = new ArrayList<>();
                    toCheck.add(pitfillExitNode);
                    handleChecks(1, pitfillExitNode, toCheck, rowColsSet, exitElevation, pitIter, delta);

                }

                // pitsList = getPitsList(nCols, nRows, xRes, yRes, pitIter);
                pitsList = getPitsList(allNodesInPit, pitIter);
                pm.message("Left pits: " + pitsList.size());
                pm.message("---------------------------------------------------------------------");

            }

            outPit = CoverageUtilities.buildCoverage("pitfiller", pitRaster, regionMap, inElev.getCoordinateReferenceSystem());
        } finally {
            pitIter.done();
        }
    }

    private void handleChecks( int iteration, GridNode pitfillExitNode, List<GridNode> toCheck, HashSet<String> rowColsSet,
            double existElevation, WritableRandomIter pitIter, double delta ) {
        iteration++;

        List<GridNode> connected = new ArrayList<>();
        for( GridNode checkNode : toCheck ) {
            List<GridNode> validSurroundingNodes = checkNode.getValidSurroundingNodes();
            for( GridNode gridNode : validSurroundingNodes ) {
                if (!pitfillExitNode.equals(gridNode) && rowColsSet.contains(gridNode.row + "_" + gridNode.col)
                        && gridNode.elevation == existElevation) {
                    if (!connected.contains(gridNode))
                        connected.add(gridNode);
                }
            }
        }
        if (connected.size() == 0) {
            return;
        }
        for( GridNode gridNode : connected ) {
            gridNode.setValueInMap(pitIter, gridNode.elevation + delta * iteration);
        }
        handleChecks(iteration, pitfillExitNode, connected, rowColsSet, existElevation, pitIter, delta);
    }

    private List<GridNode> getPitsList( int nCols, int nRows, double xRes, double yRes, WritableRandomIter pitIter ) {
        List<GridNode> pitsList = new ArrayList<>();
        pm.beginTask("Extract pits from DTM...", IJGTProgressMonitor.UNKNOWN);;
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
        }
        pm.done();
        return pitsList;
    }

    private List<GridNode> getPitsList( List<GridNode> nodesToCheckForLeftPits, WritableRandomIter pitIter ) {
        List<GridNode> pitsList = new ArrayList<>();
        pm.beginTask("Extract pits from DTM...", nodesToCheckForLeftPits.size());
        for( GridNode tmp : nodesToCheckForLeftPits ) {
            pm.worked(1);
            List<GridNode> validSurroundingNodes = tmp.getValidSurroundingNodes();
            for( GridNode gridNode : validSurroundingNodes ) {
                if (gridNode.isPit()) {
                    double surroundingMin = gridNode.getSurroundingMin();
                    if (Double.isInfinite(surroundingMin)) {
                        continue;
                    }
                    if (!pitsList.contains(gridNode))
                        pitsList.add(gridNode);
                }
            }
        }
        pm.done();
        return pitsList;
    }

    private static class PitInfo {
        GridNode originalPitNode;
        GridNode pitfillExitNode;
        List<GridNode> nodes;
    }
}
