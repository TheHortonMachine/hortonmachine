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
package org.hortonmachine.dbs.geopackage;

import java.util.Arrays;
import java.util.List;

/**
 * Spatialite table namse and groups.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GeopackageTableNames{
    public static final String startsWithIndexTables = "rtree_";

    // METADATA
    public static final List<String> metadataTables = Arrays.asList("gpkg_contents", //
            "gpkg_geometry_columns", //
            "gpkg_spatial_ref_sys", //
            "gpkg_data_columns", //
            "gpkg_tile_matrix", //
            "gpkg_metadata", //
            "gpkg_metadata_reference", //
            "gpkg_tile_matrix_set", //
            "gpkg_data_column_constraints", //
            "gpkg_extensions", //
            "gpkg_ogr_contents", //
            "gpkg_spatial_index",//
            "spatial_ref_sys",//
            "st_spatial_ref_sys"//
    );

    // INTERNAL DATA
    public static final List<String> internalDataTables = Arrays.asList(//
            "sqlite_stat1", //
            "sqlite_stat3", //
            "sql_statements_log", //
            "sqlite_sequence" //
    );

}
