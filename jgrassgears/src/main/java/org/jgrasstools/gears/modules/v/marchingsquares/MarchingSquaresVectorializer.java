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
package org.jgrasstools.gears.modules.v.marchingsquares;

import static java.lang.Math.abs;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.COLS;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.ROWS;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.XRES;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.YRES;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.getRegionParamsFromGridCoverage;

import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

@Description("Module for raster to vector conversion")
@Author(name = "Andrea Antonello, Daniele Andreis", contact = "www.hydrologis.com")
@Keywords("Raster, Vector")
@Status(Status.DRAFT)
@Label(JGTConstants.VECTORPROCESSING)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
@SuppressWarnings("nls")
public class MarchingSquaresVectorializer extends JGTModel {

    @Description("The coverage that has to be converted.")
    @In
    public GridCoverage2D inGeodata;

    @Description("The value to use to trace the polygons. If it is null then all the value of the raster are used")
    @In
    public Double pValue = doubleNovalue;

    @Description("The value to use as a name for the raster value in the Feature.")
    @In
    public String defaultFeatureField = "value";

    @Description("A threshold on cell number to filter away polygons with cells less than that.")
    @In
    public double pThres = 0;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The extracted features.")
    @Out
    public SimpleFeatureCollection outGeodata = null;

    @Description("The extracted polygons in the image space.")
    @Out
    public List<java.awt.Polygon> awtGeometriesList;

    private RandomIter iter = null;

    private double xRes;

    private double yRes;

    private GridGeometry2D gridGeometry;

    private int height;

    private int width;

