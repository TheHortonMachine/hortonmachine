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
package org.hortonmachine.gears.modules.v.marchingsquares;

import static java.lang.Math.abs;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_DEFAULT_FEATURE_FIELD_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_IN_GEODATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_OUT_GEODATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_P_THRES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_P_VALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMARCHINGSQUARESVECTORIALIZER_UI;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.COLS;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.ROWS;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.XRES;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.YRES;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.getRegionParamsFromGridCoverage;

import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

@Description(OMSMARCHINGSQUARESVECTORIALIZER_DESCRIPTION)
@Documentation(OMSMARCHINGSQUARESVECTORIALIZER_DOCUMENTATION)
@Author(name = OMSMARCHINGSQUARESVECTORIALIZER_AUTHORNAMES, contact = OMSMARCHINGSQUARESVECTORIALIZER_AUTHORCONTACTS)
@Keywords(OMSMARCHINGSQUARESVECTORIALIZER_KEYWORDS)
@Label(OMSMARCHINGSQUARESVECTORIALIZER_LABEL)
@Name(OMSMARCHINGSQUARESVECTORIALIZER_NAME)
@Status(OMSMARCHINGSQUARESVECTORIALIZER_STATUS)
@License(OMSMARCHINGSQUARESVECTORIALIZER_LICENSE)
@UI(OMSMARCHINGSQUARESVECTORIALIZER_UI)
public class OmsMarchingSquaresVectorializer extends HMModel {

    @Description(OMSMARCHINGSQUARESVECTORIALIZER_IN_GEODATA_DESCRIPTION)
    @In
    public GridCoverage2D inGeodata;

    @Description(OMSMARCHINGSQUARESVECTORIALIZER_P_VALUE_DESCRIPTION)
    @In
    public Double pValue = doubleNovalue;

    @Description(OMSMARCHINGSQUARESVECTORIALIZER_DEFAULT_FEATURE_FIELD_DESCRIPTION)
    @In
    public String defaultFeatureField = "value";

    @Description(OMSMARCHINGSQUARESVECTORIALIZER_P_THRES_DESCRIPTION)
    @In
    public double pThres = 0;

    @Description(OMSMARCHINGSQUARESVECTORIALIZER_OUT_GEODATA_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outGeodata = null;

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

        outGeodata = new DefaultFeatureCollection();
        int index = 0;
        for( Polygon polygon : geometriesList ) {
            Double tmpValue = valueRaster.get(index);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Object[] values = new Object[]{polygon, index, tmpValue};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + index);
            index++;
            ((DefaultFeatureCollection) outGeodata).add(feature);
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
