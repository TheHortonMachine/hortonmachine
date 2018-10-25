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
package org.hortonmachine.gears.modules.v.intersections;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_CORRECTED_FEATURES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_DO_REVERSE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_ERROR_FEATURES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_F_SORT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_LINES_FEATURES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_POINT_FEATURES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_P_BUFFER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINEINTERSECTIONCORRECTOR_UNTOUCHED_FEATURES_DESCRIPTION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import oms3.annotations.Author;
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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.linemerge.LineSequencer;
import org.locationtech.jts.operation.overlay.snap.GeometrySnapper;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

@Description(OMSLINEINTERSECTIONCORRECTOR_DESCRIPTION)
@Documentation(OMSLINEINTERSECTIONCORRECTOR_DOCUMENTATION)
@Author(name = OMSLINEINTERSECTIONCORRECTOR_AUTHORNAMES, contact = OMSLINEINTERSECTIONCORRECTOR_AUTHORCONTACTS)
@Keywords(OMSLINEINTERSECTIONCORRECTOR_KEYWORDS)
@Label(OMSLINEINTERSECTIONCORRECTOR_LABEL)
@Name(OMSLINEINTERSECTIONCORRECTOR_NAME)
@Status(OMSLINEINTERSECTIONCORRECTOR_STATUS)
@License(OMSLINEINTERSECTIONCORRECTOR_LICENSE)
public class OmsLineIntersectionCorrector extends HMModel {

    @Description(OMSLINEINTERSECTIONCORRECTOR_LINES_FEATURES_DESCRIPTION)
    @In
    public SimpleFeatureCollection linesFeatures;

    @Description(OMSLINEINTERSECTIONCORRECTOR_POINT_FEATURES_DESCRIPTION)
    @In
    public SimpleFeatureCollection pointFeatures;

    @Description(OMSLINEINTERSECTIONCORRECTOR_P_BUFFER_DESCRIPTION)
    @In
    public double pBuffer = 0.05;

    @Description(OMSLINEINTERSECTIONCORRECTOR_F_SORT_DESCRIPTION)
    @In
    public String fSort = null;

    @Description(OMSLINEINTERSECTIONCORRECTOR_DO_REVERSE_DESCRIPTION)
    @In
    public boolean doReverse = true;

    @Description(OMSLINEINTERSECTIONCORRECTOR_UNTOUCHED_FEATURES_DESCRIPTION)
    @Out
    public SimpleFeatureCollection untouchedFeatures;

    @Description(OMSLINEINTERSECTIONCORRECTOR_CORRECTED_FEATURES_DESCRIPTION)
    @Out
    public SimpleFeatureCollection correctedFeatures;

    @Description(OMSLINEINTERSECTIONCORRECTOR_ERROR_FEATURES_DESCRIPTION)
    @Out
    public SimpleFeatureCollection errorFeatures;

    private static final double DELTA5 = 0.00001;
    private static final double DELTA6 = 0.000001;

    private GeometryFactory gF = GeometryUtilities.gf();

