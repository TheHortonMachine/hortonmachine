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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.jgrasstools.email.EmailingProgressMonitor;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.lidartools.zprocessings.ana.NetIndexComparator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

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

        DefaultFeatureCollection newLinesCollection = new DefaultFeatureCollection();
        newLinesBuilder = getNewLinesBuilder(netFC.getBounds().getCoordinateReferenceSystem());

        FeatureExtender ext = new FeatureExtender(netFC.getSchema(), new String[]{WIDTH2, AVGSLOPE}, new Class[]{Double.class,
                Double.class});
        ArrayList<Geometry> finalPolygonGeoms = new ArrayList<Geometry>();
        DefaultFeatureCollection outFeatures = new DefaultFeatureCollection();
        Set<Entry<String, TreeSet<SimpleFeature>>> entrySet = pfaff2PointsListSet.entrySet();
        pm.beginTask("Processing...", entrySet.size());
        for( Entry<String, TreeSet<SimpleFeature>> entry : entrySet ) {
            // String tmpPfaf = entry.getKey();
            // if (
            // // !pfaf.equals("12.3")&&
            // // !pfaf.equals("12.4.1")&&
            // // !pfaf.equals("12.2.1")&&
            // // !pfaf.equals("12.5")&&
            // !tmpPfaf.equals("6.5")) {
            // continue;
            // }
            TreeSet<SimpleFeature> pointsSet = entry.getValue();
            SimpleFeature[] netPointsFeatures = pointsSet.toArray(new SimpleFeature[0]);

            ArrayList<SimpleFeature> newLinesFeatures = new ArrayList<SimpleFeature>();
            for( int i = 0; i < netPointsFeatures.length; i++ ) {
                SimpleFeature netPointFeature = netPointsFeatures[i];

                LineString line = getLineGeometry(netPointFeature);
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
                    newLinesBuilder.addAll(new Object[]{line, pfaf, linkID});
                    SimpleFeature feature = newLinesBuilder.buildFeature(null);
                    newLinesFeatures.add(feature);
                } else {
                    newWidth = addPoints(k, n, slope, newLinesFeatures, netPointFeature, line);
                }

                SimpleFeature extendedFeature = ext.extendFeature(netPointFeature, new Object[]{newWidth, slope});
                outFeatures.add(extendedFeature);
            }

            ArrayList<Geometry> triangles = getPolygonBetweenLines(newLinesFeatures);

            Geometry union = CascadedPolygonUnion.union(triangles);
            finalPolygonGeoms.add(union);
            newLinesCollection.addAll(newLinesFeatures);
            pm.worked(1);
        }
        pm.done();

        SimpleFeatureCollection outFC = FeatureUtilities.featureCollectionFromGeometry(netFC.getBounds()
                .getCoordinateReferenceSystem(), finalPolygonGeoms.toArray(new Geometry[0]));
        dumpVector(outFC, outBufferShp);
        dumpVector(newLinesCollection, outWidthLinesShp);
        dumpVector(outFeatures, outNetPointsShp);

        // ((EmailingProgressMonitor) pm).addFileToSend(outBufferShp);
        // ((EmailingProgressMonitor) pm).addFileToSend(outWidthLinesShp);
        // ((EmailingProgressMonitor) pm).addFileToSend(outNetPointsShp);
        //
        // ((EmailingProgressMonitor) pm).onModuleExit();

    }

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

    public static void main( String[] args ) throws Exception {

        String inSupFormShp = "/media/BLUEBERRY/unibz/trasporto_solido/basedata/FSmd.shp";
        String inNetPointsShp = "/media/BLUEBERRY/unibz/trasporto_solido/RESULTS/2014_02_12_rienza_shape/rienza_netpoints_width_briglieponti_slope_new.shp";
        String inWidthLinesShp = "/media/BLUEBERRY/unibz/trasporto_solido/RESULTS/2014_02_12_rienza_shape/rienza_netpoints_widthlines.shp";
        String outBufferShp = "/media/BLUEBERRY/unibz/trasporto_solido/RESULTS/2014_02_17_rienza_shape/rienza_netpoints_width_briglieponti_slope10_floodpolygon"
                + ".shp";
        String outNetPointsShp = "/media/BLUEBERRY/unibz/trasporto_solido/RESULTS/2014_02_17_rienza_shape/rienza_netpoints_width_briglieponti_slope10_floodwidth"
                + ".shp";
        String outWidthLinesShp = "/media/BLUEBERRY/unibz/trasporto_solido/RESULTS/2014_02_17_rienza_shape/rienza_netpoints_widthlines_slope10_new"
                + ".shp";

        LW07_NetworkBufferWidthCalculator networkBufferWidthCalculator = new LW07_NetworkBufferWidthCalculator();
        networkBufferWidthCalculator.inGeo = OmsVectorReader.readVector(inSupFormShp);
        networkBufferWidthCalculator.inNetPoints = OmsVectorReader.readVector(inNetPointsShp);
        networkBufferWidthCalculator.inSectWidth = OmsVectorReader.readVector(inWidthLinesShp);
        new LW07_NetworkBufferWidthCalculator().process();

    }

}
