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
package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;

/**
 * A collection of constants to use in OmsTrentoP.
 * 
 * 
 * @author Daniele Andreis
 * 
 */
public class Constants {
    /**
     * Conversion coefficient from ha/h to m/s.
     */

    public static final double HAOVERH_TO_METEROVERS = 166.666667;
    /**
     * Conversion coefficient from hour to minutes.
     */
    public static final double HOUR2MIN = 60.0;
    /**
     * Conversion coefficient from meter to cm.
     */
    public static final double METER2CM = 100.0;
    /**
     * Conversion coefficient from hm to cm.
     */
    public static final double HM2CM = 10000.0;
    /**
     * Conversion coefficient from minute to seconds.
     */
    public static final double MINUTE2SEC = 60.0;
    /**
     * Conversion coefficient from m^3 to dm^3 or liter.
     */
    public static final double CUBICMETER2LITER = 1000;
    /**
     * Conversion coefficient from m^2 to cm^2.
     */
    public static final double METER2CMSQUARED = 10000;
    /**
     * 
     */
    public static final double WSPECIFICWEIGHT = 9800;
    /**
     * 6/13
     */
    public static final double SIXOVERTHIRTEEN = 0.461538;
    /**
     * 
     */
    public static final double TWO_TWENTYOVERTHIRTEEN = 290.484571;
    /**
     * 
     */
    public static final double ONEOVERTHIRTEEN = 0.0769231;
    /**
     * 13/6
     */
    public static final double THIRTHEENOVERSIX = 2.166667;
    /**
     * 
     */
    public static final double TWO_TENOVERTHREE = 10.079368;
    /**
     * 1/6
     */
    public static final double ONEOVERSIX = 0.166667;
    /**
     * 
     */
    public static final double TWO_THIRTEENOVEREIGHT = 308.442165;
    /**
     * 3/8
     */
    public static final double THREEOVEREIGHT = 0.375;
    /**
     * 8/3
     */
    public static final double EIGHTOVERTHREE = 2.666667;
    /**
     * 
     */
    public static final double TWO_THIRTEENOVERTHREE = 20.158737;
    /**
     * 2/3
     */
    public static final double TWOOVERTHREE = 0.666667;
    /**
     * 7/13
     */
    public static final double SEVENOVERTHIRTEEN = 0.5384615;
    /**
     * 1/4
     */
    public static final double ONEOVERFOUR = 0.25;
    /**
     * 5/8
     */
    public static final double FIVEOVEREIGHT = 0.625;

    public static final double DEFAULT_ACCURACY = 0.005;
    /**
     * Minimum excavation depth of the circular pipe .
     */
    public static final double DEFAULT_MINIMUM_DEPTH = 1.2;
    /**
     * Max number of pipes that can converge in a junction.
     */

    public static final int DEFAULT_MAX_JUNCTION = 4;
    /**
     * Max number of bisection to do to search a solution of a transcendental
     * equation.
     */
    public static final int DEFAULT_J_MAX = 40;

    /**
     * Time step used to calculate the discharge in a sewer pipe (to search the
     * maximun discgarge when t and tp change).
     * 
     */

    public static final double DEFAULT_TDTP = 0.25;
    /**
     * Minimum Rain Time step to calculate the maximum discharge.
     */
    public static final double DEFAULT_TPMIN = 5;
    /**
     * Maximum Rain Time step to calculate the maximum discharge.
     */
    public static final double DEFAULT_TPMAX = 60;
    /**
     * Accuracy to use to calculate the discharge.
     * 
     */

    public static final double DEFAULT_EPSILON = 0.001;
    /**
     * Minimum Fill degree.
     */

    public static final double DEFAULT_MING = 0.01;
    /**
     * Minimum discharge in a pipe.
     * 
     */

    public static final double DEFAULT_MIN_DISCHARGE = 1.0;
    /**
     * Maximum Fill degree.
     */
    public static final double DEFAULT_MAX_THETA = 6.28319;
    /**
     * 
     * Celerity factor, value used to obtain the celerity of the discharge wave
     */

    public static final double DEFAULT_CELERITY_FACTOR = 1;
    /**
     * Exponent of the basin extension. Used to calculate the average acces time
     * to the network.
     */
    public static final double DEFAULT_EXPONENT = 0.3;
    /**
     * Tolleranza nella determinazione, con un metodo iterativo, del raggio.
     */

    public static final double DEFAULT_TOLERANCE = 0.0001;
    /**
     * Max number of time step.
     * 
     */

    public static final double DEFAULT_TMAX = 120;
    /**
     * Division base to height in the rectangular or trapezium section.
     * 
     */
    public static final double DEFAULT_C = 1;
    /**
     * Exponent of the average ponderal slope of a basin to calculate the
     * average access time to the network for area units.
     */
    public static final double DEFAULT_GAMMA = 0.2;

    /**
     * Exponent of the influx coefficent to calculate the average residence time
     * in the network, k.
     */
    public static final double DEFAULT_ESP1 = 0.4;
    /**
     * "Minimum dig depth, for rectangular or trapezium pipe.
     */
    public static final double DEFAULT_FRANCO = 0.5;

    /**
     * Time step, if pMode=1.
     */

    public final static double DEFAULT_DT = 15;

    public final static Double[] CELERITY_RANGE = {1.0, 1.6};
    public final static Integer[] JMAX_RANGE = {3, 1000};
    public final static Double[] INFLUX_EXPONENT_RANGE = {0.0, null};
    public final static Double[] GAMMA_RANGE = {0.0, null};
    public final static Double[] TOLERANCE_RANGE = {0.0, null};
    public final static Double[] THETA_RANGE = {Math.PI, null};
    public final static Double[] EPS_RANGE = {0.0, 1.0};
    public final static Double[] C_RANGE = {0.0, null};
    public final static Double[] EXPONENT_RANGE = {0.0, null};
    public final static Double[] MIN_DEPTH_RANGE = {null, null};
    public final static Integer[] MAX_JUNCTIONS_RANGE = {0, 6};
    public final static Double[] MIN_FILL_DEGREE_RANGE = {0.0, 0.1};
    public final static Double[] MIN_DISCHARGE_RANGE = {0.0, null};
    /**
     * The name of the shp with the network data.
     */
    public static final String NETWORK_PROJECT_NAME_SHP = "network_project.shp";
    public static final String NETWORK_PROJECT_OUTPUT_NAME_SHP = "network_project_output.shp";
    /**
     * The name of the shp with the network data.
     */
    public static final String NETWORK_CALIBRATION_NAME_SHP = "network_calibration.shp"; //$NON-NLS-1$
    /**
     * The name of the shp with the aree.
     */
    public static final String AREA_NAME_SHP = "areas.shp"; //$NON-NLS-1$
    /**
     * The name of the shp with the wells.
     */
    public static final String JUNCTIONS_NAME_SHP = "junctions.shp"; //$NON-NLS-1$
    public static final String PARAMETERS_CSV = "parameters.csv"; //$NON-NLS-1$
    public static final String DIAMETERS_CSV = "diameters.csv"; //$NON-NLS-1$
    /**
     * The ID of the pipe where drain the last pipe of the net.(It doesn't exist).
     */
    public static final int OUT_ID_PIPE = 0;
    /**
    * The index of the pipe where drain the last pipe of the net.(It doesn't exist).
    */
    public static final int OUT_INDEX_PIPE = -1;

}
