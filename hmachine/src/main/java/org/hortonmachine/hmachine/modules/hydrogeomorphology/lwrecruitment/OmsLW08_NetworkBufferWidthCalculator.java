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

import static java.lang.Math.pow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

@Description(OmsLW08_NetworkBufferWidthCalculator.DESCRIPTION)
@Author(name = OmsLW08_NetworkBufferWidthCalculator.AUTHORS, contact = OmsLW08_NetworkBufferWidthCalculator.CONTACTS)
@Keywords(OmsLW08_NetworkBufferWidthCalculator.KEYWORDS)
@Label(OmsLW08_NetworkBufferWidthCalculator.LABEL)
@Name("_" + OmsLW08_NetworkBufferWidthCalculator.NAME)
@Status(OmsLW08_NetworkBufferWidthCalculator.STATUS)
@License(OmsLW08_NetworkBufferWidthCalculator.LICENSE)
public class OmsLW08_NetworkBufferWidthCalculator extends HMModel implements LWFields {
    @Description(inNetPoints_DESCR)
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description(inGeo_DESCR)
    @In
    public SimpleFeatureCollection inGeo = null;

    @Description(inTransSect_DESCR)
    @In
    public SimpleFeatureCollection inTransSect = null;

    @Description(pPrePostCount4Slope_DESCR)
    @In
    public int pPrePostCount4Slope = 10;

    @Description(pK_DESCR)
    @In
    public double pK = 20.0;

    @Description(pN_DESCR)
    @In
    public double pN = -0.2;

    @Description(doKeepBridgeDamWidth_DESCR)
    @In
    public boolean doKeepBridgeDamWidth = true;

    @Description(pMinSlope_DESCR)
    @In
    public double pMinSlope = 0.001;

    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description(outInundationArea_DESCR)
    @Out
    public SimpleFeatureCollection outInundationArea = null;

    @Description(outInundationSections_DESCR)
    @Out
    public SimpleFeatureCollection outInundationSections = null;

    // VARS DOC START
    public static final String outInundationSections_DESCR = "The output layer with the sections lines where the inundation width has been calculated.";
    public static final String outInundationArea_DESCR = "The output polygon layer with the inundation areas.";
    public static final String outNetPoints_DESCR = "The output points network layer with the additional attribute of inundated width and average slope.";
    public static final String pMinSlope_DESCR = "The value to use for the places where the slope is zero in the input raster map.";
    public static final String doKeepBridgeDamWidth_DESCR = "The boolean to select if considering the width of dams and bridges or not.";
    public static final String pN_DESCR = "Formula exponent of the power law for the evaluation of the new width: newWidth = width + k * slope^n or Wr = k * omega^n";
    public static final String pK_DESCR = "Formula constant of the power law for the evaluation of the new width: newWidth = width + k * slope^n or Wr = k * omega^n";
    public static final String pPrePostCount4Slope_DESCR = "The number of cells upstream and downstream to consider to evaluate the average slope in each section.";
    public static final String inTransSect_DESCR = "The input line shapefile with the extracted transversal sections.";
    public static final String inGeo_DESCR = "The input polygon layer with the geological superficial geological formations.";
    public static final String inNetPoints_DESCR = "The input hierarchy point network layer with the information of local slope.";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw08_networkbufferwidthcalculator";
    public static final String LABEL = HMConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "network, vector, bankflull, width, inundation, power law";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Calculate the inundation zones along the channel network following a power law for the new width based on the original widht and the channel slope.";

    // VARS DOC END

    private static final double WATER_SPECIFIC_WEIGHT = 9810.0; // N/m3
    private HashMap<String, Geometry> pfafId2WidthLine;
    private SimpleFeatureBuilder newLinesBuilder;
    private PreparedGeometry preparedSupFormGeom;

