package org.jgrasstools.gears.spatialite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SpatialiteTableNames {
    public static final String USERDATA = "User Data";

    public static final String SPATIALINDEX = "Spatial Index";
    public static final String spatialIndex = "SpatialIndex";
    public static final String startsWithIndexTables = "idx_";

    // STYLE
    public static final String STYLE = "Styling (SLD/SE)";
    public static final String startsWithStyleTables = "SE_";

    // METADATA
    public static final String METADATA = "Metadata";
    public static final List<String> metadataTables = Arrays.asList("geom_cols_ref_sys", //
            "geometry_columns",//
            "geometry_columns_time",//
            "raster_coverages",//
            "raster_coverages_keyword",//
            "raster_coverages_ref_sys",//
            "raster_coverages_srid",//
            "spatial_ref_sys", //
            "spatialite_history", //
            "vector_coverages",//
            "vector_coverages_keyword",//
            "vector_coverages_ref_sys",//
            "vector_coverages_srid",//
            "vector_layers",//
            "views_geometry_columns",//
            "virts_geometry_columns"//
    );

    // INTERNAL DATA
    public static final String INTERNALDATA = "Internal Data";
    public static final List<String> internalDataTables = Arrays.asList(//
            "geometry_columns_auth", //
            "geometry_columns_field_infos", //
            "geometry_columns_statistics", //
            "sql_statements_log", //
            "sqlite_sequence",//
            "vector_layers_auth", //
            "vector_layers_field_infos", //
            "vector_layers_statistics", //
            "views_geometry_columns_auth", //
            "views_geometry_columns_field_infos", //
            "views_geometry_columns_statistics", //
            "virts_geometry_columns_auth", //
            "virts_geometry_columns_field_infos", //
            "virts_geometry_columns_statistics");

    public static HashMap<String, List<String>> getTablesSorted( List<String> allTableNames, boolean doSort ) {
        HashMap<String, List<String>> tablesMap = new HashMap<>();

        for( String tableName : allTableNames ) {
            if (tableName.equals(spatialIndex) || tableName.startsWith(startsWithIndexTables)) {
                List<String> list = tablesMap.get(SPATIALINDEX);
                if (list == null) {
                    list = new ArrayList<String>();
                    tablesMap.put(SPATIALINDEX, list);
                }
                list.add(tableName);
                continue;
            }
            if (tableName.startsWith(startsWithStyleTables)) {
                List<String> list = tablesMap.get(STYLE);
                if (list == null) {
                    list = new ArrayList<String>();
                    tablesMap.put(STYLE, list);
                }
                list.add(tableName);
                continue;
            }
            if (metadataTables.contains(tableName)) {
                List<String> list = tablesMap.get(METADATA);
                if (list == null) {
                    list = new ArrayList<String>();
                    tablesMap.put(METADATA, list);
                }
                list.add(tableName);
                continue;
            }
            if (internalDataTables.contains(tableName)) {
                List<String> list = tablesMap.get(INTERNALDATA);
                if (list == null) {
                    list = new ArrayList<String>();
                    tablesMap.put(INTERNALDATA, list);
                }
                list.add(tableName);
                continue;
            }
            List<String> list = tablesMap.get(USERDATA);
            if (list == null) {
                list = new ArrayList<String>();
                tablesMap.put(USERDATA, list);
            }
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
