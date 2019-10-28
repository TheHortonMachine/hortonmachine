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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.ISpatialTableNames;

/**
 * Spatialite table namse and groups.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SpatialiteTableNames implements ISpatialTableNames {
    public static final String CHECK_SPATIALITE_TABLE = "spatial_ref_sys";
    public static final String startsWithIndexTables = "idx_";
    public static final List<String> spatialindexTables = Arrays.asList("spatialindex", // SpatialIndex
            "knn" // KNN
    );

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
            "virts_geometry_columns_statistics");

    /**
     * Sorts all supplied table names by type.
     * 
     * <p>
     * Supported types are:
     * <ul>
     * <li>{@value ISpatialTableNames#INTERNALDATA} </li>
     * <li>{@value ISpatialTableNames#SYSTEM} </li>
     * </ul>
     * 
     * @param allTableNames list of all tables.
     * @param doSort if <code>true</code>, table names are alphabetically sorted.
     * @return the {@link LinkedHashMap}.
     */
    public static LinkedHashMap<String, List<String>> getTablesSorted( List<String> allTableNames, boolean doSort ) {
        LinkedHashMap<String, List<String>> tablesMap = new LinkedHashMap<>();
        tablesMap.put(USERDATA, new ArrayList<String>());
        tablesMap.put(SYSTEM, new ArrayList<String>());

        for( String tableName : allTableNames ) {
            tableName = tableName.toLowerCase();
            if (spatialindexTables.contains(tableName) || tableName.startsWith(startsWithIndexTables)
                    || tableName.startsWith(startsWithStyleTables) || metadataTables.contains(tableName)
                    || internalDataTables.contains(tableName)) {
                List<String> list = tablesMap.get(SYSTEM);
                list.add(tableName);
                continue;
            }
            List<String> list = tablesMap.get(USERDATA);
            list.add(tableName);
        }

        if (doSort) {
            for( List<String> values : tablesMap.values() ) {
                Collections.sort(values);
            }
        }

        return tablesMap;
    }
}
