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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.linemerge.LineSequencer;
import com.vividsolutions.jts.operation.overlay.snap.GeometrySnapper;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

@Description("Collection of Smoothing Algorithms. Type 0: McMasters Sliding Averaging "
        + "Algorithm. The new position of each point "
        + "is the average of the pLookahead  points around. Parameter pSlide is used for "
        + "linear interpolation between old and new position.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Smoothing, Vector")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class LineSmoother extends JGTModel {

    @Description("The features to be smoothed.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inFeatures;

    @Description("The smoothing type: McMasters smoothing average (0 = default).")
    @In
    public int pType = 0;

    @Description("The number of points to consider in every smoothing step (default = 7).")
    @In
    public int pLookahead = 7;

    @Description("Minimum length accepted for a line. If it is shorter than that value, the line is removed.")
    @In
    public int pLimit = 0;

    @Description("Slide parameter.")
    @In
    public double pSlide = 0.9;

    @Description("Protection buffer.")
    @In
    public double pBuffer = 0.5;

    @Description("Densifier tollerance.")
    @In
    public Double pDensify = null;

    @Description("Simplifier tollerance.")
    @In
    public Double pSimplify = null;

    @Description("Field name of sorting attribute.")
    @In
    public String fSort = null;

    @Description("Switch for correction.")
    @In
    public boolean doCorrection = true;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The smoothed features.")
    @Out
    public FeatureCollection<SimpleFeatureType, SimpleFeature> outFeatures;

    @Description("The nonsmoothed features.")
    @Out
    public FeatureCollection<SimpleFeatureType, SimpleFeature> errorFeatures;

    private static final double DELTA = 0.00001;

    private GeometryFactory gF = GeometryUtilities.gf();

    private double densify = -1;
    private double simplify = -1;

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
        if (doCorrection) {
			pBuffer = -1;
		}

        outFeatures = FeatureCollections.newCollection();
        errorFeatures = FeatureCollections.newCollection();

        int id = 0;
        FeatureIterator<SimpleFeature> inFeatureIterator = inFeatures.features();
        int size = inFeatures.size();
        List<FeatureElevationComparer> comparerList = new ArrayList<FeatureElevationComparer>();
        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inFeatures.getSchema());

        if (pLookahead != 0) {
            // Geometry first = null;
            pm.beginTask("Smoothing features...", size);
            while( inFeatureIterator.hasNext() ) {
                SimpleFeature feature = inFeatureIterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                List<LineString> lsList = smoothGeometries(geometry);
				if (lsList.size() != 0) {
					LineString[] lsArray = (LineString[]) lsList.toArray(new LineString[lsList.size()]);

					MultiLineString multiLineString = gF.createMultiLineString(lsArray);
					SimpleFeature newFeature = fGS.substituteGeometry(feature, multiLineString, id);
					comparerList.add(new FeatureElevationComparer(newFeature, fSort, pBuffer, pLimit));
					id++;
				}
                pm.worked(1);
            }
            pm.done();
            inFeatures.close(inFeatureIterator);
        } else {
            while( inFeatureIterator.hasNext() ) {
                SimpleFeature feature = inFeatureIterator.next();
                comparerList.add(new FeatureElevationComparer(feature, fSort, pBuffer, pLimit));
            }
            inFeatures.close(inFeatureIterator);
        }

        if (doCorrection) {
            Collections.sort(comparerList);
            Collections.reverse(comparerList);

            id = 0;
            size = comparerList.size();
            pm.beginTask("Correcting intersections...", size);
            for( FeatureElevationComparer featureElevationComparer : comparerList ) {
            	if (featureElevationComparer.toRemove()) {
					continue;
				}
            	
                SimpleFeature feature = featureElevationComparer.getFeature();

                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                int numGeometries = geometry.getNumGeometries();

                List<LineString> geomList = new ArrayList<LineString>();
                for( int i = 0; i < numGeometries; i++ ) {
                    geomList.add((LineString) geometry.getGeometryN(i));
                }
                LineString[] lsArray = (LineString[]) geomList
                        .toArray(new LineString[numGeometries]);

                try {
                    checkLineIntersections(featureElevationComparer, comparerList, lsArray, id);
                    id++;
                } catch (Exception e) {
                    e.printStackTrace();
                    featureElevationComparer.setDirty(true);
                    continue;
                }

                pm.worked(1);
            }
            pm.done();

            for( FeatureElevationComparer featureElevationComparer : comparerList ) {
               	if (featureElevationComparer.toRemove()) {
					continue;
				}
                SimpleFeature feature = featureElevationComparer.getFeature();
                if (!featureElevationComparer.isDirty()) {
                    outFeatures.add(feature);
                } else {
                    errorFeatures.add(feature);
                }
            }

        } else {
            for( FeatureElevationComparer featureElevationComparer : comparerList ) {
                outFeatures.add(featureElevationComparer.getFeature());
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
                    return 100;
                }
            }
        });
    }

    private void checkLineIntersections( FeatureElevationComparer currentFeatureElevationComparer,
            List<FeatureElevationComparer> comparerList, LineString[] lsArray, int id2 )
            throws Exception {

        ArrayList<LineString> newLines = new ArrayList<LineString>();
        for( LineString line : lsArray ) {
            Coordinate[] lineCoords = line.getCoordinates();
            boolean hadEqualBounds = false;
            if (lineCoords[0].distance(lineCoords[lineCoords.length - 1]) < DELTA) {
                hadEqualBounds = true;
                Coordinate coordinate = lineCoords[lineCoords.length - 1];
                coordinate.x = coordinate.x + DELTA;
                coordinate.y = coordinate.y + DELTA;
            }

            List<Polygon> intersectingPolygons = new ArrayList<Polygon>();

            PreparedGeometry preparedLine = PreparedGeometryFactory.prepare(line);

            long t0 = System.currentTimeMillis();

            int index = 0;
            for( FeatureElevationComparer featureComparer : comparerList ) {
               	if (featureComparer.toRemove()) {
					continue;
				}
                if (index == id2) {
                    continue;
                }
                index++;

                Geometry geom = featureComparer.getGeometry();
                if (preparedLine.intersects(geom)) {
                    Geometry bufferPolygon = featureComparer.getBufferPolygon();
                    // if (!featureComparer.isSnapped()) {
                    // double snapTol = GeometrySnapper.computeOverlaySnapTolerance(bufferPolygon,
                    // line);
                    // Geometry snapped = selfSnap(bufferPolygon, snapTol);
                    //
                    // System.out.println(bufferPolygon.toText());
                    // System.out.println(snapped.toText());
                    // featureComparer.setBufferPolygon(snapped);
                    // featureComparer.setSnapped(true);
                    // bufferPolygon = snapped;
                    // }
                	int numGeometries = bufferPolygon.getNumGeometries();
					for (int i = 0; i < numGeometries; i++) {
						Geometry geometryN = bufferPolygon.getGeometryN(i);
						intersectingPolygons.add((Polygon) geometryN);
					}
                    
                }
            }

            // long t1 = System.currentTimeMillis();
            // printTime("check intersections", t0, t1);

            if (intersectingPolygons.size() > 0) {

                // LineString[] intersectingLinesArray = (LineString[]) intersectingLines
                // .toArray(new LineString[intersectingLines.size()]);
                Polygon[] bufferPolygons = (Polygon[]) intersectingPolygons
                        .toArray(new Polygon[intersectingPolygons.size()]);
                MultiPolygon multiPolygon = gF.createMultiPolygon(bufferPolygons);
                // long t11 = System.currentTimeMillis();
                // printTime("buffer", t1, t11);

                // GeometryCollection coll = new GeometryCollection(bufferPolygons, gF);
                double snapTol = GeometrySnapper.computeOverlaySnapTolerance(multiPolygon, line);
                Geometry aFix = selfSnap(multiPolygon, snapTol);
                // Geometry aFix = gF.createMultiPolygon((Polygon[]) intersectingPolygons
                // .toArray(new Polygon[intersectingPolygons.size()]));

                // Geometry bFix = selfSnap(line, snapTol);

                // long t2 = System.currentTimeMillis();
                // printTime("fix snap", t11, t2);

                Geometry collection = aFix.symDifference(line);
                // System.out.println(aFix.toText());
                // System.out.println(line.toText());

                BasicLineGraphGenerator lineStringGen = new BasicLineGraphGenerator();
                if (collection instanceof GeometryCollection) {
                    List<LineSegment> linesS = new ArrayList<LineSegment>();
                    List<LineSegment> otherLinesS = new ArrayList<LineSegment>();

                    GeometryCollection geomCollection = (GeometryCollection) collection;
                    int numGeometries = geomCollection.getNumGeometries();
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

                    // long t3 = System.currentTimeMillis();
                    // printTime("get geometries", t2, t3);

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

                    // long t4 = System.currentTimeMillis();
                    // printTime("create graph", t3, t4);

                    Node startNode = lineStringGen.getNode(lineCoords[0]);
                    Node endNode = lineStringGen.getNode(lineCoords[lineCoords.length - 1]);

                    DijkstraShortestPathFinder pfinder = new DijkstraShortestPathFinder(graph,
                            startNode, costFunction());
                    pfinder.calculate();
                    Path path = pfinder.getPath(endNode);

                    // long t5 = System.currentTimeMillis();
                    // printTime("shortest path", t4, t5);

                    LineSequencer ls = new LineSequencer();
                    for( Iterator e = path.getEdges().iterator(); e.hasNext(); ) {
                        Edge edge = (Edge) e.next();
                        Object object = edge.getObject();
                        if (object instanceof LineSegment) {
                            LineSegment seg = (LineSegment) object;
                            ls.add(gF.createLineString(new Coordinate[]{seg.p0, seg.p1}));
                        }
                        // features.add( feature );
                    }
                    Geometry sequencedLineStrings = ls.getSequencedLineStrings();
                    Coordinate[] coordinates = sequencedLineStrings.getCoordinates();
                    if (coordinates.length != 0) {
                        if (hadEqualBounds) {
                            coordinates[coordinates.length - 1] = new Coordinate(coordinates[0]);
                        }

                        List<Coordinate> tmp = new ArrayList<Coordinate>();
                        for( int i = 0; i < coordinates.length; i++ ) {
                            if (i % 2 == 0 || i == coordinates.length - 1) {
                                tmp.add(coordinates[i]);
                            }
                        }
                        coordinates = (Coordinate[]) tmp.toArray(new Coordinate[tmp.size()]);

                        LineString lStr = gF.createLineString(coordinates);
                        // System.out.println(lStr.toText());
                        // long t6 = System.currentTimeMillis();
                        // printTime("create lines", t5, t6);

                        // DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(lStr);
                        // simplifier.setDistanceTolerance(0);
                        // Geometry resultGeometry = simplifier.getResultGeometry();

                        // int newLength = resultGeometry.getCoordinates().length;
                        Geometry resultGeometry = lStr;
                        int newLength = resultGeometry.getCoordinates().length;
                        int length = lineCoords.length;

                        if (Math.abs(length - newLength) > 0.5 * length) {
                            System.out.println("error");
                            newLines.add(line);
                            currentFeatureElevationComparer.setDirty(true);
                        } else {
                            newLines.add((LineString) resultGeometry);
                        }

                        // System.out.println(resultGeometry.toText());

                        // long t7 = System.currentTimeMillis();
                        // printTime("simplify", t6, t7);
                    } else {
                        newLines.add(line);
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
    }

    private Geometry selfSnap( Geometry g, double snapTolerance ) {
        GeometrySnapper snapper = new GeometrySnapper(g);
        Geometry snapped = snapper.snapTo(g, snapTolerance);
        // need to "clean" snapped geometry - use buffer(0) as a simple way to do this
        Geometry fix = snapped.buffer(0);
        return fix;
    }

    private MultiLineString checkIntersectionsShortestPath( LineString[] lsArray,
            List<Geometry> protectedAreas ) throws Exception {

        ArrayList<LineString> newLines = new ArrayList<LineString>();
        for( LineString line : lsArray ) {
            Coordinate[] lineCoords = line.getCoordinates();

            List<Polygon> intersectingAreas = new ArrayList<Polygon>();
            for( Geometry protectedArea : protectedAreas ) {
                if (line.intersects(protectedArea)) {
                    int numGeometries = protectedArea.getNumGeometries();
                    for( int i = 0; i < numGeometries; i++ ) {
                        Geometry geometryN = protectedArea.getGeometryN(i);
                        intersectingAreas.add((Polygon) geometryN);
                    }
                }
            }
            // long t0 = System.currentTimeMillis();
            if (intersectingAreas.size() > 0) {
                Polygon[] intersectingAreasArray = (Polygon[]) intersectingAreas
                        .toArray(new Polygon[intersectingAreas.size()]);
                MultiPolygon multiPolygon = gF.createMultiPolygon(intersectingAreasArray);

                // System.out.println("********************************");
                // // problem, we have to fix this
                // long t1 = System.currentTimeMillis();
                // printTime("intersect", t0, t1);

                // System.out.println(line.toText());
                // System.out.println(multiPolygon.toText());
                Geometry collection = multiPolygon.symDifference(line);
                // long t2 = System.currentTimeMillis();
                // printTime("symdiff", t1, t2);

                BasicLineGraphGenerator lineStringGen = new BasicLineGraphGenerator();
                if (collection instanceof GeometryCollection) {
                    List<LineSegment> linesS = new ArrayList<LineSegment>();
                    List<LineSegment> polygonsS = new ArrayList<LineSegment>();
                    GeometryCollection geomCollection = (GeometryCollection) collection;
                    int numGeometries = geomCollection.getNumGeometries();
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
                                polygonsS.add(seg);
                            }
                        }
                    }

                    // long t3 = System.currentTimeMillis();
                    // printTime("get geometries", t2, t3);

                    int id = 0;
                    for( LineSegment l : linesS ) {
                        lineStringGen.add(l);
                        Edge edge = lineStringGen.getEdge(l.p0, l.p1);
                        edge.setID(id);
                        id = id + 2;
                    }
                    id = 1;
                    for( LineSegment l : polygonsS ) {
                        lineStringGen.add(l);
                        Edge edge = lineStringGen.getEdge(l.p0, l.p1);
                        edge.setID(id);
                        id = id + 2;
                    }

                    Graph graph = lineStringGen.getGraph();

                    // long t4 = System.currentTimeMillis();
                    // printTime("create graph", t3, t4);

                    Node startNode = lineStringGen.getNode(lineCoords[0]);
                    Node endNode = lineStringGen.getNode(lineCoords[lineCoords.length - 1]);

                    DijkstraShortestPathFinder pfinder = new DijkstraShortestPathFinder(graph,
                            startNode, costFunction());
                    pfinder.calculate();
                    Path path = pfinder.getPath(endNode);

                    // long t5 = System.currentTimeMillis();
                    // printTime("shortest path", t4, t5);

                    LineSequencer ls = new LineSequencer();
                    for( Iterator e = path.getEdges().iterator(); e.hasNext(); ) {
                        Edge edge = (Edge) e.next();
                        Object object = edge.getObject();
                        if (object instanceof LineSegment) {
                            LineSegment seg = (LineSegment) object;
                            ls.add(gF.createLineString(new Coordinate[]{seg.p0, seg.p1}));
                        }
                        // features.add( feature );
                    }
                    Geometry sequencedLineStrings = ls.getSequencedLineStrings();
                    Coordinate[] coordinates = sequencedLineStrings.getCoordinates();
                    LineString lStr = gF.createLineString(coordinates);
                    // System.out.println(lStr.toText());
                    // long t6 = System.currentTimeMillis();
                    // printTime("create lines", t5, t6);

                    DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(lStr);
                    simplifier.setDistanceTolerance(0);
                    Geometry resultGeometry = simplifier.getResultGeometry();
                    newLines.add((LineString) resultGeometry);
                    // System.out.println(resultGeometry.toText());

                    // long t7 = System.currentTimeMillis();
                    // printTime("simplify", t6, t7);
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
        return multiLineString;
    }

    private void printTime( String msg, long t1, long t2 ) {
        long diff = t2 - t1;
        long a = diff / 1000l;
        System.out.println(msg + ": millis " + diff + " seconds " + a);
    }

    private MultiLineString checkIntersections( MultiLineString mlString,
            List<Geometry> protectedAreas ) {

        PreparedGeometry mlStringPrep = PreparedGeometryFactory.prepare(mlString);
        int lineCoordNum = mlString.getCoordinates().length;
        double percentageThres = 0.1;

        ArrayList<ArrayList<Coordinate>> newLine = new ArrayList<ArrayList<Coordinate>>();

        for( Geometry protectedArea : protectedAreas ) {
            if (mlStringPrep.intersects(protectedArea)) {
                System.out.println(mlString.toText());
                System.out.println(protectedArea.toText());
                // problem, we have to fix this
                Geometry collection = protectedArea.symDifference(mlString);

                if (collection instanceof GeometryCollection) {
                    GeometryCollection geomCollection = (GeometryCollection) collection;

                    List<LineString> linesList = new ArrayList<LineString>();
                    List<Geometry> polygonList = new ArrayList<Geometry>();
                    int numGeometries = geomCollection.getNumGeometries();
                    for( int i = 0; i < numGeometries; i++ ) {
                        Geometry geometryN = geomCollection.getGeometryN(i);
                        if (geometryN instanceof LineString) {
                            LineString line = (LineString) geometryN;
                            linesList.add(line);
                        } else {
                            polygonList.add(geometryN);
                        }
                    }

                    List<Coordinate[]> polygonCoordsList = new ArrayList<Coordinate[]>();
                    for( Geometry polygon : polygonList ) {
                        polygonCoordsList.add(polygon.getCoordinates());
                    }
                    for( int i = 0; i < linesList.size(); i = i + 1 ) {
                        ArrayList<Coordinate> tmpList = new ArrayList<Coordinate>();
                        LineString l1 = linesList.get(i);
                        LineString l2 = null;
                        Coordinate[] l1Coords = l1.getCoordinates();
                        for( Coordinate coordinate : l1Coords ) {
                            coordinate.z = Double.NaN;
                        }
                        int cNum = l1Coords.length;

                        Coordinate l1LastCoord = l1Coords[cNum - 1];

                        int l1PointIndex = -1;
                        int l2PointIndex = -1;
                        for( Coordinate[] polygonCoords : polygonCoordsList ) {
                            if (l1PointIndex != -1) {
                                break;
                            }
                            for( int j = 0; j < polygonCoords.length; j++ ) {
                                double distance = polygonCoords[j].distance(l1LastCoord);
                                if (distance < DELTA) {
                                    l1PointIndex = j;
                                    break;
                                }
                            }
                        }

                        if (l1PointIndex == -1) {
                            // loose end, add it and continue
                            for( Coordinate coordinate : l1Coords ) {
                                tmpList.add(coordinate);
                            }
                            newLine.add(tmpList);
                            continue;
                        }

                        // find the next point
                        Coordinate[] matchedPolygonCoordinates = null;
                        for( int j = i + 1; j < linesList.size(); j++ ) {
                            LineString tmpL2 = linesList.get(j);
                            Coordinate[] l2Coords = tmpL2.getCoordinates();
                            int c2Num = l2Coords.length;
                            // only first and last can be short
                            if (c2Num < lineCoordNum * percentageThres && j != 0
                                    && j != linesList.size() - 1) {
                                System.out.println("shorty: " + i);
                                i = j;
                                continue;
                            }

                            Coordinate l2FirstCoord = l2Coords[0];

                            for( Coordinate[] polygonCoords : polygonCoordsList ) {
                                if (l2PointIndex != -1) {
                                    break;
                                }
                                for( int k = 0; k < polygonCoords.length; k++ ) {
                                    if (polygonCoords[k].distance(l2FirstCoord) < DELTA) {
                                        l2PointIndex = k;
                                        matchedPolygonCoordinates = polygonCoords;
                                        break;
                                    }
                                }
                            }

                            if (l2PointIndex == -1) {
                                throw new RuntimeException();
                            }

                            /*
                             * so we found the 2 points that should be 
                             * connected along the buffer. But are they 
                             * the right ones? 
                             */
                            // check if the second line is an intermediate
                            LineString tmpLine = gF.createLineString(new Coordinate[]{l1LastCoord,
                                    l2FirstCoord});
                            Geometry tmpIntersection = null;
                            for( Geometry polygon : polygonList ) {
                                if (polygon.intersects(tmpLine)) {
                                    tmpIntersection = polygon.intersection(tmpLine);
                                    break;
                                }
                            }

                            if (tmpIntersection.getNumGeometries() > 1) {
                                // wrong piece, try the next line
                                continue;
                            } else {
                                // found the right l2
                                l2 = tmpL2;
                                // update global index
                                i = j - 1;
                                break;
                            }
                        }

                        // ok, we have both lines and the indexes
                        for( Coordinate coordinate : l1Coords ) {
                            tmpList.add(coordinate);
                        }
                        if (l1PointIndex < l2PointIndex) {
                            for( int j = l1PointIndex; j <= l2PointIndex; j = j + 1 ) {
                                tmpList.add(matchedPolygonCoordinates[j]);
                            }
                        } else {
                            for( int j = l1PointIndex; j >= l2PointIndex; j = j - 1 ) {
                                try {
                                    tmpList.add(matchedPolygonCoordinates[j]);
                                } catch (Exception e) {
                                    System.out.println("FUCK!");
                                }
                            }
                        }
                        newLine.add(tmpList);
                    }

                    LineString[] linesArray = new LineString[newLine.size()];
                    int index = 0;
                    for( ArrayList<Coordinate> coordinateList : newLine ) {
                        LineString lineString = gF.createLineString((Coordinate[]) coordinateList
                                .toArray(new Coordinate[coordinateList.size()]));
                        linesArray[index++] = lineString;
                    }
                    return gF.createMultiLineString(linesArray);
                }
            }
        }

        return mlString;

    }

    private List<LineString> smoothGeometries( Geometry geometry ) {
        List<LineString> lsList = new ArrayList<LineString>();
        int numGeometries = geometry.getNumGeometries();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometryN = geometry.getGeometryN(i);
            double length = geometryN.getLength();
            if (length <= pLimit) {
                continue;
            }

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

            Coordinate[] smoothedArray = null;
            if (smoothedCoords != null) {
                smoothedArray = (Coordinate[]) smoothedCoords.toArray(new Coordinate[smoothedCoords
                        .size()]);
            } else {
                smoothedArray = geometryN.getCoordinates();
            }
            LineString lineString = gF.createLineString(smoothedArray);

            if (simplify != -1) {
                TopologyPreservingSimplifier tpSimplifier = new TopologyPreservingSimplifier(
                        lineString);
                tpSimplifier.setDistanceTolerance(pSimplify);
                lineString = (LineString) tpSimplifier.getResultGeometry();
            }

            lsList.add(lineString);
        }
        return lsList;
    }

    public static void main( String[] args ) {

        Coordinate[] lineCoord = new Coordinate[7];
        lineCoord[0] = new Coordinate(0, 0);
        lineCoord[1] = new Coordinate(1, 1);
        lineCoord[2] = new Coordinate(2, 2);
        lineCoord[3] = new Coordinate(3, 3);
        lineCoord[4] = new Coordinate(4, 2);
        lineCoord[5] = new Coordinate(5, 1);
        lineCoord[6] = new Coordinate(6, 0);

        Coordinate[] polygonCoord = new Coordinate[4];
        polygonCoord[0] = new Coordinate(0, 1);
        polygonCoord[1] = new Coordinate(0, 2);
        polygonCoord[2] = new Coordinate(6, 2);
        polygonCoord[3] = new Coordinate(6, 1);
        // polygonCoord[4] = new Coordinate(0, 1);

        GeometryFactory gfac = GeometryUtilities.gf();

        LineString lineString = gfac.createLineString(lineCoord);
        System.out.println(lineString.toText());

        LineString polyString = gfac.createLineString(polygonCoord);
        System.out.println(polyString.toText());
        Geometry symdifference = lineString.symDifference(polyString);
        System.out.println(symdifference.toText());

        // LinearRing linearRing = gfac.createLinearRing(polygonCoord);
        // Polygon polygon = gfac.createPolygon(linearRing, null);
        // Geometry symdifference = lineString.symDifference(polygon);
        // System.out.println(symdifference.toText());

        // List<LineSegment> linesS = new ArrayList<LineSegment>();
        // List<LineSegment> polygonsS = new ArrayList<LineSegment>();
        // int numGeometries = symdifference.getNumGeometries();
        // for( int i = 0; i < numGeometries; i++ ) {
        // Geometry geometryN = symdifference.getGeometryN(i);
        // if (geometryN instanceof LineString) {
        // LineString line = (LineString) geometryN;
        // Coordinate[] coordinates = line.getCoordinates();
        // for( int j = 0; j < coordinates.length - 1; j = j + 1 ) {
        // Coordinate first = coordinates[j];
        // Coordinate sec = coordinates[j + 1];
        // LineSegment seg = new LineSegment(first, sec);
        // linesS.add(seg);
        // }
        // } else {
        // Coordinate[] coordinates = geometryN.getCoordinates();
        // for( int j = 0; j < coordinates.length - 1; j = j + 1 ) {
        // Coordinate first = coordinates[j];
        // Coordinate sec = coordinates[j + 1];
        // LineSegment seg = new LineSegment(first, sec);
        // polygonsS.add(seg);
        // }
        // }
        // }
        //
        // BasicLineGraphGenerator lineStringGen = new BasicLineGraphGenerator();
        // int id = 0;
        // for( LineSegment l : linesS ) {
        // lineStringGen.add(l);
        // Edge edge = lineStringGen.getEdge(l.p0, l.p1);
        // edge.setID(id);
        // id = id + 2;
        // }
        // id = 1;
        // for( LineSegment l : polygonsS ) {
        // lineStringGen.add(l);
        // Edge edge = lineStringGen.getEdge(l.p0, l.p1);
        // edge.setID(id);
        // id = id + 2;
        // }
        //
        // Graph graph = lineStringGen.getGraph();
        //
        // Node startNode = lineStringGen.getNode(lineCoord[0]);
        // Node endNode = lineStringGen.getNode(lineCoord[6]);
        //
        // DijkstraShortestPathFinder pfinder = new DijkstraShortestPathFinder(graph, startNode,
        // costFunction());
        // pfinder.calculate();
        // Path path = pfinder.getPath(endNode);
        //
        // LineSequencer ls = new LineSequencer();
        // for( Iterator e = path.getEdges().iterator(); e.hasNext(); ) {
        // Edge edge = (Edge) e.next();
        // Object object = edge.getObject();
        // if (object instanceof LineSegment) {
        // LineSegment seg = (LineSegment) object;
        //
        // ls.add(gfac.createLineString(new Coordinate[]{seg.p0, seg.p1}));
        // }
        // System.out.println();
        // // features.add( feature );
        // }
        // Geometry sequencedLineStrings = ls.getSequencedLineStrings();
        // Coordinate[] coordinates = sequencedLineStrings.getCoordinates();
        // LineString lStr = gfac.createLineString(coordinates);
        // DouglasPeuckerSimplifier simple = new DouglasPeuckerSimplifier(lStr);
        // simple.setDistanceTolerance(0);
        // Geometry resultGeometry = simple.getResultGeometry();
        // System.out.println(resultGeometry.toText());

    }

}