    @Execute
    public void process() throws Exception {
        if (!concatOr(correctedFeatures == null, doReset)) {
            return;
        }

        untouchedFeatures = new DefaultFeatureCollection();
        correctedFeatures = new DefaultFeatureCollection();
        errorFeatures = new DefaultFeatureCollection();

        // extract points
        int pSize = pointFeatures.size();
        pm.message("Intersection points to handle: " + pSize);
        List<LineString> pointsEnvelopes = new ArrayList<LineString>(pSize);
        FeatureIterator<SimpleFeature> pointsIterator = pointFeatures.features();
        pm.beginTask("Create point bounds...", pSize);
        while( pointsIterator.hasNext() ) {
            SimpleFeature feature = pointsIterator.next();
            Geometry point = (Geometry) feature.getDefaultGeometry();
            Coordinate[] coordinates = point.getCoordinates();
            for( Coordinate c : coordinates ) {
                double pbuff = 0.05;
                Coordinate ll = new Coordinate(c.x - pbuff, c.y - pbuff);
                Coordinate ul = new Coordinate(c.x - pbuff, c.y + pbuff);
                Coordinate ur = new Coordinate(c.x + pbuff, c.y + pbuff);
                Coordinate lr = new Coordinate(c.x + pbuff, c.y - pbuff);
                Coordinate end = new Coordinate(c.x - pbuff, c.y - pbuff);
                LineString envelopeLine = gF.createLineString(new Coordinate[]{ll, ul, ur, lr, end});
                pointsEnvelopes.add(envelopeLine);
            }
            pm.worked(1);
        }
        pm.done();
        pointsIterator.close();

        FeatureIterator<SimpleFeature> inFeatureIterator = linesFeatures.features();
        int size = linesFeatures.size();

        // Geometry first = null;
        List<FeatureElevationComparer> badFeatures = new ArrayList<FeatureElevationComparer>(pSize);
        pm.beginTask("Extract intersecting lines...", size);
        while( inFeatureIterator.hasNext() ) {
            SimpleFeature feature = inFeatureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(geometry);

            boolean touched = false;
            for( LineString envelope : pointsEnvelopes ) {
                if (preparedGeometry.intersects(envelope)) {
                    badFeatures.add(new FeatureElevationComparer(feature, fSort, pBuffer, 0.0));
                    touched = true;
                    break;
                }
            }
            if (!touched) {
                ((DefaultFeatureCollection) untouchedFeatures).add(feature);
            }
            pm.worked(1);
        }
        pm.done();
        inFeatureIterator.close();

        Collections.sort(badFeatures);
        if (doReverse) {
            Collections.reverse(badFeatures);
        }

        int id = 0;
        size = badFeatures.size();
        pm.message("Found intersecting lines: " + size);
        pm.beginTask("Correcting intersections...", size);
        for( FeatureElevationComparer featureElevationComparer : badFeatures ) {
            if (featureElevationComparer.toRemove()) {
                continue;
            }

            SimpleFeature feature = featureElevationComparer.getFeature();

            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            int numGeometries = geometry.getNumGeometries();

            List<LineString> geomList = new ArrayList<LineString>(numGeometries);
            for( int i = 0; i < numGeometries; i++ ) {
                geomList.add((LineString) geometry.getGeometryN(i));
            }
            LineString[] lsArray = (LineString[]) geomList.toArray(new LineString[numGeometries]);

            try {
                boolean splitCoordinates = correctLineIntersections(featureElevationComparer, badFeatures, lsArray, id);
                if (splitCoordinates) {
                    geomList.clear();
                    for( LineString lineString : lsArray ) {
                        Coordinate[] coordinates = lineString.getCoordinates();
                        int n = coordinates.length;
                        if (coordinates[0].distance(coordinates[n - 1]) < DELTA5) {
                            // we have a ring, rotate it by a quarter
                            int length = coordinates.length;
                            int quarter = length / 4;
                            List<Coordinate> tmpList = new ArrayList<Coordinate>(coordinates.length);
                            for( int i = quarter; i < coordinates.length - 1; i++ ) {
                                tmpList.add(coordinates[i]);
                            }
                            for( int i = 0; i <= quarter; i++ ) {
                                tmpList.add(coordinates[i]);
                            }
                            Coordinate[] tmpArray = (Coordinate[]) tmpList.toArray(new Coordinate[tmpList.size()]);
                            geomList.add(gF.createLineString(tmpArray));
                        } else {
                            // TODO improve this part
                            throw new RuntimeException("Not implemented yet."); //$NON-NLS-1$
                            // int length = coordinates.length;
                            // int half = length / 2;
                            // Coordinate[] first = new Coordinate[half];
                            // Coordinate[] second = new Coordinate[length - half + 1];
                            // System.arraycopy(coordinates, 0, first, 0, first.length);
                            // System.arraycopy(coordinates, first.length - 1, second, 0,
                            // second.length);
                            // geomList.add(gF.createLineString(first));
                            // geomList.add(gF.createLineString(second));
                        }

                    }
                    lsArray = (LineString[]) geomList.toArray(new LineString[numGeometries]);
                    correctLineIntersections(featureElevationComparer, badFeatures, lsArray, id);
                }
                id++;
            } catch (Exception e) {
                e.printStackTrace();
                featureElevationComparer.setDirty(true);
                continue;
            }

            pm.worked(1);
        }
        pm.done();

        for( FeatureElevationComparer featureElevationComparer : badFeatures ) {
            if (featureElevationComparer.toRemove()) {
                continue;
            }
            SimpleFeature feature = featureElevationComparer.getFeature();
            if (!featureElevationComparer.isDirty()) {
                ((DefaultFeatureCollection) correctedFeatures).add(feature);
            } else {
                ((DefaultFeatureCollection) errorFeatures).add(feature);
            }
        }

    }

