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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment;

import static java.lang.Math.pow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import oms3.annotations.Author;
import oms3.annotations.Description;
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
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

@Description("Calculate the inundation zones along the channel network following a power law for the new width based on the original widht and the channel slope.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("network, vector, bankflull, width, inundation, power law")
@Label("HortonMachine/Hydro-Geomorphology/LWRecruitment")
@Name("LW07_NetworkBufferWidthCalculator")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LW07_NetworkBufferWidthCalculator extends JGTModel implements LWFields {

    @Description("The input hierarchy point network layer with the information of local slope.")
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description("The input polygon layer with the geological superficial geological formations.")
    @In
    public SimpleFeatureCollection inGeo = null;

    @Description("The input line shapefile with the extracted transversal sections.")
    @In
    public SimpleFeatureCollection inSectWidth = null;

    @Description("The output points network layer with the additional attribute of average slope.")
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description("The output polygon layer with the inundation areas.")
    @Out
    public SimpleFeatureCollection outInundationArea = null;

    @Description("The output layer with the sections lines where the inundation width has been calculated.")
    @Out
    public SimpleFeatureCollection outInundationSections = null;

    // The number of cells upstream and downstream to consider to evaluate the average slope in each
    // section
    private int prePostCount4Slope = 10;

    /*
     * Parameters of the power law for the evaluation of the new width: newWidth = width + k * slope^n
     */
    private double k = 20.0; // Formula constant
    private double n = -0.2; // formula exponent

    // The boolean to select if considering the width of dams and bridges or not.
    private boolean keepBridgeDamWidth = true;

    // The value to use for the places where the slope is zero in the input raster map.
    private double MIN_SLOPE = 0.001;

    private HashMap<String, Geometry> pfafId2WidthLine;
    private SimpleFeatureBuilder newLinesBuilder;
    private PreparedGeometry preparedSupFormGeom;

    public void process() throws Exception {

        // store the geometries of the superficial geology in a list
        List<Geometry> inSupFormGeomsList = FeatureUtilities.featureCollectionToGeometriesList(inGeo, false, null);

        preparedSupFormGeom = PreparedGeometryFactory.prepare(inSupFormGeomsList.get(0));

        /*
         * read the width lines and index them by id
         */
        List<SimpleFeature> widthLinesList = FeatureUtilities.featureCollectionToList(inSectWidth);
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
        List<SimpleFeature> netList = FeatureUtilities.featureCollectionToList(inNetPoints);

        NetIndexComparator indexComparator = new NetIndexComparator();
        HashMap<String, TreeSet<SimpleFeature>> pfaff2PointsListSet = new HashMap<String, TreeSet<SimpleFeature>>();

        /*
         * create a treeSet as a navigable set using the comparator methods
         * for each network point get the hierarchy using the attribute Pfafstetter and then 
         * get the position of the point inside the link using the index (from comparator)
         */
        for( SimpleFeature netFeature : netList ) {
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
        FeatureExtender ext = new FeatureExtender(inNetPoints.getSchema(), new String[]{WIDTH2, AVGSLOPE}, new Class[]{
                Double.class, Double.class});
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
                double slope = getSlope(prePostCount4Slope, netPointsFeatures, i);

                int from = ((Double) netPointFeature.getAttribute(WIDTH_FROM)).intValue();
                double newWidth = 0;
                if (keepBridgeDamWidth && from != 0) {
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
                    newWidth = addPoints(k, n, slope, newLinesFeatures, netPointFeature, lineBankfullSection);
                }

                // add the attributes to the point network
                SimpleFeature extendedFeature = ext.extendFeature(netPointFeature, new Object[]{newWidth, slope});
                ((DefaultFeatureCollection) outNetPoints).add(extendedFeature);
            }
            //TODO create the output polygon of inundated area
            
            // add the inundated section to the output collection
            ((DefaultFeatureCollection) outInundationSections).addAll(newLinesFeatures);
            pm.worked(1);
        }
        
        pm.done();

    }

    /*
     * evaluate the inundation width using a power law based on slope and input params
     */
    private double calculateWidth( double k, double n, double slope, double width ) {
        if (slope == 0) {
            slope = MIN_SLOPE;
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

    private double addPoints( double k, double n, double slope, ArrayList<SimpleFeature> newLinesFeatures,
            SimpleFeature netPointFeature, LineString lineBankfullSection ) {

        double width = (Double) netPointFeature.getAttribute(WIDTH);
        Object pfaf = netPointFeature.getAttribute(PFAF);
        Object linkID = netPointFeature.getAttribute(LINKID);

        // check on superficial deposits for bankfull centroid
        Point centerPoint = lineBankfullSection.getCentroid();
        Coordinate center = centerPoint.getCoordinate();

        // if the center point is inside the geology polygons there is the possibility to erode
        // and new channel width can be calculated
        double newWidth;
        if (preparedSupFormGeom.intersects(centerPoint)) {
            newWidth = calculateWidth(k, n, slope, width);
        } else {
        // outside geology polygons there is rock, the channel width can not change during
        // a flooding event
            newWidth = width;
        }

        double factor = newWidth / width;

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
         * TODO: add a check of the intersection of the new vertexes of the inundated section
         * and the superficial geology
         */
//        newSeg1 = checkSupFormIntersection(centerPoint, origC1, newSeg1);
//        newSeg2 = checkSupFormIntersection(centerPoint, origC2, newSeg2);

        // consider the final coordinates of the vertexes of the inundated section and create 
        // the new line
        Coordinate newC1 = newSeg1.getEndPoint().getCoordinate();
        Coordinate newC2 = newSeg2.getEndPoint().getCoordinate();
        LineString newInundatedSeg = gf.createLineString(new Coordinate[]{newC1, newC2});

        newWidth = newInundatedSeg.getLength();

        newLinesBuilder.addAll(new Object[]{newInundatedSeg, pfaf, linkID});
        SimpleFeature feature = newLinesBuilder.buildFeature(null);
        newLinesFeatures.add(feature);
        return newWidth;
    }

    public static void main( String[] args ) throws Exception {

        String inSupFormShp = "D:/lavori_tmp/gsoc/basedata/FSmd.shp";
        String inNetPointsShp = "D:/lavori_tmp/gsoc/netpoints_width_bridgesdams_slope.shp";
        String inWidthLinesShp = "D:/lavori_tmp/gsoc/bankfull_sections.shp";
        String outInundatedPolyShp = "D:/lavori_tmp/gsoc/floodpolygon.shp";
        String outNetPointsShp = "D:/lavori_tmp/gsoc/netpoints_width_bridgesdams_slope_floodwidth.shp";
        String outWidthLinesShp = "D:/lavori_tmp/gsoc/inundated_sections.shp";

        LW07_NetworkBufferWidthCalculator networkBufferWidthCalculator = new LW07_NetworkBufferWidthCalculator();
        networkBufferWidthCalculator.inGeo = OmsVectorReader.readVector(inSupFormShp);
        networkBufferWidthCalculator.inNetPoints = OmsVectorReader.readVector(inNetPointsShp);
        networkBufferWidthCalculator.inSectWidth = OmsVectorReader.readVector(inWidthLinesShp);
        

        networkBufferWidthCalculator.process();

        SimpleFeatureCollection outNetPointFC = networkBufferWidthCalculator.outNetPoints;
        SimpleFeatureCollection outInundatedSectionFC = networkBufferWidthCalculator.outInundationSections;
        SimpleFeatureCollection outInundatedAreaFC = networkBufferWidthCalculator.outInundationArea;

        OmsVectorWriter.writeVector(outNetPointsShp, outNetPointFC);
        OmsVectorWriter.writeVector(outWidthLinesShp, outInundatedSectionFC);

        int size = outInundatedAreaFC.size();
        if (size > 0) {
            OmsVectorWriter.writeVector(outInundatedPolyShp, outInundatedAreaFC);
        }
    
    }

}
