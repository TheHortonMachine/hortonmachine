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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

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

    @Description("Minimum number of points for a line. If the points number is below that value, the line is removed.")
    @In
    public int pLimit = 2 * pLookahead;

    @Description("Slide parameter.")
    @In
    public double pSlide = 0.9;

    @Description("Protection buffer.")
    @In
    public double pBuffer = 0.5;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The smoothed features.")
    @Out
    public FeatureCollection<SimpleFeatureType, SimpleFeature> outFeatures;

    private GeometryFactory gF = GeometryUtilities.gf();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFeatures == null, doReset)) {
            return;
        }
        FeatureIterator<SimpleFeature> inFeatureIterator = inFeatures.features();

        outFeatures = FeatureCollections.newCollection();

        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inFeatures.getSchema());

        List<Geometry> protectedAreas = new ArrayList<Geometry>();

        int id = 0;
        int size = inFeatures.size();
        pm.beginTask("Smoothing features...", size);
        while( inFeatureIterator.hasNext() ) {
            SimpleFeature feature = inFeatureIterator.next();

            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            int numGeometries = geometry.getNumGeometries();

            List<LineString> lsList = smoothGeometries(geometry, numGeometries);
            int geometriesNum = lsList.size();
            if (geometriesNum == 0) {
                pm.worked(1);
                continue;
            }

            LineString[] lsArray = (LineString[]) lsList.toArray(new LineString[geometriesNum]);

            MultiLineString mlString = gF.createMultiLineString(lsArray);

            mlString = checkIntersections(mlString, protectedAreas);

            SimpleFeature newFeature = fGS.substituteGeometry(feature, mlString, id);
            id++;

            // BufferOp bufOp = new BufferOp(mlString);
            // bufOp.setEndCapStyle(BufferParameters.CAP_FLAT);
            // Geometry protectedBuffer = bufOp.getResultGeometry(pBuffer);
            // Geometry union = mlString.union();
            /*LineMerger lineMerger = new LineMerger();
            lineMerger.add(mlString);
            Collection mergedLineStrings = lineMerger.getMergedLineStrings();
            GeometryCollection gC = new GeometryCollection((Geometry[]) mergedLineStrings
                    .toArray(new Geometry[mergedLineStrings.size()]), gF);*/
            Geometry protectedBuffer = mlString.buffer(pBuffer);
            protectedAreas.add(protectedBuffer);

            outFeatures.add(newFeature);
            pm.worked(1);
        }
        pm.done();

        inFeatures.close(inFeatureIterator);

    }

    private MultiLineString checkIntersections( MultiLineString mlString,
            List<Geometry> protectedAreas ) {

        PreparedGeometry mlStringPrep = PreparedGeometryFactory.prepare(mlString);
        int lineCoordNum = mlString.getCoordinates().length;
        double percentageThres = 0.03;

        ArrayList<ArrayList<Coordinate>> newLine = new ArrayList<ArrayList<Coordinate>>();

        for( Geometry protectedArea : protectedAreas ) {
            if (mlStringPrep.intersects(protectedArea)) {
                System.out.println(mlString.toText());
                System.out.println(protectedArea.toText());
                // problem, we have to fix this
                Geometry collection = protectedArea.symDifference(mlString);
                if (collection instanceof GeometryCollection) {
                    ArrayList<Coordinate> tmpList = new ArrayList<Coordinate>();
                    GeometryCollection geomCollection = (GeometryCollection) collection;

                    List<LineString> linesList = new ArrayList<LineString>();
                    Geometry polygon = null;
                    int numGeometries = geomCollection.getNumGeometries();
                    for( int i = 0; i < numGeometries; i++ ) {
                        Geometry geometryN = geomCollection.getGeometryN(i);
                        if (geometryN instanceof LineString) {
                            LineString line = (LineString) geometryN;
                            linesList.add(line);
                        } else {
                            if (polygon != null) {
                                throw new RuntimeException();
                            }
                            polygon = geometryN;
                        }
                    }

                    Coordinate[] polygonCoords = polygon.getCoordinates();
                    for( int i = 0; i < linesList.size(); i = i + 1 ) {
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
                        for( int j = 0; j < polygonCoords.length; j++ ) {
                            double distance = polygonCoords[j].distance(l1LastCoord);
                            System.out.println(distance);
                            if (distance < 0.001) {
                                l1PointIndex = j;
                            }
                        }

                        if (l1PointIndex == -1) {
                            // loose end, add it and continue
                            for( Coordinate coordinate : l1Coords ) {
                                tmpList.add(coordinate);
                            }
                            continue;
                        }

                        // find the next point
                        for( int j = i + 1; j < linesList.size(); j++ ) {
                            LineString tmpL2 = linesList.get(j);
                            Coordinate[] l2Coords = tmpL2.getCoordinates();
                            // int c2Num = l2Coords.length;
                            Coordinate l2FirstCoord = l2Coords[0];

                            for( int k = 0; k < polygonCoords.length; k++ ) {
                                if (polygonCoords[k].distance(l2FirstCoord) < 0.001) {
                                    l2PointIndex = k;
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
                            Geometry tmpIntersection = polygon.intersection(tmpLine);
                            if (tmpIntersection.getNumGeometries() > 1) {
                                // wrong piece, try the next line
                                continue;
                            } else {
                                // found the right l2
                                l2 = tmpL2;
                                // update global index
                                i = j;
                                break;
                            }
                        }

                        // ok, we have both lines and the indexes
                        for( Coordinate coordinate : l1Coords ) {
                            tmpList.add(coordinate);
                        }
                        int first = l1PointIndex < l2PointIndex ? l1PointIndex : l2PointIndex;
                        int last = l1PointIndex < l2PointIndex ? l2PointIndex : l1PointIndex;
                        for( int j = first; j <= last; j++ ) {
                            tmpList.add(polygonCoords[j]);
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

    private List<LineString> smoothGeometries( Geometry geometry, int numGeometries ) {
        List<LineString> lsList = new ArrayList<LineString>();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometryN = geometry.getGeometryN(i);

            int coordinatesNum = geometryN.getCoordinates().length;
            if (coordinatesNum <= pLimit) {
                continue;
            }

            List<Coordinate> smoothedCoords = Collections.emptyList();;
            switch( pType ) {
            case 0:
            default:
                FeatureSlidingAverage fSA = new FeatureSlidingAverage(geometryN);
                smoothedCoords = fSA.smooth(pLookahead, false, pSlide);
                break;
            }

            Coordinate[] smoothedArray = null;
            if (smoothedCoords != null) {
                smoothedArray = (Coordinate[]) smoothedCoords.toArray(new Coordinate[smoothedCoords
                        .size()]);
            } else {
                smoothedArray = geometryN.getCoordinates();
            }
            LineString lineString = gF.createLineString(smoothedArray);
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

        Coordinate[] polygonCoord = new Coordinate[5];
        polygonCoord[0] = new Coordinate(0, 1);
        polygonCoord[1] = new Coordinate(0, 4);
        polygonCoord[2] = new Coordinate(6, 4);
        polygonCoord[3] = new Coordinate(6, 1);
        polygonCoord[4] = new Coordinate(0, 1);

        GeometryFactory gfac = GeometryUtilities.gf();

        LineString lineString = gfac.createLineString(lineCoord);

        LinearRing linearRing = gfac.createLinearRing(polygonCoord);
        Polygon polygon = gfac.createPolygon(linearRing, null);

        Geometry intersection = lineString.intersection(polygon);
        System.out.println(intersection.toText());

        Geometry difference = lineString.difference(polygon);
        System.out.println(difference.toText());

        Geometry symdifference = lineString.symDifference(polygon);
        System.out.println(symdifference.toText());

    }

}
