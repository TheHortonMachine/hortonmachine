package org.jgrasstools.spatialite;

import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.dbs.compat.GeometryColumn;
import org.jgrasstools.dbs.compat.objects.TableLevel;
import org.jgrasstools.dbs.spatialite.SpatialiteGeometryColumns;
import org.jgrasstools.dbs.spatialite.jgt.SpatialiteDb;

public class SpatialiteGuiUtils {

    public static final String JGT_SPATIALITE_LAST_FILE = "jgt-spatialite-last-file";

    public static String getSelectQuery( SpatialiteDb db, final TableLevel selectedTable, boolean geomFirst ) throws Exception {
        String tableName = selectedTable.tableName;
        String letter = tableName.substring(0, 1);
        List<String[]> tableColumns = db.getTableColumns(tableName);
        GeometryColumn geometryColumns = db.getGeometryColumnsForTable(tableName);
        String query = "SELECT ";
        if (geomFirst) {
            // first geom
            List<String> nonGeomCols = new ArrayList<String>();
            for( int i = 0; i < tableColumns.size(); i++ ) {
                String colName = tableColumns.get(i)[0];
                if (geometryColumns != null && colName.equals(geometryColumns.geometryColumnName)) {
                    colName = letter + "." + colName + " as " + colName;
                    query += colName;
                } else {
                    nonGeomCols.add(colName);
                }
            }
            // then others
            for( int i = 0; i < nonGeomCols.size(); i++ ) {
                String colName = tableColumns.get(i)[0];
                query += "," + letter + "." + colName;
            }
        } else {
            for( int i = 0; i < tableColumns.size(); i++ ) {
                if (i > 0)
                    query += ",";
                String colName = tableColumns.get(i)[0];
                if (geometryColumns != null && colName.equals(geometryColumns.geometryColumnName)) {
                    colName = letter + "." + colName + " as " + colName;
                    query += colName;
                } else {
                    query += letter + "." + colName;
                }
            }
        }
        query += " FROM " + tableName + " " + letter;
        return query;
    }

}
