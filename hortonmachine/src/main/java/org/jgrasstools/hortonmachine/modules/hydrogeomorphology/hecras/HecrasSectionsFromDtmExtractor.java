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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.coverage.ProfilePoint;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

/**
 * An extractor of geometries from dtm for Hecras.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HecrasSectionsFromDtmExtractor implements HecrasSectionsExtractor {
    private double wideSectionsDistance = -1.0D;

    private final List<FeatureMate> bridgePointsList;

    private GeometryFactory gf = GeometryUtilities.gf();
    private List<NetworkPoint> networkPointList;
    private int pointsWithSectionsNum;
    private CoordinateReferenceSystem crs;
    private SimpleFeatureCollection sectionsCollection;
    private SimpleFeatureCollection sectionPointsCollection;

    /**
     * Constructor.
     * 
     * @param riverLine the river line to consider for the cross sections extraction.
     * @param elevation the elevation {@link GridCoverage2D}.
     * @param sectionsIntervalDistance the distance to use between extracted sections. 
     * @param sectionsWidth the width of the extracted sections.
     * @param bridgesList the list of bridge {@link Point}s. 
     * @param bridgeWidthAttribute the name of the attribute in the bridges feature that defines the width of the bridge.
     * @param bridgeBuffer a buffer to use for the bridge inside which the bridge is considered to be on the river.
     * @param damsList the list of dams to be used to define the bank positions.
     * @param monitor the progress monitor.
     * @throws Exception
     */
    public HecrasSectionsFromDtmExtractor( //
            LineString riverLine, //
            GridCoverage2D elevation, //
            double sectionsIntervalDistance, //
            double sectionsWidth, //
            List<FeatureMate> bridgesList, //
            String bridgeWidthAttribute, //
            double bridgeBuffer, //
            IJGTProgressMonitor monitor //
    ) throws Exception {
        this.wideSectionsDistance = sectionsIntervalDistance;
        this.bridgePointsList = bridgesList;

        crs = elevation.getCoordinateReferenceSystem();
        RenderedImage elevRI = elevation.getRenderedImage();
        RandomIter elevIter = RandomIterFactory.create(elevRI, null);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(elevation);
        GridGeometry2D gridGeometry = elevation.getGridGeometry();

        monitor.beginTask("Extracting sections...", -1);

        networkPointList = new ArrayList<NetworkPoint>();
        LengthIndexedLine indexedLine = new LengthIndexedLine(riverLine);
        double length = riverLine.getLength();
        double runningLength = 0;
        while( runningLength <= length ) {
            // important to extract from left to right
            Coordinate leftPoint = indexedLine.extractPoint(runningLength, sectionsWidth);
            Coordinate rightPoint = indexedLine.extractPoint(runningLength, -sectionsWidth);

            NetworkPoint netPoint = getNetworkPoint(riverLine, elevIter, regionMap, gridGeometry, runningLength, leftPoint,
                    rightPoint);
            if (netPoint != null)
                networkPointList.add(netPoint);

            runningLength = runningLength + wideSectionsDistance;
        }

        /*
         * handle bridges
         */
        if (bridgePointsList != null) {
            for( int j = 0; j < bridgePointsList.size(); ++j ) {
                FeatureMate bridgeFeature = bridgePointsList.get(j);
                Geometry bridgeGeometry = bridgeFeature.getGeometry();
                Geometry bufferedBridgeGeometry = bridgeGeometry.buffer(bridgeBuffer);

                if (riverLine.intersects(bufferedBridgeGeometry)) {

                    double bridgeIndex = indexedLine.project(new Coordinate(bridgeGeometry.getCoordinate()));
                    double bridgeWidth = bridgeFeature.getAttribute(bridgeWidthAttribute, Double.class).doubleValue();
                    double pre = bridgeIndex - bridgeWidth / 2.0 - 1d;
                    double post = bridgeIndex + bridgeWidth / 2.0 + 1d;

                    // important to extract from left to right
                    Coordinate leftBridgePoint = indexedLine.extractPoint(bridgeIndex, sectionsWidth);
                    Coordinate rightBridgePoint = indexedLine.extractPoint(bridgeIndex, -sectionsWidth);
                    Coordinate leftPreBridgePoint = indexedLine.extractPoint(pre, sectionsWidth);
                    Coordinate rightPreBridgePoint = indexedLine.extractPoint(pre, -sectionsWidth);
                    Coordinate leftPostBridgePoint = indexedLine.extractPoint(post, sectionsWidth);
                    Coordinate rightPostBridgePoint = indexedLine.extractPoint(post, -sectionsWidth);

                    // bridge extraction
                    NetworkPoint netPoint = getNetworkPoint(riverLine, elevIter, regionMap, gridGeometry, bridgeIndex,
                            leftBridgePoint, rightBridgePoint);
                    if (netPoint != null)
                        networkPointList.add(netPoint);

                    // pre bridge extraction
                    netPoint = getNetworkPoint(riverLine, elevIter, regionMap, gridGeometry, pre, leftPreBridgePoint,
                            rightPreBridgePoint);
                    if (netPoint != null)
                        networkPointList.add(netPoint);

                    // post bridge extraction
                    netPoint = getNetworkPoint(riverLine, elevIter, regionMap, gridGeometry, post, leftPostBridgePoint,
                            rightPostBridgePoint);
                    if (netPoint != null)
                        networkPointList.add(netPoint);
                }
            }
        }

        // add also the river coordinates that do not have sections
        Coordinate[] coordinates = riverLine.getCoordinates();
        List<ProfilePoint> riverPprofile = CoverageUtilities.doProfile(elevIter, regionMap, gridGeometry, coordinates);
        for( ProfilePoint profilePoint : riverPprofile ) {
            Coordinate position = profilePoint.getPosition();
            position.z = profilePoint.getElevation();
            NetworkPoint netPoint = new NetworkPoint(position, profilePoint.getProgressive(), null);
            networkPointList.add(netPoint);
        }

        pointsWithSectionsNum = 0;
        for( NetworkPoint netPoint : networkPointList ) {
            if (netPoint.hasSection) {
                pointsWithSectionsNum++;
            }
        }
        Collections.sort(networkPointList);

        monitor.done();

    }

    private void createSectionsCollections() {
        SimpleFeatureTypeBuilder sectionTypeBuilder = new SimpleFeatureTypeBuilder();
        sectionTypeBuilder.setName("sectionlines");
        sectionTypeBuilder.setCRS(crs);
        sectionTypeBuilder.add("the_geom", LineString.class);
        sectionTypeBuilder.add("id", Integer.class);
        sectionTypeBuilder.add("progressive", Double.class);
        SimpleFeatureType sectionType = sectionTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder sectionBuilder = new SimpleFeatureBuilder(sectionType);

        SimpleFeatureTypeBuilder sectionPointsTypeBuilder = new SimpleFeatureTypeBuilder();
        sectionPointsTypeBuilder.setName("sectionpoint");
        sectionPointsTypeBuilder.setCRS(crs);
        sectionPointsTypeBuilder.add("the_geom", Point.class);
        sectionPointsTypeBuilder.add("count", Integer.class);
        sectionPointsTypeBuilder.add("elev", Double.class);
        sectionPointsTypeBuilder.add("progressive", Double.class);
        sectionPointsTypeBuilder.add("sectionid", Double.class);
        SimpleFeatureType sectionPointsType = sectionPointsTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder sectionPointsBuilder = new SimpleFeatureBuilder(sectionPointsType);

        sectionsCollection = FeatureCollections.newCollection();
        sectionPointsCollection = FeatureCollections.newCollection();

        int index = 0;
        for( NetworkPoint netPoint : networkPointList ) {
            if (!netPoint.hasSection) {
                continue;
            }
            LineString sectionGeometry = netPoint.sectionGeometry;

            int sectionId = netPoint.getSectionId();
            if (sectionId == -1) {
                sectionId = index;
            }

            Coordinate[] sectionCoordinates = sectionGeometry.getCoordinates();
            LineString simpleSectionGeometry = gf.createLineString(new Coordinate[]{sectionCoordinates[0],
                    sectionCoordinates[sectionCoordinates.length - 1]});

            Object[] sectionValues = new Object[]{simpleSectionGeometry, sectionId, netPoint.progressiveDistance};
            sectionBuilder.addAll(sectionValues);
            SimpleFeature sectionFeature = sectionBuilder.buildFeature(null);
            sectionsCollection.add(sectionFeature);

            Coordinate[] coordinates = sectionGeometry.getCoordinates();
            List<Double> sectionProgressive = netPoint.sectionProgressive;
            for( int i = 0; i < coordinates.length; i++ ) {
                Point point = gf.createPoint(coordinates[i]);

                Object[] sectionPointsValues = new Object[]{point, i, coordinates[i].z, sectionProgressive.get(i), sectionId};
                sectionPointsBuilder.addAll(sectionPointsValues);
                SimpleFeature sectionPointsFeature = sectionPointsBuilder.buildFeature(null);
                sectionPointsCollection.add(sectionPointsFeature);
            }
            index++;
        }
    }

    /**
     * Extract a {@link NetworkPoint}.
     * 
     * @param riverLine
     * @param elevIter
     * @param regionMap
     * @param gridGeometry
     * @param index
     * @param leftPoint
     * @param rightPoint
     * @return
     * @throws TransformException
     */
    private NetworkPoint getNetworkPoint( LineString riverLine, RandomIter elevIter, RegionMap regionMap,
            GridGeometry2D gridGeometry, double index, Coordinate leftPoint, Coordinate rightPoint ) throws TransformException {
        List<ProfilePoint> profilePoints = CoverageUtilities.doProfile(elevIter, regionMap, gridGeometry, rightPoint, leftPoint);
        List<Coordinate> coordinate3dList = new ArrayList<Coordinate>();
        for( ProfilePoint profilePoint : profilePoints ) {
            Coordinate position = profilePoint.getPosition();
            position.z = profilePoint.getElevation();
            coordinate3dList.add(position);
        }
        LineString bridgeLine3d = gf.createLineString(coordinate3dList.toArray(new Coordinate[0]));
        Geometry crossPoint = bridgeLine3d.intersection(riverLine);
        Coordinate coordinate = crossPoint.getCoordinate();
        if (coordinate == null) {
            return null;
        }
        int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, null);
        double elev = elevIter.getSampleDouble(colRow[0], colRow[1], 0);
        coordinate.z = elev;
        NetworkPoint netPoint = new NetworkPoint(coordinate, index, bridgeLine3d);
        return netPoint;
    }

    @Override
    public List<NetworkPoint> getOrderedNetworkPoints() {
        return networkPointList;
    }

    @Override
    public int getSectionsNum() {
        return pointsWithSectionsNum;
    }

    // private void addBankPositionsToSections( NetworkPoint networkPoint ) {
    // if (!networkPoint.hasSection)
    // return;
    //
    // LineString sectionLine = networkPoint.sectionGeometry;
    // Coordinate[] sectionCoordinates = networkPoint.sectionGeometry.getCoordinates();
    // List<Double> sectionPointsProgressive = networkPoint.sectionProgressive;
    // ArrayList<Double> bankStationPosition = new ArrayList<Double>();
    // if (damsList != null) {
    // Coordinate oneCoordinate = null;
    // Coordinate secondCoordinate = null;
    // Geometry tmpGeom;
    // for( int i = 0; i < damsList.size(); ++i ) {
    // tmpGeom = damsList.get(i).getGeometry();
    // if (sectionLine.intersects(tmpGeom)) {
    // if (oneCoordinate == null) {
    // System.out.println("1 geom n." + i);
    // Geometry tmp = sectionLine.intersection(tmpGeom);
    // Coordinate[] tmpCoords = tmp.getCoordinates();
    // if (tmpCoords.length > 1) {
    // oneCoordinate = tmpCoords[0];
    // secondCoordinate = tmpCoords[1];
    // break;
    // }
    // oneCoordinate = tmpCoords[0];
    // } else {
    // System.out.println("2 geom n." + i);
    // secondCoordinate = sectionLine.intersection(tmpGeom).getCoordinate();
    // break;
    // }
    // }
    // }
    //
    // double lastProgressive = sectionPointsProgressive.get(sectionPointsProgressive.size() - 1);
    //
    // if ((oneCoordinate != null) && (secondCoordinate != null)) {
    // double distance1 = Double.MAX_VALUE;
    // double distance2 = Double.MAX_VALUE;
    // double bank1 = 0.0D;
    // double elevationbank1 = 0.0D;
    // double bank2 = 0.0D;
    // double elevationbank2 = 0.0D;
    //
    // for( int i = 0; i < sectionCoordinates.length; ++i ) {
    // Coordinate runningCoordinate = sectionCoordinates[i];
    //
    // double tmpdistance1 = runningCoordinate.distance(oneCoordinate);
    // double tmpdistance2 = runningCoordinate.distance(secondCoordinate);
    // if (tmpdistance1 < distance1) {
    // distance1 = tmpdistance1;
    // bank1 = sectionPointsProgressive.get(i);
    // elevationbank1 = sectionCoordinates[i].z;
    // }
    // if (tmpdistance2 < distance2) {
    // distance2 = tmpdistance2;
    // bank2 = sectionPointsProgressive.get(i);
    // elevationbank2 = sectionCoordinates[i].z;
    // }
    // }
    //
    // if (bank1 < bank2) {
    // bankStationPosition.add(Double.valueOf(bank1 / lastProgressive));
    // bankStationPosition.add(Double.valueOf(bank2 / lastProgressive));
    // bankStationPosition.add(Double.valueOf(elevationbank1));
    // bankStationPosition.add(Double.valueOf(elevationbank2));
    // } else {
    // bankStationPosition.add(Double.valueOf(bank2 / lastProgressive));
    // bankStationPosition.add(Double.valueOf(bank1 / lastProgressive));
    // bankStationPosition.add(Double.valueOf(elevationbank2));
    // bankStationPosition.add(Double.valueOf(elevationbank1));
    // }
    // } else {
    // bankStationPosition.add(Double.valueOf(0.0D));
    // bankStationPosition.add(Double.valueOf(1.0D));
    // bankStationPosition.add(Double.valueOf(sectionPointsProgressive.get(0)));
    // bankStationPosition.add(Double.valueOf(sectionPointsProgressive.get(sectionPointsProgressive.size()
    // - 1)));
    // }
    // } else {
    // bankStationPosition.add(Double.valueOf(0.0D));
    // bankStationPosition.add(Double.valueOf(1.0D));
    // bankStationPosition.add(Double.valueOf(sectionPointsProgressive.get(0)));
    // bankStationPosition.add(Double.valueOf(sectionPointsProgressive.get(sectionPointsProgressive.size()
    // - 1)));
    // }
    //
    // networkPoint.bankPositions = bankStationPosition;
    // }

    @Override
    public SimpleFeatureCollection getSectionsCollection() {
        if (sectionsCollection == null)
            createSectionsCollections();
        return sectionsCollection;
    }

    @Override
    public SimpleFeatureCollection getSectionPointsCollection() {
        if (sectionPointsCollection == null)
            createSectionsCollections();
        return sectionPointsCollection;
    }

    public static void main( String[] args ) {
        GeometryFactory gf2 = GeometryUtilities.gf();

        LineString createLineString = gf2.createLineString(new Coordinate[]{//
                new Coordinate(0, 0),//
                        new Coordinate(5, 0),//
                        new Coordinate(5, 1),//
                        new Coordinate(6, 1),//
                        new Coordinate(6, 0),//
                        new Coordinate(10, 0)//
                });

        LengthIndexedLine l = new LengthIndexedLine(createLineString);
        Coordinate extractPoint = l.extractPoint(6.5);
        System.out.println(extractPoint);

    }
}
