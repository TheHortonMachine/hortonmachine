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

import org.eclipse.imagen.iterator.RandomIter;

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
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
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
@Documentation(OmsGeomorphonMaximaFinder.DOCUMENTATION)
@Author(name = OmsGeomorphonMaximaFinder.AUTHORS, contact = OmsGeomorphonMaximaFinder.CONTACTS)
@Keywords(OmsGeomorphonMaximaFinder.KEYWORDS)
@Label(OmsGeomorphonMaximaFinder.LABEL)
@Name("_" + OmsGeomorphonMaximaFinder.NAME)
@Status(OmsGeomorphonMaximaFinder.STATUS)
@License(OmsGeomorphonMaximaFinder.LICENSE)
@Bibliography(OmsGeomorphon.BIBLIOGRAPHY_JASIEWICZ_STEPINSKI_2013)
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
    public static final String DESCRIPTION = "Finds the tops of the trees as the peaks of the geomorphons of a surface model.";
    public static final String outMaxima_DESC = "The tops, with an id and their height over the ground (elev).";
    public static final String pElevDiff_DESC = "The largest difference of the heights over the ground in the 3 x 3 cells around a top: steeper tops are discarded.";
    public static final String pThreshold_DESC = "The flatness angle of the geomorphons: the terrain within it from the line of sight is level.";
    public static final String pRadius_DESC = "The search radius of the geomorphons: about the size of the crowns to recognize.";
    public static final String inDSM_DESC = "The surface model, as the highest points of each cell.";
    public static final String inDTM_DESC = "The elevation model of the ground, on the grid of the surface model.";
    public static final String DOCUMENTATION = """
            The surface model is classified into geomorphons, as by Geomorphon, with the given radius and flatness angle, and the tops are searched among its peaks:

            1. the cells of a peak that touch each other are one peak, and its top is the cell with the largest height over the ground;
            2. a peak of a single cell is discarded if it has more than two neighbours without height, or both a pit and a valley among its 8 neighbouring geomorphons;
            3. the top is discarded if a cell of the 3 x 3 around it has no height, or if their heights span more than the elevation difference threshold.

            The heights over the ground are the difference of the surface and the elevation model, which must have the same grid. The cells on the border of the raster are never tops. The threshold of the elevation difference is to tune with the resolution: in cells of 0.5 m the heights of a crown easily differ by more than a meter around its top, so that a threshold of 1 m keeps only the flattest tops. There is no threshold on the height: the peaks of the ground and of the shrubs are tops too.
            """;
    public static final String pThreshold_UNIT = "degree";
    public static final String pRadius_UNIT = "m";
    public static final String pElevDiffThres_UNIT = "m";

    private int peakCode;

    private int valleyCode;

    private int pitCode;

    @Execute
    public void process() throws Exception {
        checkNull(inDTM, inDSM);

        GridGeometry2D gridGeometry = inDSM.getGridGeometry();

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
        rasterDiff.pm = new DummyProgressMonitor();
        rasterDiff.process();
        GridCoverage2D dsmDtmThresDiff = rasterDiff.outRaster;

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDSM);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        TreeSet<GridNode> topNodes = new TreeSet<GridNode>(new GridNodePositionComparator());
        peakCode = GeomorphonClassification.PEAK.getCode();
        valleyCode = GeomorphonClassification.VALLEY.getCode();
        pitCode = GeomorphonClassification.PIT.getCode();

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

    /**
     * A single peak cell is a maximum unless it has both a pit and a valley around.
     */
    private boolean analyzeNeighbors( GridNode topNode ) {
        double[][] window = topNode.getWindow(3, false);
        int countvalley = 0;
        int countpit = 0;
        for( int winRow = 0; winRow < window.length; winRow++ ) {
            for( int winCol = 0; winCol < window[0].length; winCol++ ) {
                double value = window[winRow][winCol];
                if (HMConstants.isNovalue(value)) {
                    continue;
                }
                if (value == valleyCode) {
                    countvalley++;
                } else if (value == pitCode) {
                    countpit++;
                }
            }
        }
        return !(countpit > 0 && countvalley > 0);
    }

    private SimpleFeatureBuilder getOutBuilder() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("geomorphon");
        b.setCRS(inDSM.getCoordinateReferenceSystem());
        b.add("the_geom", Point.class);
        b.add("id", Integer.class);
        b.add("elev", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        return builder;
    }

}
