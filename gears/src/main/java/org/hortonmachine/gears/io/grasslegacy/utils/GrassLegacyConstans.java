/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 package org.hortonmachine.gears.io.grasslegacy.utils;

/**
 * <p>
 * Constants used by the JGrass engine
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
@SuppressWarnings("nls")
public interface GrassLegacyConstans {

    /*
     * jgrass database files and folders
     */
    /** folder of the JGrass database structure */
    public final String PERMANENT_MAPSET = "PERMANENT";

    /** folder of the JGrass database structure */
    public final String DEFAULT_WIND = "DEFAULT_WIND";

    /** folder of the JGrass database structure */
    public final String PROJ_INFO = "PROJ_INFO";

    /** folder of the JGrass database structure */
    public final String PROJ_WKT = "PROJ_INFO.WKT";

    /** folder of the JGrass database structure */
    public final String PROJ_UNITS = "PROJ_UNITS";

    /** folder of the JGrass database structure */
    public final String WIND = "WIND";

    /** folder of the JGrass database structure */
    public final String MYNAME = "MYNAME";

    /** folder of the JGrass database structure */
    public final String FCELL = "fcell";

    /** folder of the JGrass database structure */
    public final String CELL = "cell";

    /** folder of the JGrass database structure */
    public final String CATS = "cats";

    /** folder of the JGrass database structure */
    public final String HIST = "hist";

    /** folder of the JGrass database structure */
    public final String CELLHD = "cellhd";

    /** folder of the JGrass database structure */
    public final String COLR = "colr";

    /** folder of the JGrass database structure */
    public final String CELL_MISC = "cell_misc";

    /** folder of the JGrass database structure */
    public final String CELLMISC_FORMAT = "f_format";

    /** folder of the JGrass database structure */
    public final String CELLMISC_QUANT = "f_quant";

    /** folder of the JGrass database structure */
    public final String CELLMISC_RANGE = "f_range";

    /** folder of the JGrass database structure */
    public final String CELLMISC_NULL = "null";

    /** folder of the JGrass database structure */
    public final String DIG = "dig";

    /** folder of the JGrass database structure */
    public final String DIG_ATTS = "dig_atts";

    /** folder of the JGrass database structure */
    public final String DIG_CATS = "dig_cats";

    /** folder of the JGrass database structure */
    public final String SITE_LISTS = "site_lists";

    /** folder of the JGrass database structure */
    public final String VECTORS = "vector";

    /** grass sites map type */
    public final String SITESMAP = "sites";

    /** grass ascii raster format */
    public final String GRASSASCIIRASTER = "grassascii";

    /** fluidturtle ascii raster format */
    public final String FLUIDTURTLEASCIIRASTER = "fluidturtleascii";

    /** esri ascii raster format */
    public final String ESRIASCIIRASTER = "esriasciigrid";
    
    /** tmp data for hortons */
    public static String HORTON_MACHINE_PATH = "hortonmachine";

    /*
     * map formats
     */
    /** raster map types */
    public final String GRASSBINARYRASTERMAP = "grassbinaryraster";
    public final String GRASSASCIIRASTERMAP = "grassasciiraster";
    public final String FTRASTERMAP = "fluidturtleasciiraster";
    public final String ESRIRASTERMAP = "esriasciigrid";

    /** grass 6 vector map types */
    public final String GRASS6VECTORMAP = "grass6vector";
    public final String OLDGRASSVECTORMAP = "oldgrassvector";

    /** grass application paths */
    public final String GRASSBIN = "bin";
    public final String GRASSLIB = "lib";
    
    /*
     * constants for models
     */
    /** default novalue value */
    public final double defaultNovalue = -9999.0;

    /*
     * region definition headers
     */
    public final String HEADER_EW_RES = "e-w res";
    public final String HEADER_NS_RES = "n-s res";
    public final String HEADER_NORTH = "north";
    public final String HEADER_SOUTH = "south";
    public final String HEADER_EAST = "east";
    public final String HEADER_WEST = "west";
    public final String HEADER_ROWS = "rows";
    public final String HEADER_COLS = "cols";
    
    /*
     * esri header pieces
     */
    public final String ESRI_HEADER_XLL_PIECE = "xll";
    public final String ESRI_HEADER_XLL = "xllcorner";
    public final String ESRI_HEADER_YLL_PIECE = "yll";
    public final String ESRI_HEADER_YLL = "yllcorner";
    public final String ESRI_HEADER_NROWS_PIECE = "nr";
    public final String ESRI_HEADER_NROWS = "nrows";
    public final String ESRI_HEADER_NCOLS_PIECE = "nc";
    public final String ESRI_HEADER_NCOLS = "ncols";
    public final String ESRI_HEADER_DIMENSION = "dim";
    public final String ESRI_HEADER_CELLSIZE = "cellsize";
    public final String ESRI_HEADER_NOVALUE_PIECE = "nov";
    public final String ESRI_HEADER_NOVALUE = "nodata_value";
    
}