    private BitSet bitSet = null;

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outGeodata == null, doReset)) {
            return;
        }
        if (iter == null) {
            RenderedImage inputRI = inGeodata.getRenderedImage();
            iter = RandomIterFactory.create(inputRI, null);

            HashMap<String, Double> regionMap = getRegionParamsFromGridCoverage(inGeodata);
            height = regionMap.get(ROWS).intValue();
            width = regionMap.get(COLS).intValue();
            xRes = regionMap.get(XRES);
            yRes = regionMap.get(YRES);
            crs = inGeodata.getCoordinateReferenceSystem();

            bitSet = new BitSet(width * height);

            gridGeometry = inGeodata.getGridGeometry();
        }

        List<Polygon> geometriesList = new ArrayList<Polygon>();
        awtGeometriesList = new ArrayList<java.awt.Polygon>();

        pm.beginTask("Extracting vectors...", height);
        /*
         * a List where to store the different value of the raster if pValue is
         * null, otherwise only pValue is putted into.
         */
        ArrayList<Double> valueRaster = new ArrayList<Double>();
        /*
         * if pValue is a number then extract the polygon from the raster (if
         * the pixel value is equals to pValue).
         */
        if (pValue != null) {
            valueRaster.add(0, pValue);
            for( int row = 0; row < height; row++ ) {
                for( int col = 0; col < width; col++ ) {
                    double value = iter.getSampleDouble(col, row, 0);
                    if (!isNovalue(value) && !bitSet.get(row * width + col)) {

                        if (abs(value - pValue) < .0000001) {
                            java.awt.Polygon awtPolygon = new java.awt.Polygon();
                            Polygon polygon = identifyPerimeter(col, row, awtPolygon);
                            if (polygon != null) {
                                geometriesList.add(polygon);
                                awtGeometriesList.add(awtPolygon);
                            }
                        }
                    }
                }
                pm.worked(1);
            }
        } else {
            /*
             * 
             */
            pValue = doubleNovalue;
            for( int row = 0; row < height; row++ ) {
                for( int col = 0; col < width; col++ ) {
                    double value = iter.getSampleDouble(col, row, 0);
                    if (value != pValue) {
                        pValue = value;

                        if (!isNovalue(value) && !bitSet.get(row * width + col)) {

                            if (value == pValue) {
                                java.awt.Polygon awtPolygon = new java.awt.Polygon();
                                Polygon polygon = identifyPerimeter(col, row, awtPolygon);
                                if (polygon != null) {
                                    valueRaster.add(pValue);
                                    geometriesList.add(polygon);
                                    awtGeometriesList.add(awtPolygon);
                                }
                            }
                        }
                    }
                }
                pm.worked(1);
            }

        }

        pm.done();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("raster2vector");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("cat", Integer.class);
        b.add(defaultFeatureField, Double.class);
        SimpleFeatureType type = b.buildFeatureType();

        outGeodata = FeatureCollections.newCollection();
        int index = 0;
        for( Polygon polygon : geometriesList ) {
            Double tmpValue = valueRaster.get(index);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Object[] values = new Object[]{polygon, index, tmpValue};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + index);
            index++;
            outGeodata.add(feature);
        }

    }

    private Polygon identifyPerimeter( int initialX, int initialY, java.awt.Polygon awtPolygon ) throws TransformException {
        if (initialX < 0 || initialX > width - 1 || initialY < 0 || initialY > height - 1)
            throw new IllegalArgumentException("Coordinate outside the bounds.");

        int initialValue = value(initialX, initialY);
        if (initialValue == 0) {
            throw new IllegalArgumentException(String.format("Supplied initial coordinates (%d, %d) do not lie on a perimeter.",
                    initialX, initialY));
        }
        final Point2D worldPosition = new Point2D.Double(initialX, initialY);
        gridGeometry.getGridToCRS2D().transform(worldPosition, worldPosition);
        if (initialValue == 15) {
            // not a border pixel
            return null;
        }

        Coordinate startCoordinate = new Coordinate(worldPosition.getX() + xRes / 2.0, worldPosition.getY() - yRes / 2.0);
        List<Coordinate> coordinateList = new ArrayList<Coordinate>(200);
        coordinateList.add(startCoordinate);

        double currentX = startCoordinate.x;
        double currentY = startCoordinate.y;
        int x = initialX;
        int y = initialY;
        awtPolygon.addPoint(x, y);

        boolean previousWentNorth = false;
        boolean previousWentEast = false;

        do {
            Coordinate direction = null;
            int dx = 0;
            int dy = 0;
            int v = value(x, y);
            switch( v ) {
            case 1:
                dy = -1; // N
                currentY = currentY + yRes;
                previousWentNorth = true;
                break;
            case 2:
                dx = +1; // E
                currentX = currentX + xRes;
                previousWentEast = true;
                break;
            case 3:
                dx = +1; // E
                currentX = currentX + xRes;
                previousWentEast = true;
                break;
            case 4:
                dx = -1; // W
                currentX = currentX - xRes;
                previousWentEast = false;
                break;
            case 5:
                dy = -1; // N
                currentY = currentY + yRes;
                previousWentNorth = true;
                break;
            case 6:
                if (!previousWentNorth) {// W
                    dx = -1;
                    currentX = currentX - xRes;
                    previousWentEast = false;
                } else {
                    dx = +1;// E
                    currentX = currentX + xRes;
                    previousWentEast = true;
                }
                break;
            case 7:
                dx = +1; // E
                currentX = currentX + xRes;
                previousWentEast = true;
                break;
            case 8:
                dy = +1; // S
                currentY = currentY - yRes;
                previousWentNorth = false;
                break;
            case 9:
                if (previousWentEast) {
                    dy = -1; // N
                    currentY = currentY + yRes;
                    previousWentNorth = true;
                } else {
                    dy = +1; // S
                    currentY = currentY - yRes;
                    previousWentNorth = false;
                }
                break;
            case 10:
                dy = +1; // S
                currentY = currentY - yRes;
                previousWentNorth = false;
                break;
            case 11:
                dy = +1; // S
                currentY = currentY - yRes;
                previousWentNorth = false;
                break;
            case 12:
                dx = -1; // W
                currentX = currentX - xRes;
                previousWentEast = false;
                break;
            case 13:
                dy = -1; // N
                currentY = currentY + yRes;
                previousWentNorth = true;
                break;
            case 14:
                dx = -1; // W
                currentX = currentX - xRes;
                previousWentEast = false;
                break;
            default:
                throw new IllegalStateException("Illegat state: " + v);
            }
            direction = new Coordinate(currentX, currentY);
            coordinateList.add(direction);
            x = x + dx;
            y = y + dy;

            awtPolygon.addPoint(x, y);
        } while( x != initialX || y != initialY );

        double polygonArea = GeometryUtilities.getPolygonArea(awtPolygon.xpoints, awtPolygon.ypoints, coordinateList.size() - 1);
        if (polygonArea < pThres) {
            return null;
        }

        GeometryFactory gf = GeometryUtilities.gf();

        coordinateList.add(startCoordinate);

        Coordinate[] coordinateArray = (Coordinate[]) coordinateList.toArray(new Coordinate[coordinateList.size()]);
        LinearRing linearRing = gf.createLinearRing(coordinateArray);
        Polygon polygon = gf.createPolygon(linearRing, null);
        return polygon;
    }

    private int value( int x, int y ) {
        int sum = 0;
        if (isSet(x, y)) // UL
            sum |= 1;
        if (isSet(x + 1, y)) // UR
            sum |= 2;
        if (isSet(x, y + 1)) // LL
            sum |= 4;
        if (isSet(x + 1, y + 1)) // LR
            sum |= 8;

        if (sum == 0) {
            System.out.println(x + "/" + y);
        }
        // mark the used position
        bitSet.set(y * width + x);
        return sum;
    }

    private boolean isSet( int x, int y ) {
        boolean isOutsideGrid = x < 0 || x >= width || y < 0 || y >= height;
        if (isOutsideGrid) {
            return false;
        }
        double value = iter.getSampleDouble(x, y, 0);
        if (isNovalue(value)) {
            return false;
        }

        if (value == pValue) {
            return true;
        }
        return false;
    }

}