    @Execute
    public void process() throws Exception {

        // store the geometries of the superficial geology in a list
        List<Geometry> inSupFormGeomsList = FeatureUtilities.featureCollectionToGeometriesList(inGeo, false, null);

        preparedSupFormGeom = PreparedGeometryFactory.prepare(inSupFormGeomsList.get(0));

        /*
         * read the width lines and index them by id
         */
        List<SimpleFeature> widthLinesList = FeatureUtilities.featureCollectionToList(inTransSect);
        pfafId2WidthLine = new HashMap<String, Geometry>();
        for( SimpleFeature widthLineFeature : widthLinesList ) {
            String pfaf = widthLineFeature.getAttribute(PFAF).toString();
            String linkID = widthLineFeature.getAttribute(LINKID).toString();
            String id = pfaf + "_" + linkID;
            pfafId2WidthLine.put(id, (Geometry) widthLineFeature.getDefaultGeometry());
        }

        /*
         * read the network points and store it in a list
         */
        List<SimpleFeature> netPointsList = FeatureUtilities.featureCollectionToList(inNetPoints);
        SimpleFeature firstNetPoint = netPointsList.get(0);
        boolean hasHydraulic = false;
        if (firstNetPoint.getAttribute(FIELD_DISCHARGE) instanceof Double) {
            hasHydraulic = true;
        }

        NetIndexComparator indexComparator = new NetIndexComparator();
        HashMap<String, TreeSet<SimpleFeature>> pfaff2PointsListSet = new HashMap<String, TreeSet<SimpleFeature>>();

        /*
         * create a treeSet as a navigable set using the comparator methods
         * for each network point get the hierarchy using the attribute Pfafstetter and then 
         * get the position of the point inside the link using the index (from comparator)
         */
        for( SimpleFeature netFeature : netPointsList ) {
            String pfaf = netFeature.getAttribute(PFAF).toString();

            TreeSet<SimpleFeature> treeSet = pfaff2PointsListSet.get(pfaf);
            if (treeSet == null) {
                treeSet = new TreeSet<SimpleFeature>(indexComparator);
                pfaff2PointsListSet.put(pfaf, treeSet);
            }
            treeSet.add(netFeature);
        }

        // prepare to store inundated sections (lines)
        outInundationSections = new DefaultFeatureCollection();
        outInundationArea = new DefaultFeatureCollection();
        newLinesBuilder = getNewLinesBuilder(inNetPoints.getBounds().getCoordinateReferenceSystem());

        // prepare the schema for the output network points
        FeatureExtender ext = new FeatureExtender(inNetPoints.getSchema(), new String[]{WIDTH2, AVGSLOPE},
                new Class[]{Double.class, Double.class});
        // prepare to store the polygons of inundated areas
        ArrayList<Geometry> finalPolygonGeoms = new ArrayList<Geometry>();
        outNetPoints = new DefaultFeatureCollection();
        Set<Entry<String, TreeSet<SimpleFeature>>> entrySet = pfaff2PointsListSet.entrySet();
        pm.beginTask("Processing...", entrySet.size());
        for( Entry<String, TreeSet<SimpleFeature>> entry : entrySet ) {
            TreeSet<SimpleFeature> pointsSet = entry.getValue();
            SimpleFeature[] netPointsFeatures = pointsSet.toArray(new SimpleFeature[0]);

            // create the sections in each point
            ArrayList<SimpleFeature> newLinesFeatures = new ArrayList<SimpleFeature>();
            for( int i = 0; i < netPointsFeatures.length; i++ ) {
                SimpleFeature netPointFeature = netPointsFeatures[i];

                // consider the original bankfull line sections
                LineString lineBankfullSection = getLineGeometry(netPointFeature);
                double slope = getSlope(pPrePostCount4Slope, netPointsFeatures, i);

                int from = ((Double) netPointFeature.getAttribute(WIDTH_FROM)).intValue();
                double newWidth = 0;
                if (doKeepBridgeDamWidth && from != LWFields.WIDTH_FROM_CHANNELEDIT) {
                    // in this case the new lines are the same as the old
                    /*
                     * the width is taken from the attributes table, since it
                     * is changed for bridges and dams only after the creation 
                     * of the lines
                     */
                    newWidth = (Double) netPointFeature.getAttribute(WIDTH);
                    Object pfaf = netPointFeature.getAttribute(PFAF);
                    Object linkID = netPointFeature.getAttribute(LINKID);
                    newLinesBuilder.addAll(new Object[]{lineBankfullSection, pfaf, linkID});
                    SimpleFeature feature = newLinesBuilder.buildFeature(null);
                    newLinesFeatures.add(feature);
                } else {
                    // calculate the inundated section
                    newWidth = addPoints(pK, pN, hasHydraulic, slope, newLinesFeatures, netPointFeature, lineBankfullSection);
                }

                // add the attributes to the point network
                SimpleFeature extendedFeature = ext.extendFeature(netPointFeature, new Object[]{newWidth, slope});
                ((DefaultFeatureCollection) outNetPoints).add(extendedFeature);
            }
            // create the output polygon of inundated area
            ArrayList<Geometry> triangles = getPolygonBetweenLines(newLinesFeatures);

            Geometry union = CascadedPolygonUnion.union(triangles);
            finalPolygonGeoms.add(union);

            // add the inundated section to the output collection
            ((DefaultFeatureCollection) outInundationSections).addAll(newLinesFeatures);
            pm.worked(1);
        }
        SimpleFeatureCollection outInundatedAreaFC = FeatureUtilities.featureCollectionFromGeometry(
                inNetPoints.getBounds().getCoordinateReferenceSystem(), finalPolygonGeoms.toArray(new Geometry[0]));

        // add the inundated polygons to the output collection
        ((DefaultFeatureCollection) outInundationArea).addAll(outInundatedAreaFC);

        pm.done();

    }

