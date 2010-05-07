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
package org.jgrasstools.gears.libs.modules;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Constant values and novalues handling.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class JGTConstants {
    /*
     * constants for models
     */
    /**
     * The default double novalue. 
     */
    public static final double doubleNovalue = Double.NaN;

    /**
     * Checker for default double novalue.
     * 
     * <p>
     * This was done since with NaN the != check doesn't work.
     * This has to be strict in line with the {@link #doubleNovalue}.
     * </p>
     * 
     * @param value the value to check.
     * @return true if the passed value is a novalue.
     */
    public static boolean isNovalue( double value ) {
        return Double.isNaN(value);
    }

    /**
     * The default float novalue. 
     */
    public static final float floatNovalue = Float.NaN;

    /**
     * Checker for default float novalue.
     * 
     * <p>
     * This was done since with NaN the != check doesn't work.
     * This has to be strict in line with the {@link #floatNovalue}.
     * </p>
     * 
     * @param value the value to check.
     * @return true if the passed value is a novalue.
     */
    public static boolean isNovalue( float value ) {
        return Float.isNaN(value);
    }

    /**
     * The default int novalue. 
     */
    public static final int intNovalue = Integer.MAX_VALUE;

    /**
     * Checker for default int novalue.
     * 
     * <p>
     * This was done since with NaN the != check doesn't work.
     * This has to be strict in line with the {@link #intNovalue}.
     * </p>
     * 
     * @param value the value to check.
     * @return true if the passed value is a novalue.
     */
    public static boolean isNovalue( int value ) {
        return Integer.MAX_VALUE == value;
    }

    /**
     * Global formatter for joda datetime (yyyy-MM-dd HH:mm:ss).
     */
    public static String dateTimeFormatterYYYYMMDDHHMMSS_string = "yyyy-MM-dd HH:mm:ss";
    public static DateTimeFormatter dateTimeFormatterYYYYMMDDHHMMSS = DateTimeFormat
            .forPattern(dateTimeFormatterYYYYMMDDHHMMSS_string);

    /**
    * Global formatter for joda datetime (yyyy-MM-dd HH:mm).
    */
    public static String dateTimeFormatterYYYYMMDDHHMM_string = "yyyy-MM-dd HH:mm";
    public static DateTimeFormatter dateTimeFormatterYYYYMMDDHHMM = DateTimeFormat
            .forPattern(dateTimeFormatterYYYYMMDDHHMM_string);

    public static String utcDateFormatterYYYYMMDDHHMMSS_string = "yyyy-MM-dd HH:mm:ss";
    public static DateTimeFormatter utcDateFormatterYYYYMMDDHHMMSS = DateTimeFormat.forPattern(
            utcDateFormatterYYYYMMDDHHMMSS_string).withZone(DateTimeZone.UTC);
    public static String utcDateFormatterYYYYMMDDHHMM_string = "yyyy-MM-dd HH:mm";
    public static DateTimeFormatter utcDateFormatterYYYYMMDDHHMM = DateTimeFormat.forPattern(
            utcDateFormatterYYYYMMDDHHMM_string).withZone(DateTimeZone.UTC);

    /**
     * Enumeration defining meteo types.
     */
    public static int TEMPERATURE = 0;
    public static int PRESSURE = 1;
    public static int HUMIDITY = 2;
    public static int WIND = 3;
    /**
     * Average daily range temperature.
     */
    public static int DTDAY = 4;
    /**
     * Average monthly range temperature.
     */
    public static int DTMONTH = 5;

    /**
     * Earth rotation [rad/h].
     */
    public final static double omega = 0.261799388; /* velocita' di rotazione terrestre [rad/h] */
    /**
     * Zero celsius degrees in Kelvin.
     */
    public final static double tk = 273.15; /* =0 C in Kelvin */
    /**
     * Von Karman constant.
     */
    public final static double ka = 0.41; /* costante di Von Karman */
    /**
     * Freezing temperature [C]
     */
    public final static double Tf = 0.0; /* freezing temperature [C] */
    /**
     * Solar constant [W/m2].
     */
    public final static double Isc = 1367.0; /* Costante solare [W/m2] */
    /**
     * Water density [kg/m3].
     */
    public final static double rho_w = 1000.0; /* densita' dell'acqua [kg/m3] */
    /**
     * Ice density [kg/m3].
     */
    public final static double rho_i = 917.0; /* densita' del ghiaccio [kg/m3] */
    /**
     * Latent heat of melting [J/kg].
     */
    public final static double Lf = 333700.00; /* calore latente di fusione [J/kg] */
    /**
     * Latent heat of sublimation [J/kg].
     */
    public final static double Lv = 2834000.00; /* calore latente di sublimazione [J/kg] */
    /**
     * Heat capacity of water [J/(kg/K)].
     */
    public final static double C_liq = 4188.00; /* heat capacity of water       [J/(kg/K)] */
    /**
     * Heat capacity of ice [J/(kg/K)].
     */
    public final static double C_ice = 2117.27; /* heat capacity of ice     [J/(kg/K)] */
    /**
     * Adiabatic lapse rate [K/m].
     */
    public final static double GAMMA = 0.006509; /* adiabatic lapse rate [K/m]*/
    /**
     * Costante di Stefan-Boltzmann [W/(m2 K4)].
     */
    public final static double sigma = 5.67E-8; /* costante di Stefan-Boltzmann [W/(m2 K4)]*/

    /*
     * FILE EXTENTIONS
     */
    public static final String AIG = "adf";
    public static final String ESRIGRID = "asc";
    public static final String GEOTIFF = "tiff";
    public static final String GEOTIF = "tif";
    public static final String GRASSRASTER = "grassraster";

}
