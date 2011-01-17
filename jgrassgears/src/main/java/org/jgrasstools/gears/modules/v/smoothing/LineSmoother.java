/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.modules.v.smoothing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

@Description("Collection of Smoothing Algorithms. Type 0: McMasters Sliding Averaging "
        + "Algorithm. The new position of each point "
        + "is the average of the pLookahead  points around. Parameter pSlide is used for "
        + "linear interpolation between old and new position.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Smoothing, Vector")
@Status(Status.DRAFT)
@Label(JGTConstants.VECTORPROCESSING)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class LineSmoother extends JGTModel {

    @Description("The features to be smoothed.")
    @In
    public SimpleFeatureCollection linesFeatures;

    @Description("The point features that define intersections.")
    @In
    public SimpleFeatureCollection pointFeatures;

    @Description("The smoothing type: McMasters smoothing average (0 = default).")
    @In
    public int pType = 0;

    @Description("The number of points to consider in every smoothing step (default = 7).")
    @In
    public int pLookahead = 7;

    @Description("Minimum length accepted for a line. If it is shorter than that value, the line is not smoothed (if circle or alone, it is removed).")
    @In
    public int pLimit = 0;

    @Description("Slide parameter.")
    @In
    public double pSlide = 0.9;

    @Description("Densifier interval.")
    @In
    public Double pDensify = null;

    @Description("Simplifier tollerance.")
    @In
    public Double pSimplify = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The smoothed features.")
    @Out
    public SimpleFeatureCollection outFeatures;

    private static final double SAMEPOINTTHRESHOLD = 0.1;
    private GeometryFactory gF = GeometryUtilities.gf();

    private double densify = -1;
    private double simplify = -1;

    private List<SimpleFeature> linesList;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFeatures == null, doReset)) {
            return;
        }

        if (pDensify != null) {
            densify = pDensify;
        }
        if (pSimplify != null) {
            simplify = pSimplify;
        }

        outFeatures = FeatureCollections.newCollection();

        int id = 0;
        pm.message("Collecting geometries...");
        linesList = FeatureUtilities.featureCollectionToList(linesFeatures);
        int size = linesFeatures.size();
        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(linesFeatures.getSchema());
        pm.beginTask("Smoothing features...", size);
        for( SimpleFeature line : linesList ) {
            Geometry geometry = (Geometry) line.getDefaultGeometry();
            List<LineString> lsList = smoothGeometries(geometry);
            if (lsList.size() != 0) {
                LineString[] lsArray = (LineString[]) lsList.toArray(new LineString[lsList.size()]);
                MultiLineString multiLineString = gF.createMultiLineString(lsArray);
                SimpleFeature newFeature = fGS.substituteGeometry(line, multiLineString, id);
                outFeatures.add(newFeature);
                id++;
            }
            pm.worked(1);
        }
        pm.done();
    }

    private List<LineString> smoothGeometries( Geometry geometry ) {
        List<LineString> lsList = new ArrayList<LineString>();
        int numGeometries = geometry.getNumGeometries();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometryN = geometry.getGeometryN(i);
            double length = geometryN.getLength();
            Coordinate[] smoothedArray = geometryN.getCoordinates();
            Coordinate first = smoothedArray[0];
            Coordinate last = smoothedArray[smoothedArray.length - 1];
            if (length <= pLimit) {
                // if it is circle remove it, else just do not smooth it
                if (first.distance(last) < SAMEPOINTTHRESHOLD) {
                    continue;
                }
                // check if the line is an error lying around somewhere
                if (isAlone(geometryN)) {
                    continue;
                }
            } else {
                if (densify != -1) {
                    geometryN = Densifier.densify(geometryN, pDensify);
                }
                List<Coordinate> smoothedCoords = Collections.emptyList();;
                switch( pType ) {
                case 0:
                default:
                    FeatureSlidingAverage fSA = new FeatureSlidingAverage(geometryN);
                    smoothedCoords = fSA.smooth(pLookahead, false, pSlide);
                }

                if (smoothedCoords != null) {
                    smoothedArray = (Coordinate[]) smoothedCoords.toArray(new Coordinate[smoothedCoords.size()]);
                } else {
                    smoothedArray = geometryN.getCoordinates();
                }
            }

            LineString lineString = gF.createLineString(smoothedArray);

            if (simplify != -1) {
                TopologyPreservingSimplifier tpSimplifier = new TopologyPreservingSimplifier(lineString);
                tpSimplifier.setDistanceTolerance(pSimplify);
                lineString = (LineString) tpSimplifier.getResultGeometry();
            }

            lsList.add(lineString);
        }
        return lsList;
    }

    /**
     * Checks if the given geometry is connected to any other line.
     * 
     * @param geometryN the geometry to test.
     * @return true if the geometry is alone in the space, i.e. not connected at
     *              one of the ends to any other geometry.
     */
    private boolean isAlone( Geometry geometryN ) {
        Coordinate[] coordinates = geometryN.getCoordinates();
        if (coordinates.length > 1) {
            Coordinate first = coordinates[0];
            Coordinate last = coordinates[coordinates.length - 1];
            for( SimpleFeature line : linesList ) {
                Geometry lineGeom = (Geometry) line.getDefaultGeometry();
                int numGeometries = lineGeom.getNumGeometries();
                for( int i = 0; i < numGeometries; i++ ) {
                    Geometry subGeom = lineGeom.getGeometryN(i);
                    Coordinate[] lineCoordinates = subGeom.getCoordinates();
                    if (lineCoordinates.length < 2) {
                        continue;
                    } else {
                        Coordinate tmpFirst = lineCoordinates[0];
                        Coordinate tmpLast = lineCoordinates[lineCoordinates.length - 1];
                        if (tmpFirst.distance(first) < SAMEPOINTTHRESHOLD || tmpFirst.distance(last) < SAMEPOINTTHRESHOLD
                                || tmpLast.distance(first) < SAMEPOINTTHRESHOLD || tmpLast.distance(last) < SAMEPOINTTHRESHOLD) {
                            return false;
                        }
                    }
                }
            }
        }
        // 1 point line or no connection, mark it as alone for removal
        return true;
    }

    /**
     * An utility method to use the module with default values and shapefiles.
     * 
     * <p>
     * This will use the windowed average and a density of 0.2, simplification threshold of 0.1
     * and a lookahead of 13, as well as a length filter of 10.
     * </p>
     * 
     * @param shapePath the input file.
     * @param outPath the output smoothed path.
     * @throws Exception
     */
    public static void defaultSmoothShapefile( String shapePath, String outPath ) throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
        SimpleFeatureCollection initialFC = ShapefileFeatureReader.readShapefile(shapePath);

        LineSmoother smoother = new LineSmoother();
        smoother.pm = pm;
        smoother.pLimit = 10;
        smoother.linesFeatures = initialFC;
        smoother.pLookahead = 13;
        // smoother.pSlide = 1;
        smoother.pDensify = 0.2;
        smoother.pSimplify = 0.01;
        smoother.process();

        SimpleFeatureCollection smoothedFeatures = smoother.outFeatures;

        ShapefileFeatureWriter.writeShapefile(outPath, smoothedFeatures);
    }

}
