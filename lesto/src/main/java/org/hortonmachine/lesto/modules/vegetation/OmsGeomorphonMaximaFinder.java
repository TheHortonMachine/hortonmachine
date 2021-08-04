/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.lesto.modules.vegetation;
import java.util.List;
import java.util.TreeSet;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.GridNodePositionComparator;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.geomorphon.GeomorphonClassification;
import org.hortonmachine.hmachine.modules.geomorphology.geomorphon.OmsGeomorphon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

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
import oms3.annotations.Unit;

@Description(OmsGeomorphonMaximaFinder.DESCRIPTION)
@Author(name = OmsGeomorphonMaximaFinder.AUTHORS, contact = OmsGeomorphonMaximaFinder.CONTACTS)
@Keywords(OmsGeomorphonMaximaFinder.KEYWORDS)
@Label(OmsGeomorphonMaximaFinder.LABEL)
@Name("_" + OmsGeomorphonMaximaFinder.NAME)
@Status(OmsGeomorphonMaximaFinder.STATUS)
@License(OmsGeomorphonMaximaFinder.LICENSE)
public class OmsGeomorphonMaximaFinder extends HMModel {

    @Description(inDTM_DESC)
    @In
    public GridCoverage2D inDTM;

    @Description(inDSM_DESC)
    @In
    public GridCoverage2D inDSM;

    @Description(pRadius_DESC)
    @Unit(pRadius_UNIT)
    @In
    public double pRadius;

    @Description(pThreshold_DESC)
    @Unit(pThreshold_UNIT)
    @In
    public double pThreshold = 1;

    @Description(pElevDiff_DESC)
    @Unit(pElevDiffThres_UNIT)
    @In
    public double pElevDiffThres = 1;

    @Description(outMaxima_DESC)
    @Out
    public SimpleFeatureCollection outMaxima;

    public static final String LABEL = HMConstants.LESTO + "/vegetation";
    public static final String LICENSE = HMConstants.GPL3_LICENSE;
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String NAME = "geomorphonmaximafinder";
    public static final String KEYWORDS = "raster, maxima, geomorphon";
    public static final String CONTACTS = "www.hydrologis.com";
    public static final String AUTHORS = "Andrea Antonello, Silvia Franceschi";
    public static final String DESCRIPTION = "The Geomorphon method to extract maxima from rasters";
    public static final String outMaxima_DESC = "Extracted maxima.";
    public static final String pElevDiff_DESC = "Elevation difference threshold.";
    public static final String pThreshold_DESC = "Vertical angle threshold.";
    public static final String pRadius_DESC = "Maximum search radius";
    public static final String inDSM_DESC = "The DSM.";
    public static final String inDTM_DESC = "The DTM.";
    public static final String pThreshold_UNIT = "degree";
    public static final String pRadius_UNIT = "m";
    public static final String pElevDiffThres_UNIT = "m";

    private int peakCode;

    private int hollowCode;

    private int valleyCode;

    private int pitCode;

    private int spurCode;

    @Execute
    public void process() throws Exception {
        checkNull(inDTM, inDSM);

        GridGeometry2D gridGeometry = inDSM.getGridGeometry();

        DummyProgressMonitor pm = new DummyProgressMonitor();
        OmsGeomorphon g = new OmsGeomorphon();
        g.pm = pm;
        g.inElev = inDSM;
        g.pRadius = pRadius;
        g.pThreshold = pThreshold;
        g.process();
        GridCoverage2D geomorphonGC = g.outRaster;

        OmsRasterDiff rasterDiff = new OmsRasterDiff();
        rasterDiff.inRaster1 = inDSM;
        rasterDiff.inRaster2 = inDTM;
        rasterDiff.pm = pm;
        rasterDiff.process();
        GridCoverage2D dsmDtmThresDiff = rasterDiff.outRaster;

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDSM);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        TreeSet<GridNode> topNodes = new TreeSet<GridNode>(new GridNodePositionComparator());
        peakCode = GeomorphonClassification.PEAK.getCode();
        hollowCode = GeomorphonClassification.HOLLOW.getCode();
        valleyCode = GeomorphonClassification.VALLEY.getCode();
        pitCode = GeomorphonClassification.PIT.getCode();
        spurCode = GeomorphonClassification.SPUR.getCode();

        double geomorphNv = HMConstants.getNovalue(geomorphonGC);
        double elevNv = HMConstants.getNovalue(dsmDtmThresDiff);

