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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureExtender;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.ARiverSectionsExtractor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.OmsRiverSectionsExtractor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;

@Description(OmsLW04_BankfullWidthAnalyzer.DESCRIPTION)
@Author(name = OmsLW04_BankfullWidthAnalyzer.AUTHORS, contact = OmsLW04_BankfullWidthAnalyzer.CONTACTS)
@Keywords(OmsLW04_BankfullWidthAnalyzer.KEYWORDS)
@Label(OmsLW04_BankfullWidthAnalyzer.LABEL)
@Name("_" + OmsLW04_BankfullWidthAnalyzer.NAME)
@Status(OmsLW04_BankfullWidthAnalyzer.STATUS)
@License(OmsLW04_BankfullWidthAnalyzer.LICENSE)
public class OmsLW04_BankfullWidthAnalyzer extends HMModel implements LWFields {

    @Description(inBankfull_DESCR)
    @In
    public SimpleFeatureCollection inBankfull = null;

    @Description(inNetPoints_DESCR)
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description(pMaxDistanceFromNetpoint_DESCR)
    @Unit("m")
    @In
    public double pMaxDistanceFromNetpoint = 100.0;

    @Description(pMaxNetworkWidth_DESCR)
    @Unit("m")
    @In
    public double pMaxNetworkWidth = 100;

    @Description(pMinNetworkWidth_DESCR)
    @Unit("m")
    @In
    public double pMinNetworkWidth = 0.5;

    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description(outProblemPoints_DESCR)
    @Out
    public SimpleFeatureCollection outProblemPoints = null;

    @Description(outBankfullSections_DESCR)
    @Out
    public SimpleFeatureCollection outBankfullSections = null;

    // VARS DOC START
    public static final String outBankfullSections_DESCR = "The output layer with the sections lines where the bankfull width has been calculated.";
    public static final String outProblemPoints_DESCR = "The output points layer highlighting the position of the problematic sections.";
    public static final String outNetPoints_DESCR = "The output points network layer with the additional attribute of bankfull width.";
    public static final String pMinNetworkWidth_DESCR = "The minimum width for the channel network";
    public static final String pMaxNetworkWidth_DESCR = "The maximum width for the channel network";
    public static final String pMaxDistanceFromNetpoint_DESCR = "The maximum distance that a point can have from the nearest polygon. If distance is major, then the netpoint is ignored and identified as outside the region of interest.";
    public static final String inNetPoints_DESCR = "The input hierarchy point network layer.";
    public static final String inBankfull_DESCR = "The input polygon layer of the bankfull area.";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw04_bankfullwidthanalyzer";
    public static final String LABEL = HMConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "network, vector, point, bankflull, width";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Extracts the bankfull width for each section of the channels and adds it as an attribute to the input layer.";
    // VARS DOC END

    private int NEW_NETWORK_ATTRIBUTES_NUM = 2;

    // error messages
    private String NEAREST_CHANNEL_POLYGON_TOO_FAR_FROM_POINT = "nearest channeledit polygon is too far from point";
    private String NO_CHANNELEDIT_POLYGON_FOUND = "no channeledit polygon found";
    private String FOUND_INVALID_NETWORK_WIDTH_LARGE = "invalid network width (too large)";
    private String FOUND_INVALID_NETWORK_WIDTH_SMALL = "invalid network width (too small)";
    private String NO_PROPER_INTERSECTION_WITH_CHANNELEDIT = "no proper intersection with channeledit";

    @Execute
    public void process() throws Exception {

        // Creates the output points hashmap
        LinkedHashMap<SimpleFeature, double[]> allNetPointsMap = new LinkedHashMap<SimpleFeature, double[]>();

        CoordinateReferenceSystem crs = inNetPoints.getBounds().getCoordinateReferenceSystem();

        // Insert the points in the final hashmap
        List<SimpleFeature> netFeatures = FeatureUtilities.featureCollectionToList(inNetPoints);
        netFeatures.sort(new Comparator<SimpleFeature>(){
            public int compare( SimpleFeature o1, SimpleFeature o2 ) {
                int i1 = ((Number) o1.getAttribute(LWFields.LINKID)).intValue();
                int i2 = ((Number) o2.getAttribute(LWFields.LINKID)).intValue();
                if (i1 < i2) {
                    return -1;
                } else if (i1 > i2) {
                    return 1;
                }
                return 0;
            }
        });
        for( SimpleFeature netFeature : netFeatures ) {
            allNetPointsMap.put(netFeature, new double[NEW_NETWORK_ATTRIBUTES_NUM]);
        }

        // Generates supporting variables
        LinkedHashMap<SimpleFeature, String> problemPointsMap = new LinkedHashMap<SimpleFeature, String>();
        LinkedHashMap<SimpleFeature, double[]> validPointsMap = new LinkedHashMap<SimpleFeature, double[]>();
        ConcurrentLinkedQueue<Object[]> validPointsLineList = new ConcurrentLinkedQueue<Object[]>();
        handleChannelEdited(inBankfull, allNetPointsMap, validPointsMap, problemPointsMap, validPointsLineList);

        outNetPoints = getNetworkPoints(crs, validPointsMap);
        outBankfullSections = getWidthLines(crs, validPointsLineList);
        outProblemPoints = getProblemPoints(crs, problemPointsMap);

    }

