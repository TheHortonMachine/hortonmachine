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
package org.hortonmachine.lesto.modules.vegetation.rastermaxima;

import static java.lang.Math.ceil;
import static java.lang.Math.pow;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.Variables.CONIFER;
import static org.hortonmachine.gears.libs.modules.Variables.CUSTOM;
import static org.hortonmachine.gears.libs.modules.Variables.DECIDUOUS;
import static org.hortonmachine.gears.libs.modules.Variables.MIXED_PINES_AND_DECIDUOUS;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

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
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.i18n.GearsMessages;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectorize.OmsVectorizer;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.distance.DistanceOp;

@Description(OmsRasterMaximaFinder.OMSMAXIMAFINDER_DESCRIPTION)
@Author(name = GearsMessages.OMSHYDRO_AUTHORNAMES, contact = GearsMessages.OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsRasterMaximaFinder.OMSMAXIMAFINDER_KEYWORDS)
@Label(OmsRasterMaximaFinder.OMSMAXIMAFINDER_LABEL)
@Name("_" + OmsRasterMaximaFinder.OMSMAXIMAFINDER_NAME)
@Status(OmsRasterMaximaFinder.OMSMAXIMAFINDER_STATUS)
@License(GearsMessages.OMSHYDRO_LICENSE)
public class OmsRasterMaximaFinder extends HMModel {

    @Description(inGeodata_DESCRIPTION)
    @In
    public GridCoverage2D inDsmDtmDiff;

    @Description(pMode_DESCRIPTION)
    @UI("combo:" + CUSTOM + "," + MIXED_PINES_AND_DECIDUOUS + "," + DECIDUOUS + "," + CONIFER)
    @In
    public String pMode = CUSTOM;

    @Description(pThreshold_DESCRIPTION)
    @In
    public double pThreshold = 1.0;

    @Description(pSize_DESCRIPTION)
    @In
    public int pSize = 3;

    @Description(pPercent_DESCRIPTION)
    @In
    public int pPercent = 60;

    @Description(pMaxRadius_DESCRIPTION)
    @In
    public double pMaxRadius = 3.0;

    @Description(doCircular_DESCRIPTION)
    @In
    public boolean doCircular = true;

    @Description(doAllowNovalues_DESCRIPTION)
    @In
    public boolean doAllowNovalues = false;

    @Description(pBorderDistanceThres_DESCRIPTION)
    @Unit("m")
    @In
    public double pBorderDistanceThres = -1.0;

    @Description(pTopBufferThres_DESCRIPTION)
    @Unit("m")
    @In
    public double pTopBufferThres = 5.0;

    @Description(pTopBufferThresCellCount_DESCRIPTION)
    @In
    public int pTopBufferThresCellCount = 2;

