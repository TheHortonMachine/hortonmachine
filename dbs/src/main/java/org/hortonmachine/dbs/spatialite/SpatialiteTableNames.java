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

import java.util.Arrays;
import java.util.List;


/**
 * Spatialite table namse and groups.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SpatialiteTableNames  {
    public static final String CHECK_SPATIALITE_TABLE = "spatial_ref_sys";
    public static final String startsWithIndexTables = "idx_";

    // STYLE
    public static final String startsWithStyleTables = "SE_";

    // METADATA
    public static final List<String> metadataTables = Arrays.asList("geom_cols_ref_sys", //
            "geometry_columns", //
            "geometry_columns_time", //
            "raster_coverages", //
            "raster_coverages_keyword", //
            "raster_coverages_ref_sys", //
            "raster_coverages_srid", //
            "spatial_ref_sys", //
            "spatial_ref_sys_all", //
            "spatial_ref_sys_aux", //
            "spatialite_history", //
            "vector_coverages", //
            "vector_coverages_keyword", //
            "vector_coverages_ref_sys", //
            "vector_coverages_srid", //
            "vector_layers", //
            "views_geometry_columns", //
            "virts_geometry_columns"//
    );

    // INTERNAL DATA
    public static final List<String> internalDataTables = Arrays.asList(//
            "sqlite_stat1", //
            "sqlite_stat3", //
            "elementarygeometries", // ElementaryGeometries
            "geometry_columns_auth", //
            "geometry_columns_field_infos", //
            "geometry_columns_statistics", //
            "sql_statements_log", //
            "sqlite_sequence", //
            "vector_layers_auth", //
            "vector_layers_field_infos", //
            "vector_layers_statistics", //
            "views_geometry_columns_auth", //
            "views_geometry_columns_field_infos", //
            "views_geometry_columns_statistics", //
            "virts_geometry_columns_auth", //
            "virts_geometry_columns_field_infos", //
            "virts_geometry_columns_statistics",//
            "spatialindex", //
            "knn" //
            );
}
