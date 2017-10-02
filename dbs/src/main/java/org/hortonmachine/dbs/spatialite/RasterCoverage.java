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
package org.hortonmachine.dbs.spatialite;

/**
 * Class representing a raster_coverages record.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RasterCoverage {

    // COLUMN NAMES
    public static final String TABLENAME = "raster_coverages";
    public static final String COVERAGE_NAME = "coverage_name";
    public static final String TITLE = "title";
    public static final String SRID = "srid";
    public static final String COMPRESSION = "compression";
    public static final String EXTENT_MINX = "extent_minx";
    public static final String EXTENT_MINY = "extent_miny";
    public static final String EXTENT_MAXX = "extent_maxx";
    public static final String EXTENT_MAXY = "extent_maxy";

    // VARIABLES
    public String coverage_name;
    public String title;
    public int srid;
    public String compression;
    public double extent_minx;
    public double extent_miny;
    public double extent_maxx;
    public double extent_maxy;

    @Override
    public String toString() {
        return "RasterCoverage [\n\tcoverage_name=" + coverage_name //
            + ", \n\ttitle=" + title //
            + ", \n\tsrid=" + srid //
            + ", \n\tcompression=" + compression //
            + ", \n\textent_minx=" + extent_minx //
            + ", \n\textent_miny=" + extent_miny //
            + ", \n\textent_maxx=" + extent_maxx //
            + ", \n\textent_maxy=" + extent_maxy //
            + "\n]";
    }

}
