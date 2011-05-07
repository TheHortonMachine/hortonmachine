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
package org.jgrasstools.hortonmachine.utils;

import static java.lang.Double.NaN;

import java.util.HashMap;

import org.geotools.referencing.CRS;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller.Pitfiller;
import org.jgrasstools.hortonmachine.modules.geomorphology.draindir.DrainDir;
import org.jgrasstools.hortonmachine.modules.geomorphology.flow.FlowDirections;
import org.jgrasstools.hortonmachine.modules.network.extractnetwork.ExtractNetwork;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * The grass map named test and its values to be used in tests.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HMTestMaps {
    public static final HashMap<String, Double> envelopeParams = new HashMap<String, Double>();

    public static CoordinateReferenceSystem crs = null;
    public static CoordinateReferenceSystem crs3004 = null;

    static {
        envelopeParams.put(CoverageUtilities.NORTH, 5140020.0);
        envelopeParams.put(CoverageUtilities.SOUTH, 5139780.0);
        envelopeParams.put(CoverageUtilities.WEST, 1640650.0);
        envelopeParams.put(CoverageUtilities.EAST, 1640950.0);
        envelopeParams.put(CoverageUtilities.XRES, 30.0);
        envelopeParams.put(CoverageUtilities.YRES, 30.0);
        envelopeParams.put(CoverageUtilities.ROWS, 8.0);
        envelopeParams.put(CoverageUtilities.COLS, 10.0);

        try {
            crs = CRS.decode("EPSG:32632");
            crs3004 = CRS.decode("EPSG:3004");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The DEM which is the start point of the test.
     * 
     * N.B. in the first test it's read from the DataBase inside this plug-in.
     */
    public static double[][] mapData = new double[][]{//
    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
            {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
            {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
            {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
            {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
            {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500} //
    };

    /**
     * Input data for the {@link Pitfiller} module.
     */
    public static double[][] pitData = new double[][]{ //
    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
            {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
            {400, 410, 650, 700, 750, 800, 850, 800, 800, 1500}, //
            {450, 550, 430, 500, 600, 700, 800, 800, 800, 1500}, //
            {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
            {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500} //
    };

    /**
     * Output data of the {@link Pitfiller} module.
     */
    public static double[][] outPitData = new double[][]{ //
    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, 500, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
            {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
            {400, 410, 650, 700, 750, 800, 850, 800, 800, 1500}, //
            {450, 550, 430, 500, 600, 700, 800, 800, 800, 1500}, //
            {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
            {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500} //
    };

    /**
     * Output data of the {@link FlowDirections} module.
     */
    public static double[][] flowData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 6, 6, 6, 6, 6, 6, 6, NaN}, //
            {NaN, 7, 6, 6, 6, 6, 6, 7, 7, NaN}, //
            {NaN, 5, 5, 7, 6, 6, 6, 6, 5, NaN}, //
            {NaN, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
            {NaN, 2, 3, 3, 4, 4, 4, 3, 3, NaN}, //
            {NaN, 4, 4, 4, 4, 4, 5, 4, 4, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    /**
     * Output data for the {@link DrainDir} module run in LTD mode.
     */
    public static double[][] drainData0 = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 6, 5, 6, 6, 6, 5, 6, NaN}, //
            {NaN, 7, 6, 6, 6, 6, 6, 7, 7, NaN}, //
            {NaN, 10, 5, 7, 6, 6, 6, 6, 5, NaN}, //
            {NaN, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
            {NaN, 2, 3, 3, 4, 4, 4, 3, 3, NaN}, //
            {NaN, 3, 4, 4, 3, 3, 5, 4, 4, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    /**
     * Output data for the {@link DrainDir} module run in LAD mode.
     */
    public static double[][] drainData1 = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 6, 6, 6, 6, 6, 6, 6, NaN}, //
            {NaN, 7, 6, 6, 6, 6, 6, 7, 7, NaN}, //
            {NaN, 10, 5, 7, 6, 6, 6, 6, 5, NaN}, //
            {NaN, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
            {NaN, 2, 3, 3, 3, 4, 4, 3, 3, NaN}, //
            {NaN, 3, 4, 4, 4, 4, 5, 4, 4, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] mflowData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 6, 6, 6, 6, 6, 6, 6, NaN}, //
            {NaN, 7, 6, 6, 6, 6, 6, 7, 7, NaN}, //
            {NaN, 10, 5, 7, 6, 6, 6, 6, 5, NaN}, //
            {NaN, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
            {NaN, 2, 3, 3, 4, 4, 4, 3, 3, NaN}, //
            {NaN, 10, 4, 4, 4, 4, 5, 4, 4, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] mflowDataBorder = new double[][]{ //
    {5, 6, 8, 8, 9, 6, 5, 8, 5, 5}, //
            {3, NaN, 6, 6, 6, 6, 6, 6, 6, 3}, //
            {3, 7, 6, 6, 6, 6, 6, 7, 7, 3}, //
            {4, 10, 5, 7, 6, 6, 6, 6, 5, 2}, //
            {3, 3, 4, 5, 5, 5, 5, 5, 5, 3}, //
            {3, 2, 3, 3, 4, 4, 4, 3, 3, 5}, //
            {5, 10, 4, 4, 4, 4, 5, 4, 4, 3}, //
            {3, 3, 7, 3, 3, 8, 3, 9, 3, 3} //
    };

    public static double[][] slopeData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 4.71, 3.54, 2.59, 2.36, 3.54, 7.07, 5.89, NaN}, //
            {NaN, 4.67, 6.84, 2.36, 2.36, 2.36, 2.36, 6.67, 10, NaN}, //
            {NaN, 0.33, 8, 6.67, 5.89, 4.71, 3.54, 0, 0, NaN}, //
            {NaN, 4.67, 0.47, 2.33, 3.33, 3.33, 3.33, 0, 0, NaN}, //
            {NaN, 4.01, 9, 8.33, 6.13, 4.01, 3.54, 6.67, 11.67, NaN}, //
            {NaN, 4.71, 3.54, 2.36, 0.71, 0.71, 7, 5.89, 5.89, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] tcaData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 2, 2, 2, 2, 2, 2, 2, 1, NaN}, //
            {NaN, 46, 3, 3, 3, 3, 1, 5, 2, NaN}, //
            {NaN, 1, 37, 32, 20, 15, 11, 5, 2, NaN}, //
            {NaN, 2, 2, 2, 3, 1, 2, 2, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 2, 1, 1, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    /**
     * Output tca for {@link DrainDir} in LTD mode.
     */
    public static double[][] mtcaData0 = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 1, 1, 1, 2, 1, 1, NaN}, //
            {NaN, 3, 1, 2, 2, 3, 1, 2, 1, NaN}, //
            {NaN, 47, 3, 3, 4, 2, 1, 5, 2, NaN}, //
            {NaN, 1, 38, 32, 21, 15, 11, 5, 2, NaN}, //
            {NaN, 3, 2, 1, 2, 3, 2, 2, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 2, 1, 1, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    /**
     * Output tca for {@link DrainDir} in LAD mode.
     */
    public static double[][] mtcaData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 2, 2, 2, 2, 2, 2, 2, 1, NaN}, //
            {NaN, 47, 3, 3, 3, 3, 1, 5, 2, NaN}, //
            {NaN, 1, 38, 32, 23, 15, 11, 5, 2, NaN}, //
            {NaN, 3, 2, 2, 3, 1, 2, 2, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 2, 1, 1, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] tca3DData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 5438.94, 5370.95, 5963.77, 6552.17, 7082.91, 6568.44, 7191.73, NaN}, //
            {NaN, 10320.83, 9568.38, 8653.17, 8973.7, 9682.18, 10800.86, 13431.92, 10391.83, NaN}, //
            {NaN, 257338.23, 14529.45, 13231.44, 13420.35, 13928.84, 3096.91, 39495.19, 22327, NaN}, //
            {NaN, 5382.74, 212067.61, 185732.11, 123898.49, 101011.35, 82961.85, 40031.45, 22377.36, NaN}, //
            {NaN, 9234.82, 9916.27, 10455.59, 17929.35, 3652.57, 11112.19, 13567.34, 10219.8, NaN}, //
            {NaN, 5384.03, 4828.31, 4947.56, 5864.58, 14606.37, 7294.38, 6426.99, 7298.45, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] gradientData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 4.55, 6.72, 7.06, 8.33, 7.17, 7.68, NaN}, //
            {NaN, NaN, 3.73, 3, 2.48, 2.36, 3.54, 7.45, 11.21, NaN}, //
            {NaN, 4.17, 6.6, 4.49, 3.73, 3, 1.67, 3.44, 12.69, NaN}, //
            {NaN, 3.18, 1.18, 2.95, 3.34, 3.37, 1.67, 3.33, 13.04, NaN}, //
            {NaN, 4.17, 5.89, 5.1, 3.02, 2.12, 5.08, 7.07, 11.21, NaN}, //
            {NaN, 5.74, 4.96, 4.21, 6.5, 8.05, 8.43, 6.51, 8.33, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] gradientHornData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 5.45, 6.33, 6.96, 7.37, 7.00, 6.80, NaN}, //
            {NaN, NaN, NaN, 2.73, 2.44, 2.65, 4.17, 6.35, 9.94, NaN}, //
            {NaN, 3.13, 4.48, 4.49, 3.93, 3.25, 2.52, 3.36, 11.33, NaN}, //
            {NaN, 2.79, 2.00, 2.19, 2.17, 2.47, 1.93, 3.29, 11.46, NaN}, //
            {NaN, 3.89, 4.77, 4.77, 3.27, 3.42, 4.90, 6.30, 10.08, NaN}, //
            {NaN, 5.75, 5.02, 5.03, 6.14, 7.36, 7.50, 6.83, 7.29, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] gradientEvansData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 5.75, 6.24, 6.93, 7.08, 6.96, 6.56, NaN}, //
            {NaN, NaN, NaN, 2.64, 2.43, 2.75, 4.39, 5.98, 9.58, NaN}, //
            {NaN, 2.98, 3.78, 4.50, 4.01, 3.34, 2.86, 3.44, 10.91, NaN}, //
            {NaN, 3.05, 2.70, 1.93, 1.79, 2.17, 2.06, 3.36, 10.99, NaN}, //
            {NaN, 3.90, 4.42, 4.67, 3.37, 3.85, 4.84, 6.10, 9.82, NaN}, //
            {NaN, 5.75, 5.04, 5.32, 6.06, 7.15, 7.21, 6.94, 6.96, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] aspectDataDegrees = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 254.0, 242.0, 220.0, 247.0, 259.0, 257.0, 259.0, NaN}, //
            {NaN, 254.0, 256.0, 241.0, 242.0, 243.0, 250.0, 253.0, 263.0, NaN}, //
            {NaN, 270.0, 259.0, 240.0, 240.0, 241.0, 180.0, 139.0, 265.0, NaN}, //
            {NaN, 19.0, 52.0, 283.0, 273.0, 262.0, 270.0, 360.0, 275.0, NaN}, //
            {NaN, 286.0, 291.0, 314.0, 341.0, 299.0, 284.0, 281.0, 277.0, NaN}, //
            {NaN, 291.0, 300.0, 333.0, 10.0, 285.0, 281.0, 283.0, 278.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] aspectDataRadiants = new double[][]{ //
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
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] tanData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, -0.0204365, 0.0040265, 0.0134249, 0.0195928, -0.0160252, 0.0319721, NaN}, //
            {NaN, NaN, NaN, 0.0045884, 0.0070564, -0.0043396, -0.0075602, -0.0088648, 0.0148889, NaN}, //
            {NaN, 0.0726049, -0.0401959, -0.0020833, 0.0000000, 0.0026991, -0.0571662, 0.0255704, 0.0347564, NaN}, //
            {NaN, -0.0736340, 0.2192600, 0.1503699, 0.0992518, 0.0523111, 0.0571662, 0.0000000, 0.0390698, NaN}, //
            {NaN, -0.0022819, -0.0203546, -0.0069907, 0.0034304, 0.0023689, 0.0075309, -0.0175035, 0.0025633, NaN}, //
            {NaN, -0.0029989, 0.0065292, -0.0188262, 0.0054898, 0.0325153, 0.0000358, 0.0160437, 0.0227700, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] planData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, -0.0209238, 0.0040709, 0.0135588, 0.0197333, -0.0161804, 0.0322418, NaN}, //
            {NaN, NaN, NaN, 0.0048358, 0.0076094, -0.0047140, -0.0078567, -0.0089443, 0.0149480, NaN}, //
            {NaN, 0.0746667, -0.0406542, -0.0021344, 0.0000000, 0.0028446, -0.0666667, 0.0266314, 0.0348641, NaN}, //
            {NaN, -0.0771799, 0.2875568, 0.1587560, 0.1036112, 0.0545648, 0.0666667, 0.0000000, 0.0391844, NaN}, //
            {NaN, -0.0023467, -0.0206459, -0.0071239, 0.0036137, 0.0026189, 0.0076754, -0.0176777, 0.0025735, NaN}, //
            {NaN, -0.0030441, 0.0066608, -0.0193493, 0.0055543, 0.0327652, 0.0000360, 0.0162320, 0.0229333, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] profData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 0.0005018, 0.0010118, 0.0008380, 0.0002856, -0.0002802, 0.0000646, NaN}, //
            {NaN, NaN, NaN, 0.0012920, -0.0004059, 0.0006620, 0.0028001, 0.0001567, -0.0000006, NaN}, //
            {NaN, 0.0032482, -0.0004414, -0.0016163, -0.0019340, -0.0020187, 0.0000000, 0.0040650, 0.0003239, NaN}, //
            {NaN, -0.0026547, 0.1128483, 0.0021268, -0.0000321, 0.0001144, -0.0151322, 0.0052724, 0.0002928, NaN}, //
            {NaN, 0.0008304, -0.0008365, -0.0016418, -0.0051775, 0.0012921, 0.0010801, 0.0000381, -0.0000592, NaN}, //
            {NaN, 0.0004254, 0.0012926, 0.0021125, 0.0013562, 0.0007339, -0.0000186, 0.0002137, 0.0000526, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] nablaData1 = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0.055, 0.083, 0.114, 0.16, 0.125, -0.015, 0.09, NaN}, //
            {NaN, 0.055, -0.23, 0.003, 0.014, 0.017, 0.028, -0.028, 0.16, NaN}, //
            {NaN, 0.12, -0.069, -0.075, -0.0555, -0.027, -0.014, 0.111, 0.444, NaN}, //
            {NaN, 0.014, 0.205, 0.238, 0.172, 0.114, 0.047, 0.125, 0.444, NaN}, //
            {NaN, -0.047, -0.069, -0.122, -0.069, 0.027, -0.0027, -0.042, 0.083, NaN}, //
            {NaN, 0.049, 0.0725, 0.108, 0.167, 0.23, 0.075, 0.083, 0.083, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] nablaData0 = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, NaN}, //
            {NaN, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, NaN}, //
            {NaN, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, NaN}, //
            {NaN, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, NaN}, //
            {NaN, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, NaN}, //
            {NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] cp9Data = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 40, 10, 10, 10, 10, 70, NaN}, //
            {NaN, NaN, NaN, 10, 10, 10, 30, 10, 10, NaN}, //
            {NaN, 90, 40, 10, 20, 20, 40, 90, 70, NaN}, //
            {NaN, 50, 90, 90, 70, 70, 80, 30, 70, NaN}, //
            {NaN, 10, 40, 10, 20, 10, 10, 10, 10, NaN}, //
            {NaN, 10, 10, 30, 10, 70, 10, 10, 70, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] cp3Data = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 35, 25, 25, 25, 25, 15, NaN}, //
            {NaN, NaN, NaN, 25, 25, 25, 15, 25, 25, NaN}, //
            {NaN, 15, 35, 25, 35, 35, 35, 15, 15, NaN}, //
            {NaN, 35, 15, 15, 15, 15, 35, 15, 15, NaN}, //
            {NaN, 25, 35, 25, 35, 25, 25, 25, 25, NaN}, //
            {NaN, 25, 25, 15, 25, 15, 25, 25, 15, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] multiTcaData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, 2.25, 2.125, 2.0, 2.0, 2.0, 1.0, NaN}, //
            {NaN, 33.87, 3.25, 3.47, 3.45, 3.14, 1.0, 5.14, 2.0, NaN}, //
            {NaN, 1.82, 31.5, 26.14, 15.13, 8.35, 7.43, 5.14, 2.0, NaN}, //
            {NaN, 2.28, 2.0, 2.06, 3.0, 2.0, 2.0, 2.0, 1.0, NaN}, //
            {NaN, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] abData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 18.11, 34.15, 50.22, 72.43, 19.99, 300.0, NaN}, //
            {NaN, NaN, NaN, 70.12, 77.578, 52.52, 48.44, 47.16, 53.93, NaN}, //
            {NaN, 13800.0, 47.37, 84.56, 90.0, 98.36, 15.78, 725.23, 600.0, NaN}, //
            {NaN, 15.79, 11100.0, 9600.0, 6000.0, 4500.0, 3300.0, 150.0, 600.0, NaN}, //
            {NaN, 56.04, 36.44, 49.34, 100.89, 32.55, 77.77, 38.74, 32.50, NaN}, //
            {NaN, 27.48, 37.43, 18.71, 35.96, 600.0, 30.03, 57.89, 94.19, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] bData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 49.67, 26.35, 17.92, 12.43, 45.01, 3.0, NaN}, //
            {NaN, NaN, NaN, 25.67, 23.2, 34.27, 37.16, 38.16, 16.69, NaN}, //
            {NaN, 3, 57, 31.93, 30.0, 27.45, 57, 6.2, 3, NaN}, //
            {NaN, 57, 3, 3, 3, 3, 3, 30.0, 3, NaN}, //
            {NaN, 32.12, 49.39, 36.48, 26.76, 27.65, 23.14, 46.47, 27.69, NaN}, //
            {NaN, 32.75, 24.05, 48.11, 25.03, 3.0, 29.97, 15.55, 9.56, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] diametersData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 42.43, 42.43, 42.43, 42.43, 42.43, 42.43, 42.43, 0, NaN}, //
            {NaN, 228.47, 84.85, 84.85, 84.85, 84.85, 0, 67.08, 30, NaN}, //
            {NaN, 0, 201.25, 174.93, 150, 127.28, 108.17, 67.08, 30, NaN}, //
            {NaN, 42.43, 42.43, 42.43, 67.08, 0, 42.43, 42.43, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 30, 0, 0, 0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}

    };
    public static double[][] distEuclideaData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 152.97, 161.55, 174.93, 192.09, 212.13, 234.31, 258.07, NaN}, //
            {NaN, 120, 123.69, 134.16, 150, 169.71, 192.09, 216.33, 241.87, NaN}, //
            {NaN, 90, 94.87, 108.17, 127.28, 150, 174.93, 201.25, 228.47, NaN}, //
            {NaN, 60, 67.08, 84.85, 108.17, 134.16, 161.55, 189.74, 218.4, NaN}, //
            {NaN, 30, 42.43, 67.08, 94.87, 123.69, 152.97, 182.48, 212.13, NaN}, //
            {NaN, 0, 30, 60, 90, 120, 150, 180, 210, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] meandropData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 100, 75, 55, 50, 75, 150, 125, 0, NaN}, //
            {NaN, 404.13, 103.33, 100, 116.67, 166.67, 0, 190, 150, NaN}, //
            {NaN, 0, 418.11, 378.13, 326, 250, 177.27, 200, 175, NaN}, //
            {NaN, 75, 50, 15, 90, 0, 125, 125, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 105, 0, 0, 0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    /**
     * {@link ExtractNetwork} output with mode 0.
     */
    public static double[][] extractNet0Data = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, 2, NaN, NaN}, //
            {NaN, NaN, 2, 2, 2, 2, 2, 2, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    /**
     * {@link ExtractNetwork} output with mode 1.
     */
    public static double[][] extractNet1Data = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 2, 2, NaN}, //
            {NaN, 2, 2, 2, 2, 2, NaN, 2, 2, NaN}, //
            {NaN, NaN, 2, 2, 2, 2, 2, 2, 2, NaN}, //
            {NaN, NaN, 2, 2, 2, NaN, NaN, 2, 2, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] d2oPixelData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 2, 3, 5, 5, 6, 8, NaN}, //
            {NaN, 1, 1, 2, 4, 4, 5, 7, 8, NaN}, //
            {NaN, 0, 1, 3, 3, 4, 5, 6, 7, NaN}, //
            {NaN, 1, 1, 2, 3, 4, 5, 6, 7, NaN}, //
            {NaN, 2, 2, 3, 3, 4, 5, 7, 8, NaN}, //
            {NaN, 0, 3, 3, 4, 4, 5, 6, 8, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] hacklengthData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 42.43, 42.43, 42.43, 42.43, 42.43, 42.43, 42.43, 0, NaN}, //
            {NaN, 264.85, 84.85, 84.85, 84.85, 84.85, 0, 72.43, 30, NaN}, //
            {NaN, 0, 222.43, 192.43, 162.43, 132.43, 102.43, 72.43, 30, NaN}, //
            {NaN, 42.43, 42.43, 42.43, 72.43, 0, 42.43, 42.43, 0, NaN}, //
            {NaN, NaN, 0, 0, 0, 30, 0, 0, 0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] hacklength3DData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 204.45, 155.88, 117.9, 108.63, 155.88, 302.99, 253.57, 0, NaN}, //
            {NaN, 848.83, 226.53, 217.26, 264.51, 411.61, 0, 332.99, 302.99, NaN}, //
            {NaN, 0, 801.93, 725.77, 621.37, 516.97, 412.56, 382.56, 352.56, NaN}, //
            {NaN, 155.88, 108.63, 51.96, 264.09, 0, 253.57, 253.57, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 212.13, 0, 0, 0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] hackstream = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 2, 3, NaN}, //
            {NaN, 1, 2, 2, 2, 2, NaN, 2, 3, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 2, NaN}, //
            {NaN, NaN, 2, 2, 2, NaN, NaN, 1, 2, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };
    public static double[][] magnitudoData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 18, 1, 1, 1, 1, 1, 2, 1, NaN}, //
            {NaN, 1, 14, 12, 8, 6, 4, 2, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, 1, 1, 1, 1, 1, 1, 1, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN},};

    public static double[][] netNumberingChannelDataJG = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 2.0, 1.0, 3.0, 5.0, 6.0, NaN, 8.0, NaN, NaN}, //
            {NaN, NaN, 4.0, 4.0, 7.0, 8.0, 8.0, 9.0, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, 10.0, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN},};

    public static double[][] netNumberingChannelDataNN0 = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1.0, NaN, NaN, NaN, NaN, 3.0, 9.0, NaN}, //
            {NaN, 2.0, 10.0, 11.0, 12.0, 13.0, NaN, 4.0, 9.0, NaN}, //
            {NaN, NaN, 8.0, 7.0, 6.0, 5.0, 5.0, 18.0, 19.0, NaN}, //
            {NaN, NaN, 14.0, 15.0, 16.0, NaN, NaN, 17.0, 19.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] netNumberingChannelDataNN2 = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1.0, NaN, NaN, NaN, NaN, 2.0, 9.0, NaN}, //
            {NaN, NaN, 10.0, 11.0, 12.0, 13.0, NaN, 3.0, 9.0, NaN}, //
            {NaN, NaN, 8.0, 7.0, 6.0, 5.0, 4.0, 18.0, 19.0, NaN}, //
            {NaN, NaN, 14.0, 15.0, 16.0, NaN, NaN, 17.0, 19.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] basinDataNN0 = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2.0, 1.0, 10.0, 11.0, 12.0, 13.0, 3.0, NaN}, //
            {NaN, 2.0, 1.0, 10.0, 11.0, 12.0, 13.0, 3.0, 9.0, NaN}, //
            {NaN, 2.0, 10.0, 11.0, 12.0, 13.0, 5.0, 4.0, 9.0, NaN}, //
            {NaN, 2.0, 8.0, 7.0, 6.0, 5.0, 5.0, 18.0, 19.0, NaN}, //
            {NaN, 8.0, 14.0, 15.0, 16.0, 6.0, 5.0, 17.0, 19.0, NaN}, //
            {NaN, NaN, 8.0, 14.0, 15.0, 16.0, 16.0, 5.0, 17.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN},};

    public static final double[][] basinDataNN1 = { //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2.0, 1.0, 11.0, 12.0, 13.0, 14.0, 3.0, NaN}, //
            {NaN, 2.0, 1.0, 11.0, 12.0, 13.0, 14.0, 3.0, 10.0, NaN}, //
            {NaN, 2.0, 11.0, 12.0, 13.0, 14.0, 6.0, 4.0, 10.0, NaN}, //
            {NaN, 2.0, 9.0, 8.0, 7.0, 6.0, 5.0, 19.0, 20.0, NaN}, //
            {NaN, 9.0, 15.0, 16.0, 17.0, 7.0, 6.0, 18.0, 20.0, NaN}, //
            {NaN, NaN, 9.0, 15.0, 16.0, 17.0, 17.0, 6.0, 18.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] basinDataNN2 = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 1.0, 10.0, 11.0, 12.0, 13.0, 2.0, NaN}, //
            {NaN, NaN, 1.0, 10.0, 11.0, 12.0, 13.0, 2.0, 9.0, NaN}, //
            {NaN, NaN, 10.0, 11.0, 12.0, 13.0, 5.0, 3.0, 9.0, NaN}, //
            {NaN, NaN, 8.0, 7.0, 6.0, 5.0, 4.0, 18.0, 19.0, NaN}, //
            {NaN, 8.0, 14.0, 15.0, 16.0, 6.0, 5.0, 17.0, 19.0, NaN}, //
            {NaN, NaN, 8.0, 14.0, 15.0, 16.0, 16.0, 5.0, 17.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN},};

    public static double[][] netNumberingChannelData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 7, 7, NaN}, //
            {NaN, 1, 2, 3, 4, 5, NaN, 6, 7, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, 8, NaN}, //
            {NaN, NaN, 11, 10, 9, NaN, NaN, 8, 8, NaN}, //
            {NaN, 12, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] splitSubBasinDataJG = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2.0, 2.0, 1.0, 3.0, 5.0, 6.0, 8.0, NaN}, //
            {NaN, 2.0, 2.0, 1.0, 3.0, 5.0, 6.0, 8.0, 8.0, NaN}, //
            {NaN, 2.0, 1.0, 3.0, 5.0, 6.0, 8.0, 8.0, 8.0, NaN}, //
            {NaN, 2.0, 4.0, 4.0, 7.0, 8.0, 8.0, 9.0, 9.0, NaN}, //
            {NaN, 4.0, 4.0, 4.0, 10.0, 7.0, 8.0, 9.0, 9.0, NaN}, //
            {NaN, NaN, 4.0, 4.0, 4.0, 10.0, 10.0, 8.0, 9.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    /**
     * This isn't used. It maintain it only for future test.
     */
    public static double[][] splitSubBasinData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 2, 2, 3, 4, 7, 7, NaN}, //
            {NaN, 1, 2, 2, 3, 4, 5, 7, 7, NaN}, //
            {NaN, 1, 2, 3, 4, 5, 1, 6, 7, NaN}, //
            {NaN, 1, 1, 1, 1, 1, 1, 1, 8, NaN}, //
            {NaN, 1, 11, 10, 9, 1, 1, 8, 8, NaN}, //
            {NaN, 12, 1, 11, 10, 9, 9, 1, 8, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] strahlerData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1, NaN, NaN, NaN, NaN, 1, 1, NaN}, //
            {NaN, 3, 1, 1, 1, 1, NaN, 2, 1, NaN}, //
            {NaN, NaN, 3, 3, 3, 3, 3, 2, 1, NaN}, //
            {NaN, NaN, 1, 1, 1, NaN, NaN, 1, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    /**
     * It was obtained with the tcaData and strahlerData, it is to test the h.seol command.
     */
    public static double[][] soelData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, 2, NaN, NaN}, //
            {NaN, 46, 3, 3, 3, 3, NaN, 5, 2, NaN}, //
            {NaN, NaN, 37, 32, 20, 15, 11, 5, 2, NaN}, //
            {NaN, NaN, 2, 2, 3, NaN, NaN, 2, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] cp3GCData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 45, 25, 25, 25, 55, 15, NaN}, //
            {NaN, NaN, 35, 25, 25, 25, 15, 35, 55, NaN}, //
            {NaN, 35, 55, 35, 35, 35, 45, 35, 35, NaN}, //
            {NaN, 45, 35, 35, 35, 35, 35, 35, 35, NaN}, //
            {NaN, 25, 55, 55, 35, 25, 25, 35, 55, NaN}, //
            {NaN, 35, 25, 15, 25, 15, 55, 25, 15, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] cp9GCData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 40, 10, 10, 10, 110, 70, NaN}, //
            {NaN, NaN, 100, 10, 10, 10, 30, 100, 110, NaN}, //
            {NaN, 100, 110, 100, 100, 100, 40, 100, 100, NaN}, //
            {NaN, 50, 100, 100, 100, 100, 100, 100, 100, NaN}, //
            {NaN, 10, 110, 110, 100, 10, 10, 100, 110, NaN}, //
            {NaN, 100, 10, 30, 10, 70, 110, 10, 70, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] topIndexData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, -1.55, -1.26, -0.95, -0.86, -1.26, -1.96, -1.77, NaN}, //
            {NaN, -0.85, -1.23, -0.17, -0.17, -0.17, -0.17, -1.2, -2.3, NaN}, //
            {NaN, 4.94, -0.98, -0.8, -0.67, -0.45, -1.26, NaN, NaN, NaN}, //
            {NaN, -1.54, 4.37, 2.62, 1.79, 1.51, 1.19, NaN, NaN, NaN}, //
            {NaN, -0.7, -1.5, -1.43, -0.71, -1.39, -0.57, -1.2, -2.46, NaN}, //
            {NaN, -1.55, -1.26, -0.86, 0.34, 1.04, -1.95, -1.77, -1.77, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] tauData = new double[][]{ //
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

    public static double[][] h2cdTopoData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 72.43, 42.43, 84.85, 84.85, 84.85, 84.85, 42.43, NaN}, //
            {NaN, 30, 0, 42.43, 42.43, 42.43, 42.43, 0, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 0, 42.43, 0, 0, NaN}, //
            {NaN, 30, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 42.43, 0, 0, 0, 42.43, 42.43, 0, 0, NaN}, //
            {NaN, 0, 84.85, 42.43, 42.43, 42.43, 72.43, 84.85, 42.43, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] h2cdData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2, 1, 2, 2, 2, 2, 1, NaN}, //
            {NaN, 1, 0, 1, 1, 1, 1, 0, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 0, 1, 0, 0, NaN}, //
            {NaN, 1, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 1, 0, 0, 0, 1, 1, 0, 0, NaN}, //
            {NaN, 0, 2, 1, 1, 1, 2, 2, 1, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] h2cd3dData = new double[][]{ //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 347.63, 155.88, 226.53, 217.26, 264.51, 411.61, 253.57, NaN}, //
            {NaN, 143.18, 0, 108.63, 108.63, 108.63, 108.63, 0, 0, NaN}, //
            {NaN, 0, 0, 0, 0, 0, 155.88, 0, 0, NaN}, //
            {NaN, 143.18, 0, 0, 0, 0, 0, 0, 0, NaN}, //
            {NaN, 175.21, 0, 0, 0, 175.21, 155.88, 0, 0, NaN}, //
            {NaN, 0, 331.1, 108.63, 51.96, 51.96, 264.09, 409.46, 253.57, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] ddData = new double[][]{ //
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
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}//
    };

    public static double[][] rescaledDistanceData = { //
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
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] rescaledDistance3dData = { //
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
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] h2ca_forGradient = { //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 4.17, 4.17, 4.17, 2.95, 2.95, 3.34, 3.44, NaN}, //
            {NaN, 4.17, 4.17, 4.17, 2.95, 2.95, 3.34, 3.44, 3.44, NaN}, //
            {NaN, 0.0, 4.17, 2.95, 2.95, 3.34, 3.37, 0.0, 3.44, NaN}, //
            {NaN, 4.17, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.33, NaN}, //
            {NaN, 1.18, 1.18, 2.95, 2.95, 3.34, 3.37, 3.33, 3.33, NaN}, //
            {NaN, 0.0, 1.18, 1.18, 2.95, 2.95, 2.95, 3.37, 3.33, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] d2o3dData = { //
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
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] basinWateroutletData = {{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] trimWateroutletData = { //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, 900.0, 1000.0, 1200.0, 1250.0, NaN}, //
            {NaN, NaN, NaN, NaN, 800.0, 850.0, 900.0, 1000.0, 1100.0, NaN}, //
            {NaN, NaN, NaN, 700.0, 750.0, 800.0, 850.0, 800.0, 800.0, NaN}, //
            {NaN, NaN, 430.0, 500.0, 600.0, 700.0, 800.0, 800.0, 800.0, NaN}, //
            {NaN, 600.0, 700.0, 750.0, 760.0, 770.0, 850.0, 1000.0, 1150.0, NaN}, //
            {NaN, NaN, 750.0, 800.0, 780.0, 790.0, 1000.0, 1100.0, 1250.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] qcritmapData = { //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 5.0, 5.0, 5.0, 5.0, 5.0, 8888.0, 8888.0, NaN}, //
            {NaN, 5.0, 8888.0, 5.0, 5.0, 5.0, 5.0, 8888.0, 8888.0, NaN}, //
            {NaN, 1.0, 8888.0, 8888.0, 8888.0, 5.0, 5.0, 0.0, 0.0, NaN}, //
            {NaN, 5.0, 1.0, 5.0, 5.0, 5.0, 5.0, 0.0, 0.0, NaN}, //
            {NaN, 5.0, 8888.0, 8888.0, 8888.0, 5.0, 5.0, 8888.0, 8888.0, NaN}, //
            {NaN, 5.0, 5.0, 5.0, 5.0, 5.0, 8888.0, 8888.0, 8888.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
    };

    public static double[][] classimapData = { //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 8888.0, 8888.0, NaN}, //
            {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 8888.0, 8888.0, NaN}, //
            {NaN, 4.0, 8888.0, 8888.0, 8888.0, 1.0, 1.0, 2.0, 2.0, NaN}, //
            {NaN, 1.0, 4.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, NaN}, //
            {NaN, 1.0, 8888.0, 8888.0, 8888.0, 1.0, 1.0, 8888.0, 8888.0, NaN}, //
            {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 8888.0, 8888.0, 8888.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
    };

    public static double[][] trasmissivityData = { //
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
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] streamData = null;
    public static double[][] diff_forPit = {{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0.0, NaN, NaN, NaN, NaN, 0.0, 300.0, NaN}, //
            {NaN, 390.0, 0.0, 0.0, 0.0, 0.0, NaN, 0.0, 300.0, NaN}, //
            {NaN, NaN, 390.0, 390.0, 390.0, 390.0, 390.0, 0.0, 350.0, NaN}, //
            {NaN, NaN, 0.0, 0.0, 0.0, NaN, NaN, 0.0, 350.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static final double[][] netNumberingChannelDataNN1 = { //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1.0, NaN, NaN, NaN, NaN, 3.0, 10.0, NaN}, //
            {NaN, 2.0, 11.0, 12.0, 13.0, 14.0, NaN, 4.0, 10.0, NaN}, //
            {NaN, NaN, 9.0, 8.0, 7.0, 6.0, 5.0, 19.0, 20.0, NaN}, //
            {NaN, NaN, 15.0, 16.0, 17.0, NaN, NaN, 18.0, 20.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static final double[][] netNumberingChannelDataNN3 = { //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1.0, NaN, NaN, NaN, NaN, 3.0, 10.0, NaN}, //
            {NaN, 2.0, 11.0, 12.0, 13.0, 14.0, NaN, 4.0, 10.0, NaN}, //
            {NaN, NaN, 9.0, 8.0, 7.0, 6.0, 5.0, 19.0, 20.0, NaN}, //
            {NaN, NaN, 15.0, 16.0, 17.0, NaN, NaN, 18.0, 20.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static final double[][] basinDataNN3 = { //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 1.0, NaN, NaN, NaN, NaN, 2.0, 9.0, NaN}, //
            {NaN, NaN, 11.0, 12.0, 13.0, 14.0, NaN, 3.0, 10.0, NaN}, //
            {NaN, NaN, 8.0, 7.0, 6.0, 5.0, 4.0, 19.0, 20.0, NaN}, //
            {NaN, NaN, 15.0, 16.0, 17.0, NaN, NaN, 18.0, 20.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

    public static double[][] basinShapeData = new double[][]{{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 2.0, 2.0, 2.0, 3.0, 3.0, 3.0, 3.0, NaN}, //
            {NaN, 2.0, 2.0, 2.0, 2.0, 3.0, 3.0, 3.0, 3.0, NaN}, //
            {NaN, 2.0, 2.0, 2.0, 2.0, 3.0, 3.0, 3.0, 3.0, NaN}, //
            {NaN, 2.0, 2.0, 2.0, 2.0, 3.0, 3.0, 3.0, 3.0, NaN}, //
            {NaN, 2.0, 2.0, 2.0, 2.0, 3.0, 3.0, 3.0, 3.0, NaN}, //
            {NaN, NaN, 2.0, 2.0, 2.0, 3.0, 3.0, 3.0, 3.0, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] outValidation = { //
    {113, 3.873, NaN}, //
            {1008, 0.775, 0.6}, //
            {1010, 1.425, 0.4}, //
            {1019, 0.400, NaN}, //
            {1020, 1.610, 0}, //
            {1023, 0.72, NaN}, //
            {1032, 1.740, NaN}, //
            {1044, 1.885, NaN}, //
            {1048, 0.620, 0.2}, //
            {1032, 1.74, NaN}, //
            {1058, 0.625, 1.8}, //
            {1079, 0.455, 0.4}, //
            {1088, 1.485, NaN}, //
            {1101, 1.300, 0}, //
            {1133, 2.150, 0}, //
            {1134, 0.625, NaN}, //
            {1136, 1.025, 0}, //
            {1152, 1.565, 0.6}, //
            {1164, 4.191, 0}, //
            {1165, 0.894, 0}, //
            {1221, 1.575, 0}, //
            {1224, 1.585, 0}, //
            {1330, 1.335, 0}, //
            {1332, 0, 2.4}, //
            {1358, NaN, NaN} //
    };

    public static double[][] outKriging4 = { //
    {1221, 0.628}, //
            {1097, 0.658}, //
            {944, 0.76}, //
            {945, 0.769}, //
            {946, 0.746}, //
            {699, 0.806}, //
            {693, 0.796}, //
            {1105, 0.684}, //
            {1104, 0.666}, //
            {1242, 0.601}, //
            {1263, 0.593}, //
            {1260, 0.556}, //
            {769, 0.8}, //
            {614, 0.806}, //
            {615, 0.797}, //
            {1039, 0.705}, //
            {613, 0.808}, //
            {618, 0.776}, //
            {619, 0.754}, //
            {616, 0.795}, //
            {617, 0.79}, //
            {622, 0.719}, //
            {623, 0.7}, //
            {620, 0.73}, //
            {621, 0.715}, //
            {762, 0.782}, //
            {627, 0.605}, //
            {626, 0.618}, //
            {1291, 0.604}, //
            {763, 0.768}, //
            {625, 0.642}, //
            {624, 0.676}, //
            {630, 0.543}, //
            {629, 0.522}, //
            {1292, 0.572}, //
            {628, 0.561}, //
            {1041, 0.692}, //
            {1040, 0.68}, //
            {1042, 0.708}, //
            {1179, 0.619}, //
            {1066, 0.696}, //
            {1064, 0.694}, //
            {1065, 0.704}, //
            {1070, 0.687}, //
            {1068, 0.703}, //
            {1069, 0.665}, //
            {1202, 0.659}, //
            {970, 0.74}, //
            {1210, 0.669}, //
            {1075, 0.694}, //
            {1211, 0.666}, //
            {961, 0.722}, //
            {1209, 0.665}, //
            {841, 0.76}, //
            {1212, 0.674} //
    };

    public static double[][] outKriging3 = { //
    {1221, 0.6442378}, //
            {1097, 0.6638148}, //
            {944, 0.7238208}, //
            {945, 0.727541}, //
            {946, 0.712001}, //
            {699, 0.7418895}, //
            {693, 0.7299253}, //
            {1105, 0.6662023}, //
            {1104, 0.6692828}, //
            {1242, 0.6101497}, //
            {1263, 0.5973132}, //
            {1260, 0.5733984}, //
            {769, 0.7405109}, //
            {614, 0.7371844}, //
            {615, 0.7353087}, //
            {1039, 0.693413}, //
            {613, 0.7386884}, //
            {618, 0.7281364}, //
            {619, 0.7187028}, //
            {616, 0.7365323}, //
            {617, 0.7355}, //
            {622, 0.6955245}, //
            {623, 0.6830942}, //
            {620, 0.7067043}, //
            {621, 0.6968199}, //
            {762, 0.7243572}, //
            {627, 0.6249317}, //
            {626, 0.6355195}, //
            {1291, 0.6256669}, //
            {763, 0.7221966}, //
            {625, 0.6532975}, //
            {624, 0.6714035}, //
            {630, 0.56632}, //
            {629, 0.5403056}, //
            {1292, 0.5965968}, //
            {1041, 0.6860426}, //
            {1040, 0.6777603}, //
            {1042, 0.6959469}, //
            {1179, 0.6300316}, //
            {1066, 0.6763092}, //
            {1064, 0.6238516}, //
            {1065, 0.6589148}, //
            {1070, 0.6820022}, //
            {1068, 0.6902787}, //
            {1069, 0.6659198}, //
            {1202, 0.6625429}, //
            {970, 0.7136494}, //
            {1210, 0.665163}, //
            {1075, 0.648025}, //
            {1211, 0.664769}, //
            {961, 0.6922433}, //
            {1209, 0.6584804}, //
            {841, 0.7138797}, //
            {1212, 0.6706933} //
    };

    /**
     * This matrix was calculated with the insolation.f90 program by Corripio, with the default parameters and latitude=45.85 and for the first 2 day of the year. 
     */
    public static final double[][] outInsolation = {
            {4.0262055, 9.8828831, 11.387176, 10.036429, 12.566648, 12.711961, 11.378096, 10.327982, 8.7154636, 0.0000000},
            {0.0000000, 0.0000000, 7.0049877, 7.1897931, 4.3994513, 3.1457014, 4.4807262, 8.8354321, 5.6115084, 0.0000000},
            {6.8733125, 4.9421449, 4.3081231, 1.8639892, 1.8732568, 0.89052296, 0.27625597, 1.4758357, 0.91655457, 0.0000000},
            {0.0000000, 0.0000000, 2.8167233, 0.83104277, 0.21271306, 0.22496822, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.98367536, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {7.00423643E-02, 7.57485926E-02, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000,
                    0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},
            {0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000, 0.0000000},};

    public static final double[][] outHillshade = {//
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
            {0, 0, 143, 0, 0, 0, 0, 0, 0, 0}, //
            {0, 0, 197, 165, 205, 169, 173, 170, 0, 0}, //
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
    };

    public static final double[][] outSkyview = { //
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, 0.45, 0.60, 0.61, 0.60, 0.55, 0.54, NaN, NaN}, //
            {NaN, NaN, 0.60, 0.56, 0.59, 0.58, 0.55, 0.48, NaN, NaN}, //
            {NaN, NaN, 0.31, 0.33, 0.56, 0.60, 0.66, 0.50, NaN, NaN}, //
            {NaN, NaN, 0.52, 0.56, 0.62, 0.50, 0.56, 0.55, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] netFlowData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 8, NaN, NaN, NaN, NaN, NaN, 10, NaN, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 3, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] netOneData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, 1, NaN, NaN}, //
            {NaN, NaN, 1, 1, 1, 1, 1, 1, NaN, NaN}, //
            {NaN, NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] sumDownstreamData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, 10, NaN, NaN}, //
            {NaN, NaN, 2, 5, 6, 7, 8, 9, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] netOneThresData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, 1, NaN, NaN}, //
            {NaN, NaN, 1, 1, 5, 0, 1, 1, NaN, NaN}, //
            {NaN, NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    public static double[][] sumDownstreamThresData = new double[][]{//
    {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, 3, NaN, NaN}, //
            {NaN, NaN, 2, 5, NaN, NaN, 1, 2, NaN, NaN}, //
            {NaN, NaN, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
            {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
    };

    /**
     * Result for the TrentoPTest, this is for the project mode, section
     * rectangular, align =0.
     * 
     */
    public static double[][] project1Rectangular = { //
            {1, 4, 0.45, 103, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 33.16, 73.81, 8.55, 15.21, 15.49, 0.73, 0.35, 24.00, 0.79, -0.49,
                    -0.84, -0.30, -0.65}, //
            {2, 3, 0.27, 103, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 21.28, 79.34, 7.03, 12.71, 13.00, 0.71, 0.43, 20.00, 0.75, -0.27,
                    -0.71, -0.12, -0.56}, //
            {3, 4, 0.5, 35, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 57.56, 75.12, 8.89, 14.50, 14.70, 0.76, 0.27, 31.00, 0.79, -0.80,
                    -0.90, -0.56, -0.65}, //
            {4, 9, 0.62, 67, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 133.21, 72.57, 9.66, 16.00, 16.50, 0.81, 0.18, 46.00, 0.78, -1.01,
                    -1.13, -0.65, -0.77}, //
            {5, 6, 0.62, 102, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 43.86, 70.53, 9.67, 17.02, 17.30, 0.75, 0.30, 28.00, 0.75, -0.28,
                    -0.59, -0.07, -0.38}, //
            {6, 7, 0.61, 150, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 85.17, 69.02, 9.61, 18.40, 19.20, 0.78, 0.22, 37.00, 0.80, -0.68,
                    -1.01, -0.38, -0.72}, //
            {7, 9, 1.06, 33, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 153.75, 67.09, 11.84, 20.05, 20.25, 0.82, 0.17, 49.00, 0.78, -1.10,
                    -1.16, -0.72, -0.77}, //
            {8, 9, 1.48, 118, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 92.04, 62.22, 13.44, 23.37, 23.68, 0.79, 0.22, 39.00, 0.77, -0.82,
                    -1.07, -0.52, -0.77,}, //
            {9, 10, 0.34, 210, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 398.23, 66.94, 7.71, 18.85, 21.75, 0.88, 0.11, 76.00, 0.78, -1.37,
                    -1.60, -0.77, -1.01,}, //
            {10, 11, 2.15, 164, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 503.29, 62.15, 15.49, 24.40, 26.10, 0.90, 0.10, 84.00, 0.79,
                    -1.67, -1.84, -1.01, -1.17,}, //
            {11, 12, 1.23, 70, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 573.94, 61.50, 12.55, 25.75, 27.00, 0.93, 0.10, 88.00, 0.80,
                    -1.87, -1.94, -1.17, -1.24,}, //
            {12, 16, 0.89, 90, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 626.48, 61.26, 11.11, 26.35, 27.90, 0.95, 0.10, 91.00, 0.80,
                    -1.97, -2.06, -1.24, -1.33,}, //
            {13, 14, 0.55, 78, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 39.62, 72.01, 9.23, 16.08, 16.30, 0.73, 0.32, 26.00, 0.80, -0.85,
                    -1.10, -0.64, -0.89,}, //
            {14, 15, 1.04, 201, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 105.23, 66.13, 11.76, 20.50, 21.30, 0.79, 0.20, 41.00, 0.79,
                    -1.22, -1.63, -0.89, -1.30}, //
            {15, 16, 1.52, 18, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 199.30, 64.03, 13.59, 22.30, 22.35, 0.83, 0.15, 55.00, 0.79,
                    -1.74, -1.76, -1.30, -1.33,}, //
            {16, 0, 0, 199, 0, 0, 0.32, 0.7, 70, 0.1, 2, 1, 818.41, 61.35, 0.84, 26.65, 29.40, 1.02, 0.10, 101.00, 0.79, -2.13,
                    -2.33, -1.33, -1.53,} //
    };

    /**
     * Result for the TrentoPTest, this is for the project mode, section
     * trapezium, align =0.
     */
    public static double[][] project1Trapezio = {

            {1, 4, 0.45, 103, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 33.42, 74.38, 8.55, 14.74, 14.90, 1.31, 0.32, 18.00, 0.79, -0.40,
                    -0.74, -0.26, -0.59}, //
            {2, 3, 0.27, 103, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 21.48, 80.08, 7.03, 12.25, 12.42, 1.25, 0.40, 15.00, 0.77, -0.21,
                    -0.62, -0.10, -0.51}, //
            {3, 4, 0.5, 35, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 57.93, 75.61, 8.89, 14.05, 14.10, 1.37, 0.25, 23.00, 0.80, -0.69,
                    -0.78, -0.51, -0.59}, //
            {4, 9, 0.62, 67, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 134.61, 73.33, 9.66, 15.10, 15.45, 1.46, 0.17, 34.00, 0.80, -0.86,
                    -0.98, -0.59, -0.71}, //
            {5, 6, 0.62, 102, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 44.14, 70.97, 9.67, 16.60, 16.76, 1.31, 0.29, 21.00, 0.76, -0.21,
                    -0.50, -0.05, -0.34}, //
            {6, 7, 0.61, 150, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 86.55, 70.14, 9.61, 17.35, 17.85, 1.40, 0.21, 28.00, 0.79, -0.56,
                    -0.87, -0.34, -0.65}, //
            {7, 9, 1.06, 33, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 155.58, 67.89, 11.84, 19.00, 19.05, 1.46, 0.16, 37.00, 0.78, -0.94,
                    -1.00, -0.65, -0.71}, //
            {8, 9, 1.48, 118, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 92.51, 62.53, 13.44, 22.90, 23.08, 1.41, 0.20, 29.00, 0.78, -0.69,
                    -0.93, -0.47, -0.71}, //
            {9, 10, 0.34, 210, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 404.05, 67.92, 7.71, 18.10, 19.65, 1.58, 0.10, 57.00, 0.79, -1.16,
                    -1.37, -0.71, -0.92}, //
            {10, 11, 2.15, 164, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 517.32, 63.88, 15.49, 21.85, 22.80, 1.66, 0.10, 63.00, 0.78,
                    -1.42, -1.58, -0.92, -1.09}, //
            {11, 12, 1.23, 70, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 591.79, 63.41, 12.55, 22.60, 23.40, 1.72, 0.10, 66.00, 0.79,
                    -1.61, -1.68, -1.09, -1.16}, //
            {12, 16, 0.89, 90, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 647.77, 63.34, 11.11, 22.90, 23.85, 1.77, 0.10, 68.00, 0.79,
                    -1.70, -1.79, -1.16, -1.25}, //
            {13, 14, 0.55, 78, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 39.83, 72.39, 9.23, 15.74, 15.87, 1.30, 0.30, 20.00, 0.76, -0.76,
                    -0.99, -0.61, -0.84}, //
            {14, 15, 1.04, 201, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 106.78, 67.10, 11.76, 19.30, 19.80, 1.42, 0.19, 31.00, 0.78,
                    -1.08, -1.46, -0.84, -1.22}, //
            {15, 16, 1.52, 18, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 201.24, 64.65, 13.59, 21.25, 21.30, 1.51, 0.14, 41.00, 0.79,
                    -1.55, -1.57, -1.22, -1.25}, //
            {16, 0, 0, 199, 0, 0, 0.32, 0.7, 70, 0.1, 3, 1, 845.11, 63.35, 0.84, 23.20, 24.75, 1.89, 0.10, 75.00, 0.79, -1.84,
                    -2.04, -1.25, -1.45}

    };
    /**
     * Result for the TrentoPTest, this is for the project mode, section
     * circular, align =1.
     * 
     */
    public static double[][] project1align1 = {
            {1, 4, 0.45, 103, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 33.17, 73.83, 8.55, 15.19, 15.47, 0.74, 0.3, 30, 0.61, -1.54,
                    -1.85, -1.34, -1.65}, //
            {2, 3, 0.27, 103, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 21.28, 79.36, 7.03, 12.7, 12.99, 0.72, 0.37, 30, 0.44, -1.54,
                    -1.92, -1.39, -1.77}, //
            {3, 4, 0.5, 35, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 57.57, 75.13, 8.89, 14.35, 14.55, 0.77, 0.24, 40, 0.57, -2.02, -2.10,
                    -1.77, -1.85}, //
            {4, 9, 0.62, 67, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 133.24, 72.58, 9.66, 16, 16.5, 0.82, 0.17, 50, 0.78, -2.26, -2.37,
                    -1.85, -1.97}, //
            {5, 6, 0.62, 102, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 43.86, 70.52, 9.67, 17.03, 17.3, 0.75, 0.28, 30, 0.77, -1.54,
                    -1.83, -1.29, -1.57}, //
            {6, 7, 0.61, 150, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 85.21, 69.06, 9.61, 18.4, 19.2, 0.79, 0.2, 50, 0.54, -1.86, -2.16,
                    -1.57, -1.87}, //
            {7, 9, 1.06, 33, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 153.81, 67.12, 11.84, 19.9, 20.1, 0.83, 0.15, 60, 0.62, -2.26,
                    -2.31, -1.87, -1.92}, //
            {8, 9, 1.48, 118, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 92.05, 62.22, 13.44, 23.36, 23.67, 0.8, 0.19, 50, 0.57, -1.74,
                    -1.96, -1.44, -1.66}, //
            {9, 10, 0.34, 210, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 398.49, 66.98, 7.71, 19, 21.75, 0.9, 0.1, 90, 0.66, -2.58, -2.79,
                    -1.97, -2.18}, //
            {10, 11, 2.15, 164, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 504.02, 62.24, 15.49, 24.4, 25.95, 0.93, 0.1, 90, 0.79, -2.91,
                    -3.07, -2.18, -2.34}, //
            {11, 12, 1.23, 70, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 575, 61.61, 12.55, 25.75, 26.85, 0.98, 0.1, 100, 0.7, -3.06,
                    -3.13, -2.34, -2.41}, //
            {12, 16, 0.89, 90, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 627.92, 61.4, 11.11, 26.2, 27.6, 0.99, 0.1, 100, 0.75, -3.18,
                    -3.27, -2.41, -2.50}, //
            {13, 14, 0.55, 78, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 39.63, 72.02, 9.23, 16.06, 16.28, 0.75, 0.29, 30, 0.7, -1.54,
                    -1.76, -1.31, -1.53}, //
            {14, 15, 1.04, 201, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 105.29, 66.17, 11.76, 20.5, 21.3, 0.81, 0.18, 50, 0.63, -1.87,
                    -2.23, -1.53, -1.89}, //
            {15, 16, 1.52, 18, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 199.37, 64.06, 13.59, 22.3, 22.35, 0.84, 0.14, 60, 0.78, -2.38,
                    -2.41, -1.89, -1.92}, //
            {16, 0, 0, 199, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 820.31, 61.49, 0.84, 26.2, 28.95, 1.06, 0.1, 110, 0.76, -3.35, -3.55,
                    -2.50, -2.70}};;
    /**
     * Result for the TrentoPTest, this is for the project mode, section
     * circular, align =0.
     * 
     */
    public final static double[][] project1 = {
            {1, 4, 0.45, 103, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 33.17, 73.83, 8.55, 15.19, 15.47, 0.74, 0.3, 30, 0.61, -1.74,
                    -2.06, -1.54, -1.85, 0.45, 103, 0}, //
            {2, 3, 0.27, 103, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 21.28, 79.36, 7.03, 12.7, 12.99, 0.72, 0.37, 30, 0.44, -1.54,
                    -1.92, -1.39, -1.77, 0.27, 103, 0}, //
            {3, 4, 0.5, 35, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 57.57, 75.13, 8.89, 14.35, 14.55, 0.77, 0.24, 40, 0.57, -2.02, -2.1,
                    -1.77, -1.85, 0.77, 138, 0}, //
            {4, 9, 0.62, 67, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 133.24, 72.58, 9.66, 16, 16.5, 0.82, 0.17, 50, 0.78, -2.26, -2.37,
                    -1.85, -1.97, 1.84, 159, 1828.67}, //
            {5, 6, 0.62, 102, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 43.86, 70.52, 9.67, 17.03, 17.3, 0.75, 0.28, 30, 0.77, -1.59,
                    -1.88, -1.34, -1.62, 0.62, 102, 0}, //
            {6, 7, 0.61, 150, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 85.21, 69.06, 9.61, 18.4, 19.2, 0.79, 0.2, 50, 0.54, -1.91, -2.21,
                    -1.62, -1.92, 1.23, 252, 0}, //
            {7, 9, 1.06, 33, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 153.81, 67.12, 11.84, 19.9, 20.1, 0.83, 0.15, 60, 0.62, -2.31,
                    -2.36, -1.92, -1.97, 2.29, 234, 2601}, //
            {8, 9, 1.48, 118, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 92.05, 62.22, 13.44, 23.36, 23.67, 0.8, 0.19, 50, 0.57, -2.05,
                    -2.27, -1.74, -1.97, 1.48, 118, 0}, //
            {9, 10, 0.34, 210, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 398.49, 66.98, 7.71, 19, 21.75, 0.9, 0.1, 90, 0.66, -2.58, -2.79,
                    -1.97, -2.18, 5.95, 355.38, 5809.23}, //
            {10, 11, 2.15, 164, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 504.02, 62.24, 15.49, 24.4, 25.95, 0.93, 0.1, 90, 0.79, -2.91,
                    -3.07, -2.18, -2.34, 8.1, 503.22, 7251.06}, //
            {11, 12, 1.23, 70, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 575, 61.61, 12.55, 25.75, 26.85, 0.98, 0.1, 100, 0.7, -3.06,
                    -3.13, -2.34, -2.41, 9.33, 539.3, 16882.41}, //
            {12, 16, 0.89, 90, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 627.92, 61.4, 11.11, 26.2, 27.6, 0.99, 0.1, 100, 0.75, -3.18,
                    -3.27, -2.41, -2.5, 10.23, 586.64, 33549.5}, //
            {13, 14, 0.55, 78, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 39.63, 72.02, 9.23, 16.06, 16.28, 0.75, 0.29, 30, 0.7, -2.12,
                    -2.35, -1.89, -2.11, 0.55, 78, 0}, //
            {14, 15, 1.04, 201, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 105.29, 66.17, 11.76, 20.5, 21.3, 0.81, 0.18, 50, 0.63, -2.45,
                    -2.81, -2.11, -2.48, 1.59, 279, 0}, //
            {15, 16, 1.52, 18, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 199.37, 64.06, 13.59, 22.3, 22.35, 0.84, 0.14, 60, 0.78, -2.96,
                    -2.99, -2.48, -2.5, 3.11, 258, 1521}, //
            {16, 0, 0, 199, 0, 0, 0.32, 0.7, 70, 0.1, 1, 1, 820.31, 61.49, 0.84, 26.2, 28.95, 1.06, 0.1, 110, 0.76, -3.35, -3.55,
                    -2.5, -2.7, 13.34, 670.8, 64018.29}};
    /**
     * Result for the TrentoPTest, this is for the verify mode, section
     * circular.
     * 
     */
    public final static double[][] verify1 = {
            {1.00, NaN, 0.01, 0.02, 0.02, 0.01, 0.01, 0.05, 0.04, 0.00, 0.02, 0.03, 0.02, 0.01, 0.01, 0.09, 0.00}, //
            {16.00, NaN, 0.19, 0.58, 0.94, 0.36, 0.56, 1.39, 0.93, 2.15, 2.83, 3.39, 3.44, 0.31, 0.66, 2.08, 4.07}, //
            {31.00, NaN, 1.41, 3.90, 8.82, 2.93, 5.55, 10.40, 6.21, 23.92, 30.25, 35.19, 37.23, 2.63, 6.70, 13.58, 46.82}, //
            {46.00, NaN, 2.00, 5.57, 13.32, 4.43, 8.71, 15.88, 9.67, 40.11, 52.50, 60.61, 65.70, 3.95, 10.88, 20.81, 84.73}, //
            {61.00, NaN, 1.51, 4.39, 10.42, 3.57, 7.10, 13.27, 8.59, 34.62, 47.39, 54.57, 60.16, 3.14, 9.21, 18.14, 78.82}, //
            {76.00, NaN, 2.28, 6.62, 14.22, 4.95, 9.30, 18.49, 12.04, 42.72, 57.62, 66.93, 72.29, 4.36, 11.76, 25.77, 92.72}, //
            {91.00, NaN, 6.61, 18.42, 42.02, 14.07, 26.89, 50.47, 30.85, 119.50, 154.56, 179.35, 191.74, 12.56, 33.06, 66.77,
                    243.85}, //
            {106.00, NaN, 9.33, 26.09, 62.34, 20.78, 40.86, 74.73, 45.86, 188.88, 248.32, 286.70, 310.94, 18.53, 51.20, 98.39,
                    401.32}, //
    };

    /**
     * It's a collection of parameter which are used in TrentoPTest.
     */
    public final static double[][] PIPE = new double[][]{{30, 2}, {40, 2}, {50, 2}, {60, 2}, {70, 2}, {80, 2}, {90, 2}, {100, 2},
            {110, 2}, {120, 2}, {140, 4}, {160, 4}, {180, 4}, {200, 4}, {240, 6}, {280, 6}};

}
