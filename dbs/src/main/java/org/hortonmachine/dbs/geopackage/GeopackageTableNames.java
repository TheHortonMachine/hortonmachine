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
public class GeopackageTableNames implements ISpatialTableNames {
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
            if (tableName.startsWith(startsWithIndexTables) || metadataTables.contains(tableName)
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
