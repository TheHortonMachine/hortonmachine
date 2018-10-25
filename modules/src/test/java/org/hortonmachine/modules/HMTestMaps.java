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
package org.hortonmachine.modules;


import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
/**
 * The grass map named test and its values to be used in tests.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HMTestMaps {
    private static final double NaN = -9999.0;
    public static final RegionMap envelopeParams = new RegionMap();

    public static CoordinateReferenceSystem crs = null;

    public static DefaultFeatureCollection testFC;
    public static DefaultFeatureCollection testLeftFC;

    public static Coordinate westNorth;
    public static Coordinate eastNorth;
    public static Coordinate eastSouth;
    public static Coordinate centerCoord;

    static {
        double n = 5140020.0;
        double s = 5139780.0;
        double w = 1640650.0;
        double e = 1640950.0;
        envelopeParams.put(CoverageUtilities.NORTH, n);
        envelopeParams.put(CoverageUtilities.SOUTH, s);
        envelopeParams.put(CoverageUtilities.WEST, w);
        envelopeParams.put(CoverageUtilities.EAST, e);
        envelopeParams.put(CoverageUtilities.XRES, 30.0);
        envelopeParams.put(CoverageUtilities.YRES, 30.0);
        envelopeParams.put(CoverageUtilities.ROWS, 8.0);
        envelopeParams.put(CoverageUtilities.COLS, 10.0);

        try {
            crs = CrsUtilities.getCrsFromEpsg("EPSG:32632");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("cat", Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        GeometryFactory gf = GeometryUtilities.gf();

        westNorth = new Coordinate(w, n);
        eastNorth = new Coordinate(e, n);
        eastSouth = new Coordinate(e, s);
        centerCoord = new Coordinate(w + (e - w) / 2, s + (n - s) / 2);

        Object[] nw = new Object[]{gf.createPoint(westNorth), 1};
        builder.addAll(nw);
        SimpleFeature nwFeature = builder.buildFeature(type.getTypeName() + ".1");

        Object[] se = new Object[]{gf.createPoint(eastSouth), 2};
        builder.addAll(se);
        SimpleFeature seFeature = builder.buildFeature(type.getTypeName() + ".2");

        Object[] center = new Object[]{gf.createPoint(centerCoord), 3};
        builder.addAll(center);
        SimpleFeature centerFeature = builder.buildFeature(type.getTypeName() + ".3");

        testFC = new DefaultFeatureCollection();
        testFC.add(nwFeature);
        testFC.add(seFeature);
        testFC.add(centerFeature);

        b = new SimpleFeatureTypeBuilder();
        b.setName("testlefthalf");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        type = b.buildFeatureType();
        builder = new SimpleFeatureBuilder(type);

        LinearRing linearRing = gf.createLinearRing(new Coordinate[]{//
                westNorth, //
                        new Coordinate(centerCoord.x, westNorth.y), //
                        new Coordinate(centerCoord.x, eastSouth.y), //
                        new Coordinate(westNorth.x, eastSouth.y), //
                        westNorth //
                });
        Object[] leftPolygon = new Object[]{gf.createPolygon(linearRing, null)};
        builder.addAll(leftPolygon);
        SimpleFeature leftPolygonFeature = builder.buildFeature(null);

        testLeftFC = new DefaultFeatureCollection();
        testLeftFC.add(leftPolygonFeature);

    }

    /**
     * The DEM which is the start point of the test.(N.B. in the first test it's read from the DataBase inside this plug-in).
     */
    public static double[][] mapData = new double[][]{//
    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
            {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
            {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
            {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
            {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
            {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};

    public static double[][] mapDataHalf = new double[][]{//
    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
            {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
            {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
            {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
            {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
            {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};

    public static double[][] mapData4326 = new double[][]{//
    {NaN, 800.0, 900.0, 1000.0, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 600.0, NaN, 750.0, 860.0, 1250.0, 1300.0, 1350.0, 1450.0, 1500.0}, //
            {NaN, 500.0, 700.0, 750.0, 800.0, 850.0, 1000.0, 1200.0, 1250.0, 1500.0}, //
            {450.0, 550.0, 650.0, 700.0, 750.0, 800.0, 850.0, 1000.0, 1100.0, NaN}, //
            {500.0, 600.0, 700.0, 500.0, 600.0, 700.0, 800.0, 450.0, 1500.0, NaN}, //
            {600.0, 700.0, 750.0, 800.0, 760.0, 850.0, 1000.0, 1150.0, 1500.0, NaN}, //
            {800.0, 910.0, 980.0, 1150.0, 1200.0, 1000.0, 1100.0, 1250.0, 1500.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, 1300.0, 1450.0, 1500.0, NaN}};

    public static double[][] outPitData = new double[][]{//
    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, 500, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
            {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
            {400, 410, 650, 700, 750, 800, 850, 800, 800, 1500}, //
            {450, 550, 430, 500, 600, 700, 800, 800, 800, 1500}, //
            {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
            {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}

    };

    public static double[][] pitData = new double[][]{//
    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
            {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
            {400, 410, 650, 700, 750, 800, 850, 800, 800, 1500}, //
            {450, 550, 430, 500, 600, 700, 800, 800, 800, 1500}, //
            {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
            {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};

    public static double[][] flowData = new double[][]{//
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 6, 6, 6, 6, 6, 6, 6, NaN}, //
            {NaN, 7, 6, 6, 6, 6, 6, 7, 7, NaN}, //
            {NaN, 5, 5, 7, 6, 6, 6, 6, 5, NaN}, //
            {NaN, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
            {NaN, 2, 3, 3, 4, 4, 4, 3, 3, NaN}, //
            {NaN, 4, 4, 4, 4, 4, 5, 4, 4, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] drainData0 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 6, 6, 6, 6, 6, 6, 6, NaN}, //
            {NaN, 7, 6, 6, 6, 6, 6, 7, 7, NaN}, //
            {NaN, 10, 5, 7, 6, 6, 6, 6, 5, NaN}, //
            {NaN, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
            {NaN, 2, 3, 3, 4, 4, 4, 3, 3, NaN}, //
            {NaN, 4, 4, 4, 4, 4, 5, 4, 4, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] drainData1 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 6, 6, 6, 6, 6, 6, 6, NaN}, //
            {NaN, 7, 6, 6, 6, 6, 6, 7, 7, NaN}, //
            {NaN, 10, 5, 7, 6, 6, 6, 6, 5, NaN}, //
            {NaN, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
            {NaN, 2, 3, 3, 3, 4, 4, 3, 3, NaN}, //
            {NaN, 3, 4, 4, 4, 4, 5, 4, 4, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] mflowData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 6, 6, 6, 6, 6, 6, 6, NaN}, //
            {NaN, 7, 6, 6, 6, 6, 6, 7, 7, NaN}, //
            {NaN, 10, 5, 7, 6, 6, 6, 6, 5, NaN}, //
            {NaN, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
            {NaN, 2, 3, 3, 4, 4, 4, 3, 3, NaN}, //
            {NaN, 10, 4, 4, 4, 4, 5, 4, 4, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] mflowDataBorder = new double[][]{//
    {5, 6, 8, 8, 9, 6, 5, 8, 5, 5}, //
            {3, NaN, 6, 6, 6, 6, 6, 6, 6, 3}, //
            {3, 7, 6, 6, 6, 6, 6, 7, 7, 3}, //
            {4, 10, 5, 7, 6, 6, 6, 6, 5, 2}, //
            {3, 3, 4, 5, 5, 5, 5, 5, 5, 3}, //
            {3, 2, 3, 3, 4, 4, 4, 3, 3, 5}, //
            {5, 10, 4, 4, 4, 4, 5, 4, 4, 3}, //
            {3, 3, 7, 3, 3, 8, 3, 9, 3, 3}};

    public static double[][] slopeData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 4.71, 3.54, 2.59, 2.36, 3.54, 7.07, 5.89, NaN}, //
            {NaN, 4.67, 6.84, 2.36, 2.36, 2.36, 2.36, 6.67, 10, NaN}, //
            {NaN, 0.33, 8, 6.67, 5.89, 4.71, 3.54, 0, 0, NaN}, //
            {NaN, 4.67, 0.47, 2.33, 3.33, 3.33, 3.33, 0, 0, NaN}, //
            {NaN, 4.01, 9, 8.33, 6.13, 4.01, 3.54, 6.67, 11.67, NaN}, //
            {NaN, 4.71, 3.54, 2.36, 0.71, 0.71, 7, 5.89, 5.89, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] tcaData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 2, 2, 2, 2, 2, 2, 2, 1, NaN}, //
            {NaN, 46, 3, 3, 3, 3, 1, 5, 2, NaN}, //
            {NaN, 1, 37, 32, 20, 15, 11, 5, 2, NaN}, //
            {NaN, 2, 2, 2, 3, 1, 2, 2, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 2, 1, 1, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] mtcaData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 2, 2, 2, 2, 2, 2, 2, 1, NaN}, //
            {NaN, 47, 3, 3, 3, 3, 1, 5, 2, NaN}, //
            {NaN, 1, 38, 32, 23, 15, 11, 5, 2, NaN}, //
            {NaN, 3, 2, 2, 3, 1, 2, 2, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 2, 1, 1, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] tca3DData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 5438.94, 5370.95, 5963.77, 6552.17, 7082.91, 6568.44, 7191.73, NaN}, //
            {NaN, 10320.83, 9568.38, 8653.17, 8973.7, 9682.18, 10800.86, 13431.92, 10391.83, NaN}, //
            {NaN, 257338.23, 14529.45, 13231.44, 13420.35, 13928.84, 3096.91, 39495.19, 22327, NaN}, //
            {NaN, 5382.74, 212067.61, 185732.11, 123898.49, 101011.35, 82961.85, 40031.45, 22377.36, NaN}, //
            {NaN, 9234.82, 9916.27, 10455.59, 17929.35, 3652.57, 11112.19, 13567.34, 10219.8, NaN}, //
            {NaN, 5384.03, 4828.31, 4947.56, 5864.58, 14606.37, 7294.38, 6426.99, 7298.45, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] gradientData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 4.55, 6.72, 7.06, 8.33, 7.17, 7.68, NaN}, //
            {NaN, NaN, 3.73, 3, 2.48, 2.36, 3.54, 7.45, 11.21, NaN}, //
            {NaN, 4.17, 6.6, 4.49, 3.73, 3, 1.67, 3.44, 12.69, NaN}, //
            {NaN, 3.18, 1.18, 2.95, 3.34, 3.37, 1.67, 3.33, 13.04, NaN}, //
            {NaN, 4.17, 5.89, 5.1, 3.02, 2.12, 5.08, 7.07, 11.21, NaN}, //
            {NaN, 5.74, 4.96, 4.21, 6.5, 8.05, 8.43, 6.51, 8.33, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] aspectDataDegrees = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 254.0, 242.0, 220.0, 247.0, 259.0, 257.0, 259.0, NaN}, //
            {NaN, 254.0, 256.0, 241.0, 242.0, 243.0, 250.0, 253.0, 263.0, NaN}, //
            {NaN, 270.0, 259.0, 240.0, 240.0, 241.0, 180.0, 139.0, 265.0, NaN}, //
            {NaN, 19.0, 52.0, 283.0, 273.0, 262.0, 270.0, 360.0, 275.0, NaN}, //
            {NaN, 286.0, 291.0, 314.0, 341.0, 299.0, 284.0, 281.0, 277.0, NaN}, //
            {NaN, 291.0, 300.0, 333.0, 10.0, 285.0, 281.0, 283.0, 278.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] aspectDataRadiants = new double[][]{//
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 4.433, 4.224, 3.84, 4.311, 4.52, 4.486, 4.52, NaN}, //
            {NaN, 4.433136300065597, 4.468042885105484, 4.2062434973063345, 4.223696789826278, 4.241150082346221,
                    4.363323129985824, 4.4156830075456535, 4.590215932745086, NaN}, //
            {NaN, 4.71238898038469, 4.520402762665314, 4.1887902047863905, 4.1887902047863905, 4.2062434973063345,
                    3.141592653589793, 2.426007660272118, 4.625122517784973, NaN}, //
            {NaN, 0.33161255787892263, 0.9075712110370513, 4.939281783143953, 4.76474885794452, 4.572762640225143,
                    4.71238898038469, 6.283185307179586, 4.799655442984406, NaN}, //
            {NaN, 4.991641660703783, 5.078908123303499, 5.480333851262195, 5.951572749300664, 5.218534463463045,
                    4.956735075663896, 4.9043751981040655, 4.834562028024293, NaN}, //
            {NaN, 5.078908123303499, 5.235987755982989, 5.811946409141117, 0.17453292519943295, 4.974188368183839,
                    4.9043751981040655, 4.939281783143953, 4.852015320544236, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] tanData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, -0.0204365, 0.0040265, 0.0134249, 0.0195928, -0.0160252, 0.0319721, NaN}, //
            {NaN, NaN, NaN, 0.0045884, 0.0070564, -0.0043396, -0.0075602, -0.0088648, 0.0148889, NaN}, //
            {NaN, 0.0726049, -0.0401959, -0.0020833, 0.0000000, 0.0026991, -0.0571662, 0.0255704, 0.0347564, NaN}, //
            {NaN, -0.0736340, 0.2192600, 0.1503699, 0.0992518, 0.0523111, 0.0571662, 0.0000000, 0.0390698, NaN}, //
            {NaN, -0.0022819, -0.0203546, -0.0069907, 0.0034304, 0.0023689, 0.0075309, -0.0175035, 0.0025633, NaN}, //
            {NaN, -0.0029989, 0.0065292, -0.0188262, 0.0054898, 0.0325153, 0.0000358, 0.0160437, 0.0227700, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] planData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, -0.0209238, 0.0040709, 0.0135588, 0.0197333, -0.0161804, 0.0322418, NaN}, //
            {NaN, NaN, NaN, 0.0048358, 0.0076094, -0.0047140, -0.0078567, -0.0089443, 0.0149480, NaN}, //
            {NaN, 0.0746667, -0.0406542, -0.0021344, 0.0000000, 0.0028446, -0.0666667, 0.0266314, 0.0348641, NaN}, //
            {NaN, -0.0771799, 0.2875568, 0.1587560, 0.1036112, 0.0545648, 0.0666667, 0.0000000, 0.0391844, NaN}, //
            {NaN, -0.0023467, -0.0206459, -0.0071239, 0.0036137, 0.0026189, 0.0076754, -0.0176777, 0.0025735, NaN}, //
            {NaN, -0.0030441, 0.0066608, -0.0193493, 0.0055543, 0.0327652, 0.0000360, 0.0162320, 0.0229333, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] profData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 0.0005018, 0.0010118, 0.0008380, 0.0002856, -0.0002802, 0.0000646, NaN}, //
            {NaN, NaN, NaN, 0.0012920, -0.0004059, 0.0006620, 0.0028001, 0.0001567, -0.0000006, NaN}, //
            {NaN, 0.0032482, -0.0004414, -0.0016163, -0.0019340, -0.0020187, 0.0000000, 0.0040650, 0.0003239, NaN}, //
            {NaN, -0.0026547, 0.1128483, 0.0021268, -0.0000321, 0.0001144, -0.0151322, 0.0052724, 0.0002928, NaN}, //
            {NaN, 0.0008304, -0.0008365, -0.0016418, -0.0051775, 0.0012921, 0.0010801, 0.0000381, -0.0000592, NaN}, //
            {NaN, 0.0004254, 0.0012926, 0.0021125, 0.0013562, 0.0007339, -0.0000186, 0.0002137, 0.0000526, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] nablaData1 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0.055, 0.083, 0.114, 0.16, 0.125, -0.015, 0.09, NaN}, //
            {NaN, 0.055, -0.23, 0.003, 0.014, 0.017, 0.028, -0.028, 0.16, NaN}, //
            {NaN, 0.12, -0.069, -0.075, -0.0555, -0.027, -0.014, 0.111, 0.444, NaN}, //
            {NaN, 0.014, 0.205, 0.238, 0.172, 0.114, 0.047, 0.125, 0.444, NaN}, //
            {NaN, -0.047, -0.069, -0.122, -0.069, 0.027, -0.0027, -0.042, 0.083, NaN}, //
            {NaN, 0.049, 0.0725, 0.108, 0.167, 0.23, 0.075, 0.083, 0.083, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] nablaData0 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, NaN}, //
            {NaN, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, NaN}, //
            {NaN, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, NaN}, //
            {NaN, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, NaN}, //
            {NaN, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, NaN}, //
            {NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] cp9Data = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 40, 10, 10, 10, 10, 70, NaN}, //
            {NaN, NaN, NaN, 10, 10, 10, 30, 10, 10, NaN}, //
            {NaN, 90, 40, 10, 20, 20, 40, 90, 70, NaN}, //
            {NaN, 50, 90, 90, 70, 70, 80, 30, 70, NaN}, //
            {NaN, 10, 40, 10, 20, 10, 10, 10, 10, NaN}, //
            {NaN, 10, 10, 30, 10, 70, 10, 10, 70, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] cp3Data = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 35, 25, 25, 25, 25, 15, NaN}, //
            {NaN, NaN, NaN, 25, 25, 25, 15, 25, 25, NaN}, //
            {NaN, 15, 35, 25, 35, 35, 35, 15, 15, NaN}, //
            {NaN, 35, 15, 15, 15, 15, 35, 15, 15, NaN}, //
            {NaN, 25, 35, 25, 35, 25, 25, 25, 25, NaN}, //
            {NaN, 25, 25, 15, 25, 15, 25, 25, 15, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] multiTcaData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, 2.25, 2.125, 2.0, 2.0, 2.0, 1.0, NaN}, //
            {NaN, 33.87, 3.25, 3.47, 3.45, 3.14, 1.0, 5.14, 2.0, NaN}, //
            {NaN, 1.82, 31.5, 26.14, 15.13, 8.35, 7.43, 5.14, 2.0, NaN}, //
            {NaN, 2.28, 2.0, 2.06, 3.0, 2.0, 2.0, 2.0, 1.0, NaN}, //
            {NaN, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] abData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 18.11, 34.15, 50.22, 72.43, 19.99, 300.0, NaN}, //
            {NaN, NaN, NaN, 70.12, 77.578, 52.52, 48.44, 47.16, 53.93, NaN}, //
            {NaN, 13800.0, 47.37, 84.56, 90.0, 98.36, 15.78, 725.23, 600.0, NaN}, //
            {NaN, 15.79, 11100.0, 9600.0, 6000.0, 4500.0, 3300.0, 150.0, 600.0, NaN}, //
            {NaN, 56.04, 36.44, 49.34, 100.89, 32.55, 77.77, 38.74, 32.50, NaN}, //
            {NaN, 27.48, 37.43, 18.71, 35.96, 600.0, 30.03, 57.89, 94.19, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] bData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 49.67, 26.35, 17.92, 12.43, 45.01, 3.0, NaN}, //
            {NaN, NaN, NaN, 25.67, 23.2, 34.27, 37.16, 38.16, 16.69, NaN}, //
            {NaN, 3, 57, 31.93, 30.0, 27.45, 57, 6.2, 3, NaN}, //
            {NaN, 57, 3, 3, 3, 3, 3, 30.0, 3, NaN}, //
            {NaN, 32.12, 49.39, 36.48, 26.76, 27.65, 23.14, 46.47, 27.69, NaN}, //
            {NaN, 32.75, 24.05, 48.11, 25.03, 3.0, 29.97, 15.55, 9.56, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] diametersData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 42.43, 42.43, 42.43, 42.43, 42.43, 42.43, 42.43, 0, NaN}, //
            {NaN, 228.47, 84.85, 84.85, 84.85, 84.85, 0, 67.08, 30, NaN}, //
            {NaN, 0, 201.25, 174.93, 150, 127.28, 108.17, 67.08, 30, NaN}, //
            {NaN, 42.43, 42.43, 42.43, 67.08, 0, 42.43, 42.43, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 30, 0, 0, 0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };
    public static double[][] distEuclideaData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 152.97, 161.55, 174.93, 192.09, 212.13, 234.31, 258.07, NaN}, //
            {NaN, 120, 123.69, 134.16, 150, 169.71, 192.09, 216.33, 241.87, NaN}, //
            {NaN, 90, 94.87, 108.17, 127.28, 150, 174.93, 201.25, 228.47, NaN}, //
            {NaN, 60, 67.08, 84.85, 108.17, 134.16, 161.55, 189.74, 218.4, NaN}, //
            {NaN, 30, 42.43, 67.08, 94.87, 123.69, 152.97, 182.48, 212.13, NaN}, //
            {NaN, 0, 30, 60, 90, 120, 150, 180, 210, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };
    public static double[][] meandropData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 100, 75, 55, 50, 75, 150, 125, 0, NaN}, //
            {NaN, 404.13, 103.33, 100, 116.67, 166.67, 0, 190, 150, NaN}, //
            {NaN, 0, 418.11, 378.13, 326, 250, 177.27, 200, 175, NaN}, //
            {NaN, 75, 50, 15, 90, 0, 125, 125, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 105, 0, 0, 0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] extractNet0Data = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, 2, NaN, NaN}, //
            {NaN, NaN, 2, 2, 2, 2, 2, 2, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] extractNet1Data = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 2, 2, NaN}, //
            {NaN, 2, 2, 2, 2, 2, NaN, 2, 2, NaN}, //
            {NaN, NaN, 2, 2, 2, 2, 2, 2, 2, NaN}, //
            {NaN, NaN, 2, 2, 2, NaN, NaN, 2, 2, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] marchingSq1 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 2, 2, NaN}, //
            {NaN, 2, 2, 2, 2, 2, NaN, 2, 2, NaN}, //
            {NaN, NaN, 2, 2, 2, 2, 2, 2, 2, NaN}, //
            {NaN, NaN, 2, 2, 2, NaN, NaN, 2, 2, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] marchingSq2 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 2, 2, NaN}, //
            {NaN, NaN, NaN, 2, NaN, NaN, NaN, 2, 2, NaN}, //
            {NaN, NaN, NaN, NaN, 2, NaN, 2, 2, 2, NaN}, //
            {NaN, NaN, 2, 2, NaN, NaN, NaN, 2, 2, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };
    public static double[][] marchingSq3 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, 2, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, 2, 2, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, 2, 2, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] marchingSq4 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, 2, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, 2, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] marchingSq5 = new double[][]{//
    {2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] marchingSq6 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] marchingSq7 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, 1, 1, 1, 2, 2, NaN}, //
            {NaN, NaN, 2, 2, 1, 1, 1, 2, 2, NaN}, //
            {NaN, NaN, 5, 5, 3, 3, 4, 4, NaN, NaN}, //
            {NaN, NaN, 5, 5, 3, 3, 4, 4, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] d2oPixelData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, 3, 5, 5, 6, 8, NaN}, //
            {NaN, 1, 1, 2, 4, 4, 5, 7, 8, NaN}, //
            {NaN, 0, 1, 3, 3, 4, 5, 6, 7, NaN}, //
            {NaN, 1, 1, 2, 3, 4, 5, 6, 7, NaN}, //
            {NaN, 2, 2, 3, 3, 4, 5, 7, 8, NaN}, //
            {NaN, 0, 3, 3, 4, 4, 5, 6, 8, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] hacklengthData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 42.43, 42.43, 42.43, 42.43, 42.43, 42.43, 42.43, 0, NaN}, //
            {NaN, 264.85, 84.85, 84.85, 84.85, 84.85, 0, 72.43, 30, NaN}, //
            {NaN, 0, 222.43, 192.43, 162.43, 132.43, 102.43, 72.43, 30, NaN}, //
            {NaN, 42.43, 42.43, 42.43, 72.43, 0, 42.43, 42.43, 0, NaN}, //
            {NaN, NaN, 0, 0, 0, 30, 0, 0, 0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] hacklength3DData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 204.45, 155.88, 117.9, 108.63, 155.88, 302.99, 253.57, 0, NaN}, //
            {NaN, 848.83, 226.53, 217.26, 264.51, 411.61, 0, 332.99, 302.99, NaN}, //
            {NaN, 0, 801.93, 725.77, 621.37, 516.97, 412.56, 382.56, 352.56, NaN}, //
            {NaN, 155.88, 108.63, 51.96, 264.09, 0, 253.57, 253.57, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 212.13, 0, 0, 0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] hackstream = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 2, 3, NaN}, //
            {NaN, 1, 2, 2, 2, 2, NaN, 2, 3, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 2, NaN}, //
            {NaN, NaN, 2, 2, 2, NaN, NaN, 1, 2, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] magnitudoData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 18, 1, 1, 1, 1, 1, 2, 1, NaN}, //
            {NaN, 1, 14, 12, 8, 6, 4, 2, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] netNumberingChannelDataJG = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 2.0, 1.0, 3.0, 5.0, 6.0, NaN, 8.0, NaN, NaN}, //
            {NaN, NaN, 4.0, 4.0, 7.0, 8.0, 8.0, 9.0, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, 10.0, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] netNumberingChannelDataNN0 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1.0, NaN, NaN, NaN, NaN, 3.0, 9.0, NaN}, //
            {NaN, 2.0, 10.0, 11.0, 12.0, 13.0, NaN, 4.0, 9.0, NaN}, //
            {NaN, NaN, 8.0, 7.0, 6.0, 5.0, 5.0, 18.0, 19.0, NaN}, //
            {NaN, NaN, 14.0, 15.0, 16.0, NaN, NaN, 17.0, 19.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] basinDataNN0 = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2.0, 1.0, 10.0, 11.0, 12.0, 13.0, 3.0, NaN}, //
            {NaN, 2.0, 1.0, 10.0, 11.0, 12.0, 13.0, 3.0, 9.0, NaN}, //
            {NaN, 2.0, 10.0, 11.0, 12.0, 13.0, 5.0, 4.0, 9.0, NaN}, //
            {NaN, 2.0, 8.0, 7.0, 6.0, 5.0, 5.0, 18.0, 19.0, NaN}, //
            {NaN, 8.0, 14.0, 15.0, 16.0, 6.0, 5.0, 17.0, 19.0, NaN}, //
            {NaN, NaN, 8.0, 14.0, 15.0, 16.0, 16.0, 5.0, 17.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] netNumberingChannelData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 7, 7, NaN}, //
            {NaN, 1, 2, 3, 4, 5, NaN, 6, 7, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 8, NaN}, //
            {NaN, NaN, 11, 10, 9, NaN, NaN, 8, 8, NaN}, //
            {NaN, 12, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };
    public static double[][] splitSubBasinDataJG = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2.0, 2.0, 1.0, 3.0, 5.0, 6.0, 8.0, NaN}, //
            {NaN, 2.0, 2.0, 1.0, 3.0, 5.0, 6.0, 8.0, 8.0, NaN}, //
            {NaN, 2.0, 1.0, 3.0, 5.0, 6.0, 8.0, 8.0, 8.0, NaN}, //
            {NaN, 2.0, 4.0, 4.0, 7.0, 8.0, 8.0, 9.0, 9.0, NaN}, //
            {NaN, 4.0, 4.0, 4.0, 10.0, 7.0, 8.0, 9.0, 9.0, NaN}, //
            {NaN, NaN, 4.0, 4.0, 4.0, 10.0, 10.0, 8.0, 9.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    /**
     * This isn't used. It maintain it only for future test.
     */
    public static double[][] splitSubBasinData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 2, 2, 3, 4, 7, 7, NaN}, //
            {NaN, 1, 2, 2, 3, 4, 5, 7, 7, NaN}, //
            {NaN, 1, 2, 3, 4, 5, 1, 6, 7, NaN}, //
            {NaN, 1, 1, 1, 1, 1, 1, 1, 8, NaN}, //
            {NaN, 1, 11, 10, 9, 1, 1, 8, 8, NaN}, //
            {NaN, 12, 1, 11, 10, 9, 9, 1, 8, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };

    public static double[][] strahlerData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1, NaN, NaN, NaN, NaN, 1, 1, NaN}, //
            {NaN, 3, 1, 1, 1, 1, NaN, 2, 1, NaN}, //
            {NaN, NaN, 3, 3, 3, 3, 3, 2, 1, NaN}, //
            {NaN, NaN, 1, 1, 1, NaN, NaN, 1, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };

    /**
     * It was obtained with the tcaData and strahlerData, it is to test the h.seol command.
     */
    public static double[][] soelData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 2, NaN, NaN}, //
            {NaN, 46, 3, 3, 3, 3, NaN, 5, 2, NaN}, //
            {NaN, NaN, 37, 32, 20, 15, 11, 5, 2, NaN}, //
            {NaN, NaN, 2, 2, 3, NaN, NaN, 2, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] cp3GCData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 45, 25, 25, 25, 55, 15, NaN}, //
            {NaN, NaN, 35, 25, 25, 25, 15, 35, 55, NaN}, //
            {NaN, 35, 55, 35, 35, 35, 45, 35, 35, NaN}, //
            {NaN, 45, 35, 35, 35, 35, 35, 35, 35, NaN}, //
            {NaN, 25, 55, 55, 35, 25, 25, 35, 55, NaN}, //
            {NaN, 35, 25, 15, 25, 15, 55, 25, 15, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] cp9GCData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 40, 10, 10, 10, 110, 70, NaN}, //
            {NaN, NaN, 100, 10, 10, 10, 30, 100, 110, NaN}, //
            {NaN, 100, 110, 100, 100, 100, 40, 100, 100, NaN}, //
            {NaN, 50, 100, 100, 100, 100, 100, 100, 100, NaN}, //
            {NaN, 10, 110, 110, 100, 10, 10, 100, 110, NaN}, //
            {NaN, 100, 10, 30, 10, 70, 110, 10, 70, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] topIndexData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, -1.55, -1.26, -0.95, -0.86, -1.26, -1.96, -1.77, NaN}, //
            {NaN, -0.85, -1.23, -0.17, -0.17, -0.17, -0.17, -1.2, -2.3, NaN}, //
            {NaN, 4.94, -0.98, -0.8, -0.67, -0.45, -1.26, NaN, NaN, NaN}, //
            {NaN, -1.54, 4.37, 2.62, 1.79, 1.51, 1.19, NaN, NaN, NaN}, //
            {NaN, -0.7, -1.5, -1.43, -0.71, -1.39, -0.57, -1.2, -2.46, NaN}, //
            {NaN, -1.55, -1.26, -0.86, 0.34, 1.04, -1.95, -1.77, -1.77, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] tauData = new double[][]{
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 54530914921.93, 103150213053.96, 162128596219.33, 346185997460.3, 98653540807.6, 3233363603907.54,
                    NaN}, //
            {NaN, NaN, NaN, 253015166252.08, 289517920014.01, 172103737229.23, 154512228813.26, 298033415868.6, 466863480034.18,
                    NaN}, //
            {NaN, 78030445128402.5, 338439086034.99, 649211988382.51, 649356040815.64, 629778708697.54, 45383102545.96, 0, 0, NaN}, //
            {NaN, 54634893768.73, 73888789711026.2, 177016982772739.0, 120018215583173.0, 81782839144181.3, 54083412287755.0, 0,
                    0, NaN}, //
            {NaN, 267204084896.77, 258037112439.66, 367094775560.6, 776581704858.18, 129493185601.08, 380628218732.5,
                    229286976257.17, 263415339238.22, NaN}, //
            {NaN, 115023236942.34, 143563692535.35, 43463269679.94, 46631689708.24, 1988184005077.19, 168611999625.77,
                    360548980496.92, 689973802894.4, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //

    };

    public static double[][] h2cdTopoData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 72.43, 42.43, 84.85, 84.85, 84.85, 84.85, 42.43, NaN}, //
            {NaN, 30, 0, 42.43, 42.43, 42.43, 42.43, 0, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 0, 42.43, 0, 0, NaN}, //
            {NaN, 30, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 42.43, 0, 0, 0, 42.43, 42.43, 0, 0, NaN}, //
            {NaN, 0, 84.85, 42.43, 42.43, 42.43, 72.43, 84.85, 42.43, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] h2cdData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 1, 2, 2, 2, 2, 1, NaN}, //
            {NaN, 1, 0, 1, 1, 1, 1, 0, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 0, 1, 0, 0, NaN}, //
            {NaN, 1, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 1, 0, 0, 0, 1, 1, 0, 0, NaN}, //
            {NaN, 0, 2, 1, 1, 1, 2, 2, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] h2cd3dData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 347.63, 155.88, 226.53, 217.26, 264.51, 411.61, 253.57, NaN}, //
            {NaN, 143.18, 0, 108.63, 108.63, 108.63, 108.63, 0, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 0, 155.88, 0, 0, NaN}, //
            {NaN, 143.18, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 175.21, 0, 0, 0, 175.21, 155.88, 0, 0, NaN}, //
            {NaN, 0, 331.1, 108.63, 51.96, 51.96, 264.09, 409.46, 253.57, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] ddData = new double[][]{//
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0.023570226039551584, NaN, NaN, NaN, NaN, 0.016666666666666666, 0.03333333333333333, NaN}, //
            {NaN, 0.017742957517564183, 0.011111111111111112, 0.011111111111111112, 0.015713484026367724, 0.015713484026367724,
                    NaN, 0.029428090415820635, 0.03333333333333333, NaN}, //
            {NaN, NaN, 0.018982943974653582, 0.019434223176554563, 0.021380711874576984, 0.023142696805273543,
                    0.028527919885979077, 0.02666666666666667, 0.03333333333333333, NaN}, //
            {NaN, NaN, 0.016666666666666666, 0.016666666666666666, 0.015713484026367724, NaN, NaN, 0.016666666666666666,
                    0.03333333333333333, NaN}, //
            {NaN, 0.0, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] rescaledDistanceData = {//
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 51.72792206135786, 55.45584412271571, 64.45584412271572, 136.88225099390857, 140.61017305526642,
                    170.61017305526642, 256.58073580374355, NaN}, //
            {NaN, 39.0, 42.72792206135786, 51.72792206135786, 124.15432893255071, 127.88225099390857, 157.88225099390857,
                    243.8528137423857, 252.8528137423857, NaN}, //
            {NaN, 30.0, 39.0, 111.42640687119285, 115.15432893255071, 145.1543289325507, 175.1543289325507, 234.8528137423857,
                    243.8528137423857, NaN}, //
            {NaN, 39.0, 72.42640687119285, 102.42640687119285, 132.42640687119285, 162.42640687119285, 192.42640687119285,
                    222.42640687119285, 231.42640687119285, NaN}, //
            {NaN, 85.15432893255071, 81.42640687119285, 111.42640687119285, 115.15432893255071, 145.1543289325507,
                    175.1543289325507, 231.42640687119285, 240.42640687119285, NaN}, //
            {NaN, 42.42640687119285, 97.88225099390857, 94.15432893255071, 124.15432893255071, 127.88225099390857,
                    136.88225099390857, 187.88225099390857, 244.1543289325507, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] rescaledDistance3dData = {//
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0.0, 0.0, 0.0, 0.0, 455.9900485969531, 555.399322667447, 757.0073089555855, NaN}, //
            {NaN, 143.17821063276352, 293.0870177950569, 274.45607392255715, 357.88771429204087, 409.2246767925934,
                    464.5037782221883, 680.9349749569496, 599.1463713854708, NaN}, //
            {NaN, 2.1316282072803006E-14, 241.8677324489565, 325.29937281844025, 376.6363353189928, 431.9154367485877,
                    487.7525915162834, 478.6974907953828, 508.6974907953828, NaN}, //
            {NaN, 143.17821063276355, 46.90415759823432, 123.0618886568734, 227.46495374597896, 331.86801883508446,
                    436.27108392418995, 466.27108392418995, 496.27108392418995, NaN}, //
            {NaN, 222.1183122775866, 318.5657117423568, 374.85545489715685, 386.5006861032632, 402.67910842533126,
                    487.75259151628336, 668.5085680857568, 601.6560923456917, NaN}, //
            {NaN, 0.0, 268.8836840819463, 351.15405321595745, 390.44391216527674, 402.0891433713831, 465.7287536781724,
                    563.8249255149192, 744.5809020843926, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] h2ca_forGradient = {//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 4.17, 4.17, 4.17, 2.95, 2.95, 3.34, 3.44, NaN}, //
            {NaN, 4.17, 4.17, 4.17, 2.95, 2.95, 3.34, 3.44, 3.44, NaN}, //
            {NaN, 0.0, 4.17, 2.95, 2.95, 3.34, 3.37, 0.0, 3.44, NaN}, //
            {NaN, 4.17, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.33, NaN}, //
            {NaN, 1.18, 1.18, 2.95, 2.95, 3.34, 3.37, 3.33, 3.33, NaN}, //
            {NaN, 0.0, 1.18, 1.18, 2.95, 2.95, 2.95, 3.37, 3.33, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] d2o3dData = {//
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 347.62869363537226, 448.97159047625587, 468.39379858647465, 542.5549826424445, 641.1487129121938,
                    843.5283898114521, 934.5094216190689, NaN}, //
            {NaN, 143.17821063276352, 293.0870177950569, 350.4955373609587, 433.9271777304424, 485.2641402309948,
                    540.5432416605897, 680.9349749569496, 810.1937594290096, NaN}, //
            {NaN, 0.0, 241.86773244895653, 325.29937281844025, 376.6363353189927, 431.9154367485876, 487.7525915162834,
                    478.6974907953828, 508.69749079538286, NaN}, //
            {NaN, 143.17821063276352, 46.90415759823432, 123.0618886568734, 227.46495374597885, 331.86801883508446,
                    436.27108392418995, 466.27108392419, 496.27108392419, NaN}, //
            {NaN, 222.1183122775866, 318.5657117423568, 374.85545489715685, 386.5006861032632, 402.67910842533115,
                    487.7525915162834, 668.5085680857569, 847.5544453291959, NaN}, //
            {NaN, 0.0, 378.00288495878556, 427.193516654359, 426.81697912422317, 438.4622103303295, 650.5942446862938,
                    741.3270381784027, 922.0830147478762, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] basinWateroutletData = {//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}}

    ;
    public static double[][] trimWateroutletData = {//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, 900.0, 1000.0, 1200.0, 1250.0, NaN}, //
            {NaN, NaN, NaN, NaN, 800.0, 850.0, 900.0, 1000.0, 1100.0, NaN}, //
            {NaN, NaN, NaN, 700.0, 750.0, 800.0, 850.0, 800.0, 800.0, NaN}, //
            {NaN, NaN, 430.0, 500.0, 600.0, 700.0, 800.0, 800.0, 800.0, NaN}, //
            {NaN, 600.0, 700.0, 750.0, 760.0, 770.0, 850.0, 1000.0, 1150.0, NaN}, //
            {NaN, NaN, 750.0, 800.0, 780.0, 790.0, 1000.0, 1100.0, 1250.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };
    public static double[][] qcritmapData = {//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 5.0, 5.0, 5.0, 5.0, 5.0, 8888.0, 8888.0, NaN}, //
            {NaN, 5.0, 8888.0, 5.0, 5.0, 5.0, 5.0, 8888.0, 8888.0, NaN}, //
            {NaN, 1.0, 8888.0, 8888.0, 8888.0, 5.0, 5.0, 0.0, 0.0, NaN}, //
            {NaN, 5.0, 1.0, 5.0, 5.0, 5.0, 5.0, 0.0, 0.0, NaN}, //
            {NaN, 5.0, 8888.0, 8888.0, 8888.0, 5.0, 5.0, 8888.0, 8888.0, NaN}, //
            {NaN, 5.0, 5.0, 5.0, 5.0, 5.0, 8888.0, 8888.0, 8888.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //

    };

    public static double[][] classimapData = {//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 8888.0, 8888.0, NaN}, //
            {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 8888.0, 8888.0, NaN}, //
            {NaN, 4.0, 8888.0, 8888.0, 8888.0, 1.0, 1.0, 2.0, 2.0, NaN}, //
            {NaN, 1.0, 4.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, NaN}, //
            {NaN, 1.0, 8888.0, 8888.0, 8888.0, 1.0, 1.0, 8888.0, 8888.0, NaN}, //
            {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 8888.0, 8888.0, 8888.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //

    };

    public static double[][] trasmissivityData = {//
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0.035887947214095973, 0.04697525674777372, 0.06224006936644656, 0.0674177531070194, 0.04697525674777372,
                    0.02420042272882682, 0.028923955693270643, NaN}, //
            {NaN, 0.036181916543984345, 0.02499742168449707, 0.0674177531070194, 0.0674177531070194, 0.0674177531070194,
                    0.0674177531070194, 0.02562070173695107, 0.017194242646828593, NaN}, //
            {NaN, 0.16409583056495225, 0.021433202137015238, 0.02562070173695107, 0.028923955693270643, 0.035887947214095973,
                    0.04697525674777372, 0.1728, 0.1728, NaN}, //
            {NaN, 0.036181916543984345, 0.1563880956623711, 0.06815149747418152, 0.04969930843755912, 0.04969930843755912,
                    0.04969930843755912, 0.1728, 0.1728, NaN}, //
            {NaN, 0.041811768158428964, 0.01908256770573349, 0.020596415714827003, 0.02782147015912487, 0.041811768158428964,
                    0.04697525674777372, 0.02562070173695107, 0.014753132782320282, NaN}, //
            {NaN, 0.035887947214095973, 0.04697525674777372, 0.0674177531070194, 0.14089817974238386, 0.14089817974238386,
                    0.024437610357807074, 0.028923955693270643, 0.028923955693270643, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };

    public static double[][] diff_forPit = {//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0.0, NaN, NaN, NaN, NaN, 0.0, 300.0, NaN}, //
            {NaN, 390.0, 0.0, 0.0, 0.0, 0.0, NaN, 0.0, 300.0, NaN}, //
            {NaN, NaN, 390.0, 390.0, 390.0, 390.0, 390.0, 0.0, 350.0, NaN}, //
            {NaN, NaN, 0.0, 0.0, 0.0, NaN, NaN, 0.0, 350.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };
    public static final double[][] netNumberingChannelDataNN1 = {//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1.0, NaN, NaN, NaN, NaN, 3.0, 10.0, NaN}, //
            {NaN, 2.0, 11.0, 12.0, 13.0, 14.0, NaN, 4.0, 10.0, NaN}, //
            {NaN, NaN, 9.0, 8.0, 7.0, 6.0, 5.0, 19.0, 20.0, NaN}, //
            {NaN, NaN, 15.0, 16.0, 17.0, NaN, NaN, 18.0, 20.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static final double[][] basinDataNN1 = {//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2.0, 1.0, 11.0, 12.0, 13.0, 14.0, 3.0, NaN}, //
            {NaN, 2.0, 1.0, 11.0, 12.0, 13.0, 14.0, 3.0, 10.0, NaN}, //
            {NaN, 2.0, 11.0, 12.0, 13.0, 14.0, 6.0, 4.0, 10.0, NaN}, //
            {NaN, 2.0, 9.0, 8.0, 7.0, 6.0, 5.0, 19.0, 20.0, NaN}, //
            {NaN, 9.0, 15.0, 16.0, 17.0, 7.0, 6.0, 18.0, 20.0, NaN}, //
            {NaN, NaN, 9.0, 15.0, 16.0, 17.0, 17.0, 6.0, 18.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };

    public static double[][] cutoutData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 410, NaN, NaN, NaN, NaN, NaN, 490, NaN, NaN}, //
            {NaN, NaN, 430, 500, 600, 700, 800, 500, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 700, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] cutoutDataInverse = new double[][]{//
    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
            {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
            {400, NaN, 650, 700, 750, 800, 850, NaN, 450, 1500}, //
            {450, 550, NaN, NaN, NaN, NaN, NaN, NaN, 450, 1500}, //
            {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
            {600, NaN, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};

    public static double[][] cutoutDataMaxMinInverse800_1400 = new double[][]{//
    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, NaN, NaN}, //
            {NaN, NaN, NaN, 850, 860, 900, 1000, 1200, 1250, NaN}, //
            {NaN, NaN, NaN, NaN, 800, 850, 900, 1000, 1100, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, 800, 850, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, 850, 1000, 1150, NaN}, //
            {NaN, NaN, NaN, 800, NaN, NaN, 1000, 1100, 1250, NaN}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, NaN, NaN}};

    public static double[][] contourExtractorData = {//
    {95.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5}, //
            {100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5}, //
            {100.5, 90.5, 90.5, 90.5, 90.5, 90.5, 90.5, 90.5, 90.5, 100.5}, //
            {100.5, 90.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 90.5, 100.5}, //
            {100.5, 90.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 90.5, 100.5}, //
            {100.5, 90.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 90.5, 100.5}, //
            {100.5, 90.5, 90.5, 90.5, 90.5, 90.5, 90.5, 90.5, 90.5, 100.5}, //
            {100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5, 100.5}//
    };

    public static double[][] all2Data = new double[][]{//
    {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0}, //
            {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0}, //
            {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0}, //
            {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0}, //
            {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0}, //
            {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0}, //
            {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0}, //
            {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0}};

    public static double[][] all1Data = new double[][]{//
    {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}, //
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}, //
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}, //
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}, //
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}, //
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}, //
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}, //
            {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}};

    public static double[][] rangeLookupInData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 254.0, 242.0, 220.0, 247.0, 259.0, 257.0, 259.0, NaN}, //
            {NaN, 254.0, 256.0, 241.0, 242.0, 243.0, 250.0, 253.0, 263.0, NaN}, //
            {NaN, 270.0, 259.0, 240.0, 240.0, 241.0, 180.0, 139.0, 265.0, NaN}, //
            {NaN, 19.0, 52.0, 283.0, 273.0, 262.0, 270.0, 360.0, 275.0, NaN}, //
            {NaN, 286.0, 291.0, 314.0, 341.0, 299.0, 284.0, 281.0, 277.0, NaN}, //
            {NaN, 291.0, 300.0, 333.0, 10.0, 285.0, 281.0, 283.0, 278.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] rangeLookupOutData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, NaN}, //
            {NaN, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, NaN}, //
            {NaN, 4.0, 3.0, 3.0, 3.0, 3.0, 3.0, 2.0, 3.0, NaN}, //
            {NaN, 1.0, 1.0, 4.0, 4.0, 3.0, 4.0, NaN, 4.0, NaN}, //
            {NaN, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, NaN}, //
            {NaN, 4.0, 4.0, 4.0, 1.0, 4.0, 4.0, 4.0, 4.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    // public static SimpleFeatureCollection createFcFromPoint( Coordinate point,
    // CoordinateReferenceSystem crs ) {
    // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
    // b.setName("test");
    // b.setCRS(crs);
    // b.add("the_geom", Point.class);
    // b.add("cat", Integer.class);
    // SimpleFeatureType type = b.buildFeatureType();
    // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
    // GeometryFactory gf = GeometryUtilities.gf();
    // Object[] nw = new Object[]{gf.createPoint(point), 1};
    // builder.addAll(nw);
    //        SimpleFeature pointFeature = builder.buildFeature(type.getTypeName() + ".1"); //$NON-NLS-1$
    // testFC = FeatureCollections.newCollection();
    // testFC.add(pointFeature);
    // return testFC;
    // }

    // public static void main( String[] args ) {
    // double radtodeg = 360.0 / (2 * Math.PI);
    //
    // NumberFormat formatter = new DecimalFormat("0.000");
    // for( int i = 0; i < aspectDataDegrees.length; i++ ) {
    // System.out.print("{");
    // for( int j = 0; j < aspectDataDegrees[0].length; j++ ) {
    // double value = aspectDataDegrees[i][j] / radtodeg;
    // System.out.print(value + ", ");
    // }
    // System.out.println("}, //");
    // }
    // }

}