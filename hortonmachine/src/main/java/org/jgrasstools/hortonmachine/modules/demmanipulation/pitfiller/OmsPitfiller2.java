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

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
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
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

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
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;

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
                for( GridNode pitNode : pitsList ) {
                    if (allNodesInPit.contains(pitNode)) {
                        pm.worked(1);
                        continue;
                    }
                    count++;

                    List<GridNode> nodesInPit = new ArrayList<>();
                    nodesInPit.add(pitNode);

                    double maxValue = Double.NEGATIVE_INFINITY;
                    int workingIndex = 0;
                    while( workingIndex < nodesInPit.size() ) {
                        List<GridNode> surroundingNodes = nodesInPit.get(workingIndex).getSurroundingNodes();
                        for( GridNode tmpNode : surroundingNodes ) {
                            if (tmpNode == null || tmpNode.touchesBound() || nodesInPit.contains(tmpNode)) {
                                continue;
                            }
                            List<GridNode> subSurroundingNodes = tmpNode.getSurroundingNodes();
                            subSurroundingNodes.removeAll(nodesInPit);

                            if (tmpNode.isPitFor(subSurroundingNodes)) {
                                nodesInPit.add(tmpNode);

                                double surroundingMin = Double.POSITIVE_INFINITY;
                                boolean touched = false;
                                for( GridNode gridNode : subSurroundingNodes ) {
                                    if (gridNode != null && gridNode.isValid()) {
                                        if (surroundingMin > gridNode.elevation) {
                                            surroundingMin = gridNode.elevation;
                                            touched = true;
                                        }
                                    }
                                }
                                if (touched && surroundingMin > maxValue) {
                                    maxValue = surroundingMin;
                                }
                            }
                        }
                        workingIndex++;
                    }

                    if (nodesInPit.size() == 1) {
                        maxValue = nodesInPit.get(0).getSurroundingMin();
                    }

                    if (Double.isInfinite(maxValue) || Double.isNaN(maxValue)) {
                        throw new RuntimeException("Found invalid value at: " + count);
                    }

                    PitInfo info = new PitInfo();
                    info.pitFillValue = maxValue;
                    info.nodes = nodesInPit;
                    pitInfoList.add(info);
                    allNodesInPit.addAll(nodesInPit);

                    pm.worked(1);
                }
                pm.done();

                for( PitInfo pitInfo : pitInfoList ) {
                    double value = pitInfo.pitFillValue;
                    List<GridNode> values = pitInfo.nodes;
                    pm.message("Flooding with value: " + value + " cells num: " + values.size());
                    for( GridNode gridNode : values ) {
                        gridNode.setValueInMap(pitIter, value);
                    }
                }

                pm.message("Calculating left pits...");
                pitsList = getPitsList(nCols, nRows, xRes, yRes, pitIter);
            }
            outPit = CoverageUtilities.buildCoverage("pitfiller", pitRaster, regionMap, inElev.getCoordinateReferenceSystem());
        } finally {
            pitIter.done();
        }
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

    private static class PitInfo {
        private double pitFillValue = doubleNovalue;

        List<GridNode> nodes;
    }
}