    /*
     * create the polygons between the inundated sections for the inundated
     * areas
     */
    private ArrayList<Geometry> getPolygonBetweenLines( ArrayList<SimpleFeature> newLinesFeatures ) {
        ArrayList<Geometry> polygons = new ArrayList<Geometry>();
        for( int i = 0; i < newLinesFeatures.size() - 1; i++ ) {
            SimpleFeature f1 = newLinesFeatures.get(i);
            SimpleFeature f2 = newLinesFeatures.get(i + 1);

            LineString l1 = (LineString) f1.getDefaultGeometry();
            LineString l2 = (LineString) f2.getDefaultGeometry();
            MultiLineString multiLine = gf.createMultiLineString(new LineString[]{l1, l2});

            Geometry convexHull = multiLine.convexHull();
            Geometry buffer = convexHull.buffer(0.1);
            polygons.add(buffer);
        }
        return polygons;
    }

    /*
     * evaluate the inundation width using a power law based on slope and input params
     */
    private double calculateWidth( double k, double n, double slope, double width ) {
        if (slope == 0) {
            slope = pMinSlope;
        }
        double newWidth = width + k * pow(slope, n);
        if (Double.isInfinite(newWidth) || Double.isNaN(newWidth)) {
            throw new RuntimeException();
        }
        return newWidth;
    }

    /*
     * create a collection of lines (sections) for each network point
     */
    private SimpleFeatureBuilder getNewLinesBuilder( CoordinateReferenceSystem crs ) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("net");
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        b.add(PFAF, String.class);
        b.add(LINKID, Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        return builder;
    }

    private LineString getLineGeometry( SimpleFeature f ) {
        String pfaf = f.getAttribute(PFAF).toString();
        String linkID = f.getAttribute(LINKID).toString();
        String id = pfaf + "_" + linkID;
        Geometry geometry = pfafId2WidthLine.get(id);
        Geometry geometryN = geometry.getGeometryN(0);
        return (LineString) geometryN;
    }

    /*
     * evaluate the average slope in each point considering the given input number of  
     * cells upstream and downstream
     */
    private double getSlope( int prePostCount4Slope, SimpleFeature[] features, int i ) {
        int start = i - prePostCount4Slope;
        int end = i + prePostCount4Slope + 1;
        if (start < 0) {
            start = 0;
        }
        if (end > features.length) {
            end = features.length;
        }

        double slopeAvg = 0;
        int count = 0;

        for( int j = start; j < end; j++ ) {
            double slope = (Double) features[j].getAttribute(SLOPE);
            slopeAvg += slope;
            count++;
        }
        slopeAvg = slopeAvg / count;

        if (Double.isNaN(slopeAvg)) {
            System.out.println();
        }

        return slopeAvg;
    }

