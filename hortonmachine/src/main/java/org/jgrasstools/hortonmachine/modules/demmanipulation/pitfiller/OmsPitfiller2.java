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

    private WritableRandomIter pitIter;
    private RandomIter elevationIter = null;

    private int nCols;
    private int nRows;
    private double xRes;
    private double yRes;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {
        checkNull(inElev);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        elevationIter = CoverageUtilities.getRandomIterator(inElev);

        // output raster
        WritableRaster pitRaster = CoverageUtilities.renderedImage2WritableRaster(inElev.getRenderedImage(), false);
        pitIter = CoverageUtilities.getWritableRandomIterator(pitRaster);

        List<GridNode> pitsList = new ArrayList<>();
        for( int row = 0; row < nRows; row++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int col = 0; col < nCols; col++ ) {
                GridNode node = new GridNode(elevationIter, nCols, nRows, xRes, yRes, col, row);
                if (node.isPit()) {
                    pitsList.add(node);
                    System.out.println(row + "/" + col + " ->  " + node.elevation + "/" + node.getSurroundingMin());
                }
            }
        }

        pm.message(msg.message("pitfiller.numpit") + pitsList.size());

        List<GridNode> allNodesInPit = new ArrayList<>();
        HashMap<Double, List<GridNode>> pitValue2Nodes = new HashMap<>();
        for( GridNode pitNode : pitsList ) {
            if (allNodesInPit.contains(pitNode)) {
                continue;
            }

            List<GridNode> nodesInPit = new ArrayList<>();
            nodesInPit.add(pitNode);

            double maxValue = Double.NEGATIVE_INFINITY;
            int workingIndex = 0;
            while( workingIndex < nodesInPit.size() ) {
                List<GridNode> surroundingNodes = nodesInPit.get(workingIndex).getSurroundingNodes();
                for( GridNode tmpNode : surroundingNodes ) {
                    if (tmpNode.touchesBound() || nodesInPit.contains(tmpNode)) {
                        continue;
                    }
                    List<GridNode> subSurroundingNodes = tmpNode.getSurroundingNodes();
                    subSurroundingNodes.removeAll(nodesInPit);

                    if (tmpNode.isPitFor(subSurroundingNodes)) {
                        nodesInPit.add(tmpNode);

                        double surroundingMin = Double.POSITIVE_INFINITY;
                        for( GridNode gridNode : subSurroundingNodes ) {
                            if (gridNode.isValid()) {
                                if (surroundingMin > gridNode.elevation) {
                                    surroundingMin = gridNode.elevation;
                                }
                            }
                        }
                        if (surroundingMin > maxValue) {
                            maxValue = surroundingMin;
                        }
                    }
                }
                workingIndex++;
            }

            if (nodesInPit.size() == 1) {
                maxValue = nodesInPit.get(0).getSurroundingMin();
            }
            pitValue2Nodes.put(maxValue, nodesInPit);
            allNodesInPit.addAll(nodesInPit);
        }

        for( Entry<Double, List<GridNode>> entry : pitValue2Nodes.entrySet() ) {
            double value = entry.getKey();
            List<GridNode> values = entry.getValue();
            for( GridNode gridNode : values ) {
                gridNode.setValueInMap(pitIter, value);
            }
        }

        outPit = CoverageUtilities.buildCoverage("pitfiller", pitRaster, regionMap, inElev.getCoordinateReferenceSystem());
    }
}