    private DijkstraIterator.EdgeWeighter costFunction() {
        return (new DijkstraIterator.EdgeWeighter(){
            public double getWeight( Edge e ) {
                int id = e.getID();
                if (id % 2 == 0) {
                    return 1;
                } else {
                    return 10000;
                }
            }
        });
    }

    /**
     * Method to correct line intersections.
     * 
     * @param currentFeatureElevationComparer the current checked line wrapper.
     * @param comparerList the list of lines that may intersect with the current checked line.
     * @param lsArray the geometries of the current checked line.
     * @param id the index of the current checked line in the compareList. Used 
     *          to avoid the intersection of the line with itself.  
     * @return false if correction went smooth, true if there were problems and
     *           the rotation of the geometry's first point is requested for a second try.
     * @throws Exception
     */
    private boolean correctLineIntersections( FeatureElevationComparer currentFeatureElevationComparer,
            List<FeatureElevationComparer> comparerList, LineString[] lsArray, int currentLineIndex ) throws Exception {

        ArrayList<LineString> newLines = new ArrayList<LineString>(lsArray.length);
        for( final LineString line : lsArray ) {
            Coordinate[] lineCoords = line.getCoordinates();
            boolean hadEqualBounds = false;
            if (lineCoords[0].distance(lineCoords[lineCoords.length - 1]) < DELTA6) {
                hadEqualBounds = true;
                Coordinate coordinate = new Coordinate();
                coordinate.x = lineCoords[lineCoords.length - 1].x + DELTA6;
                coordinate.y = lineCoords[lineCoords.length - 1].y + DELTA6;
                lineCoords[lineCoords.length - 1] = coordinate;
            }

            List<Polygon> intersectingPolygons = new ArrayList<Polygon>();

            PreparedGeometry preparedLine = PreparedGeometryFactory.prepare(line);

            int index = 0;
            for( FeatureElevationComparer featureComparer : comparerList ) {
                if (featureComparer.toRemove()) {
                    continue;
                }
                if (index == currentLineIndex) {
                    index++;
                    continue;
                }
                index++;

                Geometry geom = featureComparer.getGeometry();
                if (preparedLine.intersects(geom)) {
                    Geometry bufferPolygon = featureComparer.getBufferPolygon();
                    int numGeometries = bufferPolygon.getNumGeometries();
                    for( int i = 0; i < numGeometries; i++ ) {
                        Geometry geometryN = bufferPolygon.getGeometryN(i);
                        intersectingPolygons.add((Polygon) geometryN);
                    }
                }
            }

            if (intersectingPolygons.size() > 0) {
                final Geometry union = CascadedPolygonUnion.union(intersectingPolygons);
                if (union.covers(gF.createPoint(lineCoords[0]))) {
                    // request the rotation of the geometry's first point
                    return true;
                }

                final Geometry[] collection = new Geometry[1];
                long time1 = System.currentTimeMillis();
                try {
                    collection[0] = union.symDifference(line);
                } catch (Exception e) {
                    Thread t = new Thread(new Runnable(){

                        public void run() {
                            double snapTol = GeometrySnapper.computeOverlaySnapTolerance(union, line);
                            Geometry aFix = selfSnap(union, snapTol);
                            collection[0] = aFix.symDifference(line);
                        }
                    });

                    t.start();

                    long time2 = System.currentTimeMillis();
                    long sec = (time2 - time1) / 1000l;
                    while( sec < 60 && collection[0] == null ) {
                        Thread.sleep(300);
                        time2 = System.currentTimeMillis();
                        sec = (time2 - time1) / 1000l;
                    }

                    if (t.isAlive()) {
                        t.interrupt();
                    }

                    if (collection[0] == null) {
                        throw new RuntimeException("Didn't make it to create the snap.");
                    }

                }

                BasicLineGraphGenerator lineStringGen = new BasicLineGraphGenerator();
                if (collection[0] instanceof GeometryCollection) {
                    GeometryCollection geomCollection = (GeometryCollection) collection[0];
                    int numGeometries = geomCollection.getNumGeometries();
                    List<LineSegment> linesS = new ArrayList<LineSegment>(numGeometries);
                    List<LineSegment> otherLinesS = new ArrayList<LineSegment>(numGeometries);

                    for( int i = 0; i < numGeometries; i++ ) {
                        Geometry geometryN = geomCollection.getGeometryN(i);
                        Coordinate[] coordinates = geometryN.getCoordinates();

                        if (geometryN instanceof LineString) {
                            for( int j = 0; j < coordinates.length - 1; j = j + 1 ) {
                                Coordinate first = coordinates[j];
                                Coordinate sec = coordinates[j + 1];
                                LineSegment seg = new LineSegment(first, sec);
                                linesS.add(seg);
                            }
                        } else {
                            for( int j = 0; j < coordinates.length - 1; j = j + 1 ) {
                                Coordinate first = coordinates[j];
                                Coordinate sec = coordinates[j + 1];
                                LineSegment seg = new LineSegment(first, sec);
                                otherLinesS.add(seg);
                            }
                        }
                    }

                    int id = 0;
                    for( LineSegment l : linesS ) {
                        lineStringGen.add(l);
                        Edge edge = lineStringGen.getEdge(l.p0, l.p1);
                        edge.setID(id);
                        id = id + 2;
                    }
                    id = 1;
                    for( LineSegment l : otherLinesS ) {
                        lineStringGen.add(l);
                        Edge edge = lineStringGen.getEdge(l.p0, l.p1);
                        edge.setID(id);
                        id = id + 2;
                    }

                    Graph graph = lineStringGen.getGraph();

                    Node startNode = lineStringGen.getNode(lineCoords[0]);
                    Node endNode = lineStringGen.getNode(lineCoords[lineCoords.length - 1]);

                    DijkstraShortestPathFinder pfinder = new DijkstraShortestPathFinder(graph, startNode, costFunction());
                    pfinder.calculate();
                    Path path = null;
                    try {
                        path = pfinder.getPath(endNode);
                    } catch (Exception e) {
                        return true;
                    }

                    LineSequencer ls = new LineSequencer();
                    for( Iterator e = path.getEdges().iterator(); e.hasNext(); ) {
                        Edge edge = (Edge) e.next();
                        Object object = edge.getObject();
                        if (object instanceof LineSegment) {
                            LineSegment seg = (LineSegment) object;
                            ls.add(gF.createLineString(new Coordinate[]{seg.p0, seg.p1}));
                        }
                    }
                    Geometry sequencedLineStrings = ls.getSequencedLineStrings();
                    Coordinate[] coordinates = sequencedLineStrings.getCoordinates();
                    if (coordinates.length != 0) {
                        if (hadEqualBounds) {
                            coordinates[coordinates.length - 1] = new Coordinate(coordinates[0]);
                        }

                        List<Coordinate> tmp = new ArrayList<Coordinate>(coordinates.length);
                        for( int i = 0; i < coordinates.length; i++ ) {
                            if (i % 2 == 0 || i == coordinates.length - 1) {
                                tmp.add(coordinates[i]);
                            }
                        }
                        coordinates = (Coordinate[]) tmp.toArray(new Coordinate[tmp.size()]);

                        Geometry resultGeometry = gF.createLineString(coordinates);
                        double newLength = resultGeometry.getLength();
                        double length = line.getLength();

                        if (Math.abs(length - newLength) > 0.5 * length) {
                            return true;
                        } else {
                            newLines.add((LineString) resultGeometry);
                        }
                    } else {
                        // request the rotation of the geometry's first point
                        return true;
                    }

                }

            } else {
                newLines.add(line);
            }
        }

        LineString[] linesArray;
        if (newLines.size() == 0) {
            linesArray = lsArray;
        } else {
            linesArray = (LineString[]) newLines.toArray(new LineString[newLines.size()]);
        }
        MultiLineString multiLineString = gF.createMultiLineString(linesArray);
        currentFeatureElevationComparer.substituteGeometry(multiLineString);
        return false;
    }

    private Geometry selfSnap( Geometry g, double snapTolerance ) {
        GeometrySnapper snapper = new GeometrySnapper(g);
        Geometry snapped = snapper.snapTo(g, snapTolerance);
        // need to "clean" snapped geometry - use buffer(0) as a simple way to do this
        Geometry fix = snapped.buffer(0);
        return fix;
    }
}