    @Description(outMaxima_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMaxima;

    @Description(outCircles_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outCircles;

    public SimpleFeatureCollection outBorders;

    // PARAMETERS DOCS START
    public static final String OMSMAXIMAFINDER_DESCRIPTION = "Module to find local maxima.";
    public static final String OMSMAXIMAFINDER_KEYWORDS = "Raster, Maxima";
    public static final String OMSMAXIMAFINDER_LABEL = HMConstants.LESTO + "/vegetation";
    public static final String OMSMAXIMAFINDER_NAME = "rastermaximafinder";
    public static final int OMSMAXIMAFINDER_STATUS = 5;
    public static final String inGeodata_DESCRIPTION = "The input CHM.";
    public static final String pThreshold_DESCRIPTION = "Threshold on maxima. Only maxima higher than the threshold are kept.";
    public static final String pMode_DESCRIPTION = "Processing mode.";
    public static final String pPercent_DESCRIPTION = "Percentage to apply to the maxima window to downsize it (default is 60%). This is ignored in CUSTOM mode.";
    public static final String pMaxRadius_DESCRIPTION = "Maximum radius to use in meters. This is ignored in CUSTOM mode.";
    public static final String doCircular_DESCRIPTION = "Use circular window.";
    public static final String doAllowNovalues_DESCRIPTION = "Allow novalues inside the window of the maxima.";
    public static final String pBorderDistanceThres_DESCRIPTION = "Distance threshold to mark maxima as near a border. If <0 check is ignored.";
    public static final String pSize_DESCRIPTION = "The windows size in cells to use for custom mode(default is 3).";
    public static final String outMaxima_DESCRIPTION = "The maxima vector.";
    public static final String outCircles_DESCRIPTION = "The maxima related areas vector.";
    public static final String pTopBufferThresCellCount_DESCRIPTION = "Top buffer threshold cell count";
    public static final String pTopBufferThres_DESCRIPTION = "Top buffer threshold";
    // PARAMETERS DOCS END

    private DecimalFormat formatter = new DecimalFormat("0.0");
    public static final String NOTE = "note";

    @Execute
    public void process() throws Exception {
        checkNull(inDsmDtmDiff, pMode);

        int mode = 0;
        if (pMode.equals(CUSTOM)) {
            mode = 0;
        } else if (pMode.equals(MIXED_PINES_AND_DECIDUOUS)) {
            mode = 1;
        } else if (pMode.equals(DECIDUOUS)) {
            mode = 2;
        } else if (pMode.equals(CONIFER)) {
            mode = 3;
        } else {
            throw new ModelsIllegalargumentException("Processing mode not recognized: " + pMode, this);
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDsmDtmDiff);

        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        GridGeometry2D gridGeometry = inDsmDtmDiff.getGridGeometry();
        GeometryFactory gf = GeometryUtilities.gf();

        SimpleFeatureTypeBuilder maximaTypeBuilder = new SimpleFeatureTypeBuilder();
        maximaTypeBuilder.setName("pointtype");
        maximaTypeBuilder.setCRS(inDsmDtmDiff.getCoordinateReferenceSystem());
        maximaTypeBuilder.add("the_geom", Point.class);
        maximaTypeBuilder.add("id", Integer.class);
        maximaTypeBuilder.add("elev", Double.class);
        maximaTypeBuilder.add(NOTE, String.class);
        SimpleFeatureType maximaType = maximaTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder maximaBuilder = new SimpleFeatureBuilder(maximaType);
        outMaxima = new DefaultFeatureCollection();

        SimpleFeatureTypeBuilder circleTypeBuilder = new SimpleFeatureTypeBuilder();
        circleTypeBuilder.setName("pointtype");
        circleTypeBuilder.setCRS(inDsmDtmDiff.getCoordinateReferenceSystem());
        circleTypeBuilder.add("the_geom", Polygon.class);
        circleTypeBuilder.add("id_maxima", Integer.class);
        circleTypeBuilder.add("area", Double.class);
        SimpleFeatureType circleType = circleTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder circleBuilder = new SimpleFeatureBuilder(circleType);
        outCircles = new DefaultFeatureCollection();

        RandomIter elevIter = CoverageUtilities.getRandomIterator(inDsmDtmDiff);
        double novalue = HMConstants.getNovalue(inDsmDtmDiff);

        int id = 1;
        pm.beginTask("Finding maxima...", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {

                GridNode node = new GridNode(elevIter, cols, rows, xRes, yRes, c, r, novalue);
                if (!node.isValid()) {
                    continue;
                }
                double elevation = node.elevation;

                int size = 3;
                switch( mode ) {
                case 1: {
                    /*
                     * Popescu & Kini 2004 for mixed pines and deciduous trees
                     */
                    double windowWidth = 2.51503 + 0.00901 * pow(elevation, 2.0);
                    windowWidth = windowWidth * pPercent / 100.0;
                    if (windowWidth > 2 * pMaxRadius) {
                        windowWidth = 2 * pMaxRadius;
                    }
                    size = (int) ceil(windowWidth / xRes);
                    break;
                }
                case 2: {
                    double windowWidth = 3.09632 + 0.00895 * pow(elevation, 2.0);
                    windowWidth = windowWidth * pPercent / 100.0;
                    if (windowWidth > 2 * pMaxRadius) {
                        windowWidth = 2 * pMaxRadius;
                    }
                    size = (int) ceil(windowWidth / xRes);
                    break;
                }
                case 3: {
                    double windowWidth = 3.75105 + 0.17919 * elevation + 0.01241 * pow(elevation, 2.0);
                    windowWidth = windowWidth * pPercent / 100.0;
                    if (windowWidth > 2 * pMaxRadius) {
                        windowWidth = 2 * pMaxRadius;
                    }
                    size = (int) ceil(windowWidth / xRes);
                    break;
                }
                default:
                    size = pSize;
                    break;
                }
                if (size > cols / 2) {
                    throw new ModelsIllegalargumentException(
                            "The windows width is larger than half the processing region for elevation = " + elevation, this);
                }

                boolean tmpDoCircular = doCircular;
                if (size <= 3) {
                    size = 3;
                    tmpDoCircular = false;
                }
                double[][] window = node.getWindow(size, tmpDoCircular);
                // int center = (int) ((window.length - 1.0) / 2.0);

                String note = "";
                boolean isMax = true;
                for( int mrow = 0; mrow < window.length; mrow++ ) {
                    for( int mcol = 0; mcol < window[0].length; mcol++ ) {
                        if (!isNovalue(window[mrow][mcol])) {
                            // valid value
                            if (window[mrow][mcol] > elevation) {
                                isMax = false;
                                break;
                            }
                        }
                    }
                    if (!isMax)
                        break;
                }
                if (isMax) {
                    int nonValidCells = 0;

                    List<GridNode> surroundingNodes = node.getSurroundingNodes();
                    int topBufferThresViolationCount = 0;
                    for( GridNode gridNode : surroundingNodes ) {
                        if (gridNode != null) {
                            double elev = gridNode.elevation;
                            double elevDiff = elevation - elev;
                            if (elevDiff > pTopBufferThres) {
                                topBufferThresViolationCount++;
                            }
                        } else {
                            nonValidCells++;
                        }
                        if (nonValidCells > 1 && !doAllowNovalues) {
                            note = "exclude: found invalid neighbor cells = " + nonValidCells;
                            isMax = false;
                            break;
                        }

                    }

                    if (isMax && pTopBufferThresCellCount > 0 && topBufferThresViolationCount >= pTopBufferThresCellCount) {
                        isMax = false;
                        note = "exclude: elevation diff of neighbors from top violates thres (" + pTopBufferThres + "/"
                                + topBufferThresViolationCount + ")";
                    }
                    // create circle

                    if (isMax) {
                        if (elevation >= pThreshold) {
                            Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, gridGeometry);
                            Point point = gf.createPoint(coordinate);

                            String elevStr = formatter.format(elevation);
                            elevStr = elevStr.replace(',', '.');
                            elevation = Double.parseDouble(elevStr);
                            Object[] values = new Object[]{point, id, elevation, note};
                            maximaBuilder.addAll(values);
                            SimpleFeature feature = maximaBuilder.buildFeature(null);
                            ((DefaultFeatureCollection) outMaxima).add(feature);
                            double radius = (size * xRes) / 2.0;
                            Geometry buffer = point.buffer(radius);
                            values = new Object[]{buffer, id, buffer.getArea()};
                            circleBuilder.addAll(values);
                            feature = circleBuilder.buildFeature(null);
                            ((DefaultFeatureCollection) outCircles).add(feature);

                            id++;
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        elevIter.done();

        if (pBorderDistanceThres > 0) {
            OmsVectorizer vectorizer = new OmsVectorizer();
            vectorizer.pm = pm;
            vectorizer.inRaster = inDsmDtmDiff;
            vectorizer.pValue = null;
            vectorizer.pThres = 0;
            vectorizer.doMask = true;
            vectorizer.pMaskThreshold = pThreshold;
            vectorizer.fDefault = "rast";
            vectorizer.process();
            SimpleFeatureCollection diffPolygons = vectorizer.outVector;
            List<Geometry> diffGeoms = FeatureUtilities.featureCollectionToGeometriesList(diffPolygons, true, null);
            List<LineString> bordersGeoms = new ArrayList<LineString>();
            for( Geometry geometry : diffGeoms ) {
                if (geometry instanceof Polygon) {
                    Polygon polygon = (Polygon) geometry;
                    LineString exteriorRing = polygon.getExteriorRing();
                    bordersGeoms.add(exteriorRing);
                    int numInteriorRing = polygon.getNumInteriorRing();
                    for( int i = 0; i < numInteriorRing; i++ ) {
                        LineString interiorRingN = polygon.getInteriorRingN(i);
                        bordersGeoms.add(interiorRingN);
                    }
                }
            }
            MultiLineString allBorders = gf.createMultiLineString(bordersGeoms.toArray(GeometryUtilities.TYPE_LINESTRING));

            outBorders = FeatureUtilities.featureCollectionFromGeometry(inDsmDtmDiff.getCoordinateReferenceSystem(), allBorders);

            SimpleFeatureIterator maximaIter = outMaxima.features();
            while( maximaIter.hasNext() ) {
                SimpleFeature maxima = maximaIter.next();
                Geometry maximaGeometry = (Geometry) maxima.getDefaultGeometry();
                double distance = DistanceOp.distance(allBorders, maximaGeometry);
                if (distance < pBorderDistanceThres) {
                    maxima.setAttribute(NOTE, "exclude: near border: " + distance);
                }
            }
            maximaIter.close();

        }

    }
}