    private double addPoints( double k, double n, boolean hasHydraulic, double slope, ArrayList<SimpleFeature> newLinesFeatures,
            SimpleFeature netPointFeature, LineString lineBankfullSection ) {

        double width = (Double) netPointFeature.getAttribute(WIDTH);
        Object pfaf = netPointFeature.getAttribute(PFAF);
        Object linkID = netPointFeature.getAttribute(LINKID);
        double discharge = (double) netPointFeature.getAttribute(FIELD_DISCHARGE);

        // check on superficial deposits for bankfull centroid
        Point centerPoint = lineBankfullSection.getCentroid();
        Coordinate center = centerPoint.getCoordinate();

        // if the center point is inside the geology polygons there is the possibility to erode
        // and new channel width can be calculated
        double factor;
        if (preparedSupFormGeom.intersects(centerPoint)) {
            if (hasHydraulic) {
                double omega = WATER_SPECIFIC_WEIGHT * discharge * slope / width;
                factor = k * pow(omega, n);
            } else {
                double newWidth = calculateWidth(k, n, slope, width);
                factor = newWidth / width;
            }
        } else {
            // outside geology polygons there is rock, the channel width can not change during
            // a flooding event
            factor = 1.0;
        }

        Point startPoint = lineBankfullSection.getStartPoint();
        Point endPoint = lineBankfullSection.getEndPoint();

        Coordinate origC1 = startPoint.getCoordinate();
        Coordinate origC2 = endPoint.getCoordinate();
        LineSegment seg1 = new LineSegment(center, origC1);
        LineSegment seg2 = new LineSegment(center, origC2);

        // create the two new vertexes of the inundated section considering the same line
        Coordinate c1 = seg1.pointAlong(factor);
        Coordinate c2 = seg2.pointAlong(factor);

        // create the new segments from the center to the two new vertexes
        LineString newSeg1 = gf.createLineString(new Coordinate[]{center, c1});
        LineString newSeg2 = gf.createLineString(new Coordinate[]{center, c2});

        /*
         * TODO
         * check of the intersection of the new vertexes of the inundated section
         * and the superficial geology
         */
        newSeg1 = checkSupFormIntersection(centerPoint, origC1, newSeg1);
        newSeg2 = checkSupFormIntersection(centerPoint, origC2, newSeg2);

        // consider the final coordinates of the vertexes of the inundated section and create
        // the new line
        Coordinate newC1 = newSeg1.getEndPoint().getCoordinate();
        Coordinate newC2 = newSeg2.getEndPoint().getCoordinate();
        LineString newInundatedSeg = gf.createLineString(new Coordinate[]{newC1, newC2});

        double newWidth = newInundatedSeg.getLength();

        newLinesBuilder.addAll(new Object[]{newInundatedSeg, pfaf, linkID});
        SimpleFeature feature = newLinesBuilder.buildFeature(null);
        newLinesFeatures.add(feature);
        return newWidth;
    }

    /*
     * check the intersection of the extracted inundated sections with the geology of
     * superficial forms to avoid to have erosion where there is rock on the surface
     */
    private LineString checkSupFormIntersection( Point centerPoint, Coordinate origC, LineString newOneSideSegment ) {
        if (preparedSupFormGeom.intersects(newOneSideSegment) && !preparedSupFormGeom.covers(newOneSideSegment)) {
            Geometry intersection1 = preparedSupFormGeom.getGeometry().intersection(newOneSideSegment);
            if (intersection1 instanceof LineString) {
                newOneSideSegment = (LineString) intersection1;
            } else if (intersection1 instanceof MultiLineString) {
                newOneSideSegment = null;
                MultiLineString multiLineIntersected = (MultiLineString) intersection1;
                double minDistance = Double.POSITIVE_INFINITY;
                for( int i = 0; i < multiLineIntersected.getNumGeometries(); i++ ) {
                    LineString tmpLine = (LineString) multiLineIntersected.getGeometryN(i);
                    double distance = DistanceOp.distance(tmpLine, centerPoint);
                    if (distance < minDistance) {
                        minDistance = distance;
                        newOneSideSegment = tmpLine;
                    }
                }
                if (newOneSideSegment == null) {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException("Geometry found: " + intersection1);
            }
        }

        Point checkPoint = newOneSideSegment.getStartPoint();
        if (centerPoint.equals(checkPoint)) {
            // need to check since we are not sure about the order after intersection
            checkPoint = newOneSideSegment.getEndPoint();
        }
        if (centerPoint.distance(checkPoint) < centerPoint.getCoordinate().distance(origC)) {
            // the new line segment is smaller, that is not allowed
            newOneSideSegment = gf.createLineString(new Coordinate[]{centerPoint.getCoordinate(), origC});
        }

        return newOneSideSegment;
    }
    
    public static void main( String[] args ) throws Exception {

        String base = "D:/lavori_tmp/unibz/2016_06_gsoc/data01/";

        OmsLW08_NetworkBufferWidthCalculator ex = new OmsLW08_NetworkBufferWidthCalculator();
        ex.inNetPoints = OmsVectorReader.readVector(base + "net_point_width_damsbridg_slope_lateral.shp");
        ex.inTransSect = OmsVectorReader.readVector(base + "extracted_bankfullsections_lateral2.shp");
        ex.inGeo = OmsVectorReader.readVector(base + "geology.shp");
        ex.pK = 0.07;
        ex.pN = 0.44;

        ex.process();
        SimpleFeatureCollection outNetPoints = ex.outNetPoints;
        SimpleFeatureCollection outInundArea = ex.outInundationArea;
        SimpleFeatureCollection outInundSect = ex.outInundationSections;
        
        OmsVectorWriter.writeVector(base + "net_point_width_damsbridg_slope_lateral_inund.shp", outNetPoints);
        OmsVectorWriter.writeVector(base + "inund_area2.shp", outInundArea);
        OmsVectorWriter.writeVector(base + "inund_sections2.shp", outInundSect);

    }

}
