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
package org.hortonmachine.gears.libs.modules;

import java.io.File;
import java.text.DecimalFormat;

import javax.swing.filechooser.FileFilter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.io.geopaparazzi.GeopaparazziUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Constant values and novalues handling.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HMConstants {
    /*
     * constants for models
     */
    /**
     * The default double novalue. 
     * 
     * <p>Note: if this changes, also the checker methods like 
     * {@link #isNovalue(double)} have to be changed.
     */
    public static final double doubleNovalue = -9999.0;

    /** 
     * Get the novalue from the coverage, if defined.
     * 
     * @param gc the coverage to check.
     * @return the novalue from the coverage or a default if not defined.
     */
    public static double getNovalue( GridCoverage2D gc ) {
        Double nvObj = CoverageUtilities.getNovalue(gc);
        double nv = HMConstants.doubleNovalue;
        if (nvObj != null) {
            nv = nvObj;
        }
        return nv;
    }
    public static int getIntNovalue( GridCoverage2D gc ) {
        Double nvObj = CoverageUtilities.getNovalue(gc);
        int nv = HMConstants.intNovalue;
        if (nvObj != null) {
            nv = nvObj.intValue();
        }
        return nv;
    }

    /**
     * Check if a value is novalue, the standard HM way.
     * 
     * @param value the value to check.
     * @return <code>true</code> if the value is a novalue.
     */
    public static boolean isNovalue( double value ) {
        return Double.isNaN(value) || value == doubleNovalue;
    }

    /** 
     * Check if the value is a novalue, also against a provided possible value. 
     * 
     * @param value the value to check.
     * @param noValue the novalue to check against.
     * @return <code>true</code> if the value is a novalue.
     */
    public static boolean isNovalue( double value, double noValue ) {
        return value == noValue || isNovalue(value);
    }

    /**
     * Checker for a list of default double novalues.
     * 
     * @param values the list of values to check.
     * @return true if one of the passes values is a novalue.
     * 
     * @see #isNovalue(double)
     */
    public static boolean isOneNovalue( double... values ) {
        for( double value : values ) {
            if (Double.isNaN(value) || value == doubleNovalue)
                return true;
        }
        return false;
    }

    /**
     * The default float novalue. 
     */
    public static final float floatNovalue = -9999f;

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
        return Float.isNaN(value) || value == floatNovalue;
    }

    /**
     * The default int novalue. 
     */
    public static final int intNovalue = -9999;

    /**
     * The default short novalue. 
     */
    public static final short shortNovalue = -9999;

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
        return intNovalue == value;
    }

    /**
     * Check if the width and height of a raster would lead to a numeric overflow.
     * 
     * @param width width of the matrix or raster.
     * @param height height of the matrix or raster.
     * @return true if there is overfow.
     */
    public static boolean doesOverFlow( int width, int height ) {
        if ((long) width * (long) height < Integer.MAX_VALUE) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Global formatter for joda datetime (yyyyMMddHHmmss).
     */
    public static String dateTimeFormatterYYYYMMDDHHMMSScompact_string = "yyyyMMddHHmmss";
    public static DateTimeFormatter dateTimeFormatterYYYYMMDDHHMMSScompact = DateTimeFormat
            .forPattern(dateTimeFormatterYYYYMMDDHHMMSScompact_string);

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
    public static DateTimeFormatter utcDateFormatterYYYYMMDDHHMMSS = DateTimeFormat
            .forPattern(utcDateFormatterYYYYMMDDHHMMSS_string).withZone(DateTimeZone.UTC);
    public static String utcDateFormatterYYYYMMDDHHMM_string = "yyyy-MM-dd HH:mm";
    public static DateTimeFormatter utcDateFormatterYYYYMMDDHHMM = DateTimeFormat.forPattern(utcDateFormatterYYYYMMDDHHMM_string)
            .withZone(DateTimeZone.UTC);

    public static DecimalFormat DEGREE6_FORMATTER = new DecimalFormat("###.######");

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
    public static final String PNG = "png";
    public static final String JPG = "jpg";
    public static final String JPEG = "jpeg";
    public static final String GEOTIFF = "tiff";
    public static final String GEOTIF = "tif";
    public static final String GRASS = "grass";
    public static final String SHP = "shp";
    public static final String GPKG = "gpkg";
    public static final String LAS = "las";
    public static final String LAZ = "laz";

    public static final String DB_TABLE_PATH_SEPARATOR = "#";

    public static final String[] SUPPORTED_VECTOR_EXTENSIONS = {SHP, GPKG};
    public static final String[] SUPPORTED_LIDAR_EXTENSIONS = {LAS};
    public static final String[] SUPPORTED_RASTER_EXTENSIONS = {GEOTIFF, GEOTIF, ESRIGRID, GPKG};
    public static final String[] SUPPORTED_DB_EXTENSIONS = {EDb.SPATIALITE.getExtension(), EDb.H2GIS.getExtension(),
            GeopaparazziUtilities.GPAP_EXTENSION, EDb.GEOPACKAGE.getExtension()};

    /*
     * modules categories
     */
    public static final String OTHER = "Others";
    // IO
    public static final String MATRIXREADER = "Matrix Reader";
    public static final String GENERICREADER = "Generic Reader";
    public static final String GENERICWRITER = "Generic Writer";
    public static final String HASHMAP_READER = "HashMap Data Reader";
    public static final String HASHMAP_WRITER = "HashMap Data Writer";
    public static final String LIST_READER = "List Data Reader";
    public static final String LIST_WRITER = "List Data Writer";
    public static final String RASTERREADER = "Raster Reader";
    public static final String GRIDGEOMETRYREADER = "Grid Geometry Reader";
    public static final String RASTERWRITER = "Raster Writer";
    public static final String FEATUREREADER = "Vector Reader";
    public static final String FEATUREWRITER = "Vector Writer";
    // processing
    public static final String RASTERPROCESSING = "Raster Processing";
    public static final String VECTORPROCESSING = "Vector Processing";
    public static final String LESTO = "Lesto";
    public static final String MOBILE = "Mobile";
    // horton
    public static final String BASIN = "HortonMachine/Basin";
    public static final String DEMMANIPULATION = "HortonMachine/Dem Manipulation";
    public static final String GEOMORPHOLOGY = "HortonMachine/Geomorphology";
    public static final String HYDROGEOMORPHOLOGY = "HortonMachine/Hydro-Geomorphology";
    public static final String HILLSLOPE = "HortonMachine/Hillslope";
    public static final String NETWORK = "HortonMachine/Network";
    public static final String STATISTICS = "HortonMachine/Statistics";
    public static final String GDAL = "Gdal";
    public static final String PDAL = "Pdal";

    public static final String GPL3_LICENSE = "General Public License Version 3 (GPLv3)";

    /*
     * vars ui hints
     */
    public static final String WORKINGFOLDER = "@@@WORKINGFOLDER@@@";
    public static final String HIDE_UI_HINT = "hide";

    public static final String FILEIN_UI_HINT_GENERIC = "infile";
    public static final String FILEIN_UI_HINT_CSV = "infile_csv";
    public static final String FILEIN_UI_HINT_LAS = "infile_las";
    public static final String FILEIN_UI_HINT_RASTER = "infile_raster";
    public static final String FILEIN_UI_HINT_VECTOR = "infile_vector";
    public static final String FILEIN_UI_HINT_DBF = "infile_dbf";
    public static final String FILEIN_UI_HINT_GPAP = "infile_gpap";
    public static final String FILEIN_UI_HINT_JSON = "infile_json";

    public static final String FOLDERIN_UI_HINT = "infolder";
    public static final String FILEOUT_UI_HINT = "outfile";
    public static final String FOLDEROUT_UI_HINT = "outfolder";
    public static final String FILESPATHLIST_UI_HINT = "filespathlist";
    public static final String CRS_UI_HINT = "crs";
    public static final String COMBO_UI_HINT = "combo";
    public static final String ITERATOR_UI_HINT = "iterator";
    public static final String EASTINGNORTHING_UI_HINT = "eastnorth";
    public static final String NORTHING_UI_HINT = "northing";
    public static final String EASTING_UI_HINT = "easting";
    public static final String MULTILINE_UI_HINT = "multiline";
    public static final String MAPCALC_UI_HINT = "mapcalc";
    public static final String PROCESS_NORTH_UI_HINT = "process_north";
    public static final String PROCESS_SOUTH_UI_HINT = "process_south";
    public static final String PROCESS_EAST_UI_HINT = "process_east";
    public static final String PROCESS_WEST_UI_HINT = "process_west";
    public static final String PROCESS_COLS_UI_HINT = "process_cols";
    public static final String PROCESS_ROWS_UI_HINT = "process_rows";
    public static final String PROCESS_XRES_UI_HINT = "process_xres";
    public static final String PROCESS_YRES_UI_HINT = "process_yres";

    public static final FileFilter vectorFileFilter = new HMFileFilter("Supported Vector Files", SUPPORTED_VECTOR_EXTENSIONS);

    public static final FileFilter rasterFileFilter = new HMFileFilter("Supported Raster Files", SUPPORTED_RASTER_EXTENSIONS);

    public static final FileFilter lasFileFilter = new HMFileFilter("Supported LiDAR Files", SUPPORTED_LIDAR_EXTENSIONS);

    public static final FileFilter dbFileFilter = new HMFileFilter("Supported Database Files", SUPPORTED_DB_EXTENSIONS);

    public static boolean isVector( File file ) {
        String name = file.getName().toLowerCase();
        for( String ext : SUPPORTED_VECTOR_EXTENSIONS ) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRaster( File file ) {
        String name = file.getName().toLowerCase();
        for( String ext : SUPPORTED_RASTER_EXTENSIONS ) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