    private void handleChannelEdited( SimpleFeatureCollection channeleditedFC,
            LinkedHashMap<SimpleFeature, double[]> netPointsMap, LinkedHashMap<SimpleFeature, double[]> validPointsMap,
            LinkedHashMap<SimpleFeature, String> problemPointsMap, ConcurrentLinkedQueue<Object[]> validPointsLineList ) {

        // index the bankfull geometries
        List<Geometry> channelGeometriesList = FeatureUtilities.featureCollectionToGeometriesList(channeleditedFC, true, null);
        STRtree channelIndex = new STRtree();
        for( Geometry channelGeometry : channelGeometriesList ) {
            PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(channelGeometry);
            channelIndex.insert(channelGeometry.getEnvelopeInternal(), preparedGeometry);
        }

        Set<Entry<SimpleFeature, double[]>> entrySet = netPointsMap.entrySet();
        pm.beginTask("Calculating channel edited width...", entrySet.size());
        /*
         * Main cycle over all the channel points to extract the bakfull width
         */
        Point previousNetPoint = null;
        double progressive = 0;
        for( Entry<SimpleFeature, double[]> entry : entrySet ) {
            SimpleFeature netFeature = entry.getKey();
            Point netPoint = (Point) netFeature.getDefaultGeometry();
            if (previousNetPoint != null) {
                progressive += netPoint.distance(previousNetPoint);
            }
            previousNetPoint = netPoint;

            Object linkId = netFeature.getAttribute(LINKID);
            Object pfaf = netFeature.getAttribute(PFAF);

            double[] attributes = entry.getValue();
            // expand the envelop of the point to verify if it is inside the area of interest
            Envelope env = new Envelope(netPoint.getCoordinate());
            env.expandBy(pMaxDistanceFromNetpoint);
            // consider the bankfull polygons
            List<PreparedPolygon> channelsList = channelIndex.query(env);
            /*
             * pick the right polygon considering the intersection between a point and a 
             * banfull polygon
             */
            PreparedPolygon channel = null;
            for( PreparedPolygon tmpChannel : channelsList ) {
                if (tmpChannel.intersects(netPoint)) {
                    channel = tmpChannel;
                    break;
                }
            }
            boolean netPointIsOutsideChanneledit = false;
            Coordinate nearestOnChannelCoordinate = null;
            if (channel == null) {
                // if net point doesn't lie inside a channel polygon
                netPointIsOutsideChanneledit = true;
                double minDistance = Double.POSITIVE_INFINITY;
                for( PreparedPolygon tmpChannel : channelsList ) {
                    Coordinate[] nearestPoints = DistanceOp.nearestPoints(tmpChannel.getGeometry(), netPoint);
                    Coordinate onChannelCoordinate = nearestPoints[0];
                    Coordinate onNetCoordinate = nearestPoints[1];
                    double distance = onChannelCoordinate.distance(onNetCoordinate);
                    /*
                     * calculates the distance between the point and the nearest coordinate
                     * of the nearest bankfull polygon
                     */
                    if (distance < minDistance) {
                        minDistance = distance;
                        channel = tmpChannel;
                        nearestOnChannelCoordinate = onChannelCoordinate;
                    }
                }
            } else {
                // if net point lies inside a channel polygon
                netPointIsOutsideChanneledit = false;
                Geometry geometry = channel.getGeometry();
                /*
                 * calculates the distance between the point and the nearest coordinate
                 * of the intersecting banfull polygon
                 */
                Coordinate[] nearestPoints = DistanceOp.nearestPoints(((Polygon) geometry).getExteriorRing(), netPoint);
                nearestOnChannelCoordinate = nearestPoints[0];
            }

            if (channel == null) {
                // the point does not have a bakfull section and it is labeled a problem point
                problemPointsMap.put(netFeature, NO_CHANNELEDIT_POLYGON_FOUND);
            } else {
                Coordinate netCoordinate = netPoint.getCoordinate();
                /*
                 * verify the distance between the point and the nearest coordinate of bankfull
                 * polygon, if the distance is grater than max_dist the point is labeled as
                 * problem point
                 */
                if (nearestOnChannelCoordinate.distance(netCoordinate) > pMaxDistanceFromNetpoint) {
                    problemPointsMap.put(netFeature, NEAREST_CHANNEL_POLYGON_TOO_FAR_FROM_POINT);
                    pm.worked(1);
                    continue;
                }

                /*
                 * create the section segment with one vertex intersecting the bankfull polygon
                 */
                int SEGMENTLENGTH = 200;
                int runningSegmentlength = SEGMENTLENGTH;
                LineSegment segment;
                if (netPointIsOutsideChanneledit) {
                    segment = new LineSegment(netCoordinate, nearestOnChannelCoordinate);
                } else {
                    segment = new LineSegment(nearestOnChannelCoordinate, netCoordinate);
                }
                LineString channelLines = ((Polygon) channel.getGeometry()).getExteriorRing();

                Coordinate[] coordinates = new Coordinate[0];
                double length = 0;
                int lengthLimit = 100;
                /*
                 * extends the length of the segment so that it intersect at least once the
                 * bankfull polygon
                 */
                while( coordinates.length < 1 && length < lengthLimit ) {
                    Coordinate otherPoint = segment.pointAlong(runningSegmentlength);
                    LineString lineString = gf.createLineString(new Coordinate[]{nearestOnChannelCoordinate, otherPoint});
                    Geometry intersection = channelLines.intersection(lineString);
                    coordinates = intersection.getCoordinates();
                    if (coordinates.length == 1 && coordinates[0].distance(nearestOnChannelCoordinate) < 0.001) {
                        /*
                         * make sure that if there is only one intersection is not the point
                         * itself due to floating point approximation
                         */
                        length = 0;
                        coordinates = new Coordinate[0];
                    } else {
                        length = lineString.getLength();
                    }
                    runningSegmentlength = runningSegmentlength + SEGMENTLENGTH;

                    // TODO set limit
                }

                /*
                 * if no other intersection is found label the network point as problem point
                 */
                if (coordinates.length < 1) {
                    problemPointsMap.put(netFeature, NO_PROPER_INTERSECTION_WITH_CHANNELEDIT);
                } else {
                    /*
                     * if only one other intersection is found it is for sure not the same
                     * point nearestOnChannelCoordinate (checked before)
                     */
                    if (coordinates.length == 1) {
                        Coordinate coordinate = coordinates[0];
                        assign(validPointsMap, problemPointsMap, validPointsLineList, netFeature, linkId, pfaf, progressive,
                                attributes, nearestOnChannelCoordinate, coordinate);
                    } else {
                        double minDistance = Double.POSITIVE_INFINITY;
                        Coordinate nearest = null;
                        for( Coordinate coordinate : coordinates ) {
                            if (coordinate.distance(nearestOnChannelCoordinate) < 0.001) {
                                // it is the starting border coord, ignore it
                                continue;
                            }
                            double distance = coordinate.distance(netCoordinate);
                            if (distance < minDistance) {
                                minDistance = distance;
                                nearest = coordinate;
                            }
                        }
                        assign(validPointsMap, problemPointsMap, validPointsLineList, netFeature, linkId, pfaf, progressive,
                                attributes, nearestOnChannelCoordinate, nearest);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * check the width of the section against the given thresholds
     * and create the section linestring
     */
    private void assign( LinkedHashMap<SimpleFeature, double[]> validPointsMap,
            LinkedHashMap<SimpleFeature, String> problemPointsMap, ConcurrentLinkedQueue<Object[]> validPointsLineList,
            SimpleFeature netFeature, Object linkId, Object pfaf, double progressive, double[] attributes,
            Coordinate nearestOnChannelCoordinate, Coordinate coordinate ) {
        double width = coordinate.distance(nearestOnChannelCoordinate);
        if (width > pMaxNetworkWidth) {
            problemPointsMap.put(netFeature, FOUND_INVALID_NETWORK_WIDTH_LARGE);
        } else if (width < pMinNetworkWidth) {
            problemPointsMap.put(netFeature, FOUND_INVALID_NETWORK_WIDTH_SMALL);
        } else {
            attributes[0] = width;
            attributes[1] = WIDTH_FROM_CHANNELEDIT;

            LineString widthLine = gf.createLineString(new Coordinate[]{coordinate, nearestOnChannelCoordinate});
            Object[] newLineAttr = {widthLine, pfaf, linkId, linkId, progressive};
            validPointsLineList.add(newLineAttr);
            validPointsMap.put(netFeature, attributes);
        }
    }

    private DefaultFeatureCollection getNetworkPoints( CoordinateReferenceSystem crs,
            LinkedHashMap<SimpleFeature, double[]> validPointsMap ) throws Exception {

        FeatureExtender ext = null;
        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        Set<Entry<SimpleFeature, double[]>> entrySet = validPointsMap.entrySet();
        for( Entry<SimpleFeature, double[]> entry : entrySet ) {
            SimpleFeature pointFeature = entry.getKey();

            if (ext == null) {
                ext = new FeatureExtender(pointFeature.getFeatureType(), new String[]{WIDTH, WIDTH_FROM},
                        new Class[]{Double.class, Double.class});
            }

            double[] attributes = entry.getValue();
            Object[] attrObj = new Object[attributes.length];
            for( int i = 0; i < attrObj.length; i++ ) {
                attrObj[i] = attributes[i];
            }
            SimpleFeature extendedFeature = ext.extendFeature(pointFeature, attrObj);
            newCollection.add(extendedFeature);
        }

        return newCollection;
    }

    private DefaultFeatureCollection getProblemPoints( CoordinateReferenceSystem crs,
            LinkedHashMap<SimpleFeature, String> problemPointsMap ) throws Exception {

        FeatureExtender ext = null;

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        Set<Entry<SimpleFeature, String>> entrySet = problemPointsMap.entrySet();
        for( Entry<SimpleFeature, String> entry : entrySet ) {
            SimpleFeature pointFeature = entry.getKey();
            String notes = entry.getValue();

            if (ext == null) {
                ext = new FeatureExtender(pointFeature.getFeatureType(), new String[]{NOTES}, new Class[]{String.class});
            }

            SimpleFeature extendedFeature = ext.extendFeature(pointFeature, new Object[]{notes});
            newCollection.add(extendedFeature);
        }

        return newCollection;
    }

    private DefaultFeatureCollection getWidthLines( CoordinateReferenceSystem crs,
            ConcurrentLinkedQueue<Object[]> validPointsLineList ) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("net");
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        b.add(PFAF, String.class);
        b.add(LINKID, Integer.class);
        b.add(ARiverSectionsExtractor.FIELD_SECTION_ID, Integer.class);
        b.add(ARiverSectionsExtractor.FIELD_PROGRESSIVE, Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();

        for( Object[] objects : validPointsLineList ) {
            builder.addAll(objects);
            SimpleFeature feature = builder.buildFeature(null);
            newCollection.add(feature);
        }

        return newCollection;
    }

    public static void main( String[] args ) throws Exception {

        String base = "D:/lavori_tmp/unibz/2016_06_gsoc/data01/";

        OmsLW04_BankfullWidthAnalyzer ex = new OmsLW04_BankfullWidthAnalyzer();
        ex.inBankfull = OmsVectorReader.readVector(base + "channelpolygon_merged.shp");
        ex.inNetPoints = OmsVectorReader.readVector(base + "net_point.shp");
        // ex.inSections = OmsVectorReader.readVector(base + "shape/sections_adige_75.shp");
        // ex.pMaxDistanceFromNetpoint = 20;
        // ex.pMaxNetworkWidth = 75;
        // ex.pMinNetworkWidth =
        ex.process();
        SimpleFeatureCollection outNetPoints = ex.outNetPoints;
        SimpleFeatureCollection outBankfullSections = ex.outBankfullSections;
        SimpleFeatureCollection outProblemPoints = ex.outProblemPoints;

        OmsVectorWriter.writeVector(base + "net_point_width.shp", outNetPoints);
        OmsVectorWriter.writeVector(base + "bankfullsections.shp", outBankfullSections);
        OmsVectorWriter.writeVector(base + "problempoints.shp", outProblemPoints);

    }
}