        RandomIter geomorphIter = CoverageUtilities.getRandomIterator(geomorphonGC);
        RandomIter elevIter = CoverageUtilities.getRandomIterator(dsmDtmThresDiff);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                GridNode geomorphNode = new GridNode(geomorphIter, cols, rows, 1, -1, c, r, geomorphNv);
                GridNode elevNode = new GridNode(elevIter, cols, rows, 1, -1, c, r, elevNv);
                if (geomorphNode.elevation == peakCode && !elevNode.touchesBound()) {
                    // found peak
                    boolean isLocalMaxima = true;
                    TreeSet<GridNode> peakNodes = new TreeSet<GridNode>(new GridNodePositionComparator());
                    peakNodes.add(geomorphNode);
                    gatherNodes(peakNodes, geomorphNode);

                    if (peakNodes.size() == 1) {
                        GridNode topNode = peakNodes.first();
                        GridNode topElevNode = new GridNode(elevIter, cols, rows, 1, -1, topNode.col, topNode.row, elevNv);
                        List<GridNode> validSurroundingNodes = topElevNode.getValidSurroundingNodes();
                        if (validSurroundingNodes.size() < 6) {
                            // no more than 2 invalid permitted
                            isLocalMaxima = false;
                        } else {
                            if (!analyzeNeighbors(topNode)) {
                                isLocalMaxima = false;
                            }
                        }
                    }

                    GridNode topNode = null;
                    if (isLocalMaxima) {
                        double maxElev = Double.NEGATIVE_INFINITY;
                        for( GridNode peakNode : peakNodes ) {
                            double elev = peakNode.getValueFromMap(elevIter);
                            if (elev > maxElev) {
                                maxElev = elev;
                                topNode = peakNode;
                            }
                        }
                        if (topNode != null) {
                            // check

                            GridNode topElevNode = new GridNode(elevIter, cols, rows, 1, -1, topNode.col, topNode.row, elevNv);
                            double[][] windowValues = topElevNode.getWindow(3, false);
                            double min = Double.POSITIVE_INFINITY;
                            double max = Double.NEGATIVE_INFINITY;
                            for( double[] windowRow : windowValues ) {
                                for( double windowValue : windowRow ) {
                                    if (HMConstants.isNovalue(windowValue)) {
                                        isLocalMaxima = false;
                                        break;
                                    } else {
                                        min = Math.min(min, windowValue);
                                        max = Math.max(max, windowValue);
                                    }
                                }
                                if (!isLocalMaxima) {
                                    break;
                                }
                            }
                            if (max - min > pElevDiffThres) {
                                isLocalMaxima = false;
                            }

                        }
                    }
                    if (isLocalMaxima && topNode != null) {
                        topNodes.add(topNode);
                    }
                }
            }
        }

        outMaxima = new DefaultFeatureCollection();
        SimpleFeatureBuilder builder = getOutBuilder();
        int id = 0;
        for( GridNode topNode : topNodes ) {
            Coordinate coordinate = CoverageUtilities.coordinateFromColRow(topNode.col, topNode.row, gridGeometry);
            Point point = gf.createPoint(coordinate);

            double elev = topNode.getValueFromMap(elevIter);
            Object[] values = new Object[]{point, id++, elev};
            try {
                builder.addAll(values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            SimpleFeature newFeature = builder.buildFeature(null);
            ((DefaultFeatureCollection) outMaxima).add(newFeature);

        }

        geomorphIter.done();
        elevIter.done();

        int size = outMaxima.size();
        if (size == 0) {
            pm.message("No tops extracted...");
        } else {
            pm.message("Extracted tops =  " + outMaxima.size());
        }

    }

    private void gatherNodes( TreeSet<GridNode> peakNodes, GridNode node ) {
        List<GridNode> surroundingNodes = node.getValidSurroundingNodes();
        for( GridNode surrNode : surroundingNodes ) {
            if (surrNode.elevation == peakCode) {
                if (peakNodes.add(surrNode)) {
                    gatherNodes(peakNodes, surrNode);
                }
            }
        }
    }

    private boolean analyzeNeighbors( GridNode topNode ) {

        // peak found without other peaks around
        // double newValue = HMConstants.doubleNovalue;
        // double newValue = gNode.elevation;
        double[][] window = topNode.getWindow(3, false);
        // identify the hollows > 1006
        int counthollow = 0;
        // identify the valleys > 1008
        int countvalley = 0;
        // identify the pits > 1009
        int countpit = 0;
        // identify the spurs > 1004
        int countspur = 0;
        for( int winRow = 0; winRow < window.length; winRow++ ) {
            for( int winCol = 0; winCol < window[0].length; winCol++ ) {
                double value = window[winRow][winCol];
                if (HMConstants.isNovalue(value)) {
                    continue;
                }
                if (value == hollowCode) {
                    counthollow++;
                } else if (value == valleyCode) {
                    countvalley++;
                } else if (value == pitCode) {
                    countpit++;
                } else if (value == spurCode) {
                    countspur++;
                }
            }
        }
        if (countpit > 0 && countvalley > 0) {
            return false;
        }
        if (countpit > 0 && countvalley > 0 && countspur > 0 && counthollow > 0) {
            return false;
        }

        return true;
    }

    private SimpleFeatureBuilder getOutBuilder() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("geomorphon");
        b.setCRS(inDSM.getCoordinateReferenceSystem());
        b.add("the_geom", Point.class);
        b.add("id", String.class);
        b.add("elev", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        return builder;
    }

}
