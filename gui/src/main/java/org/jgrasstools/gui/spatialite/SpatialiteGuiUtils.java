package org.jgrasstools.gui.spatialite;

import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jgrasstools.gears.spatialite.QueryResult;
import org.jgrasstools.gears.spatialite.SpatialiteDb;
import org.jgrasstools.gears.spatialite.SpatialiteGeometryColumns;
import org.jgrasstools.gui.spatialite.objects.ColumnLevel;
import org.jgrasstools.gui.spatialite.objects.TableLevel;


public class SpatialiteGuiUtils {

    public static String getSelectQuery( SpatialiteDb db, final TableLevel selectedTable, boolean geomFirst )
            throws SQLException {
        String tableName = selectedTable.tableName;
        String letter = tableName.substring(0, 1);
        List<String[]> tableColumns = db.getTableColumns(tableName);
        SpatialiteGeometryColumns geometryColumns = db.getGeometryColumnsForTable(tableName);
        String query = "SELECT ";
        if (geomFirst) {
            // first geom
            List<String> nonGeomCols = new ArrayList<String>();
            for( int i = 0; i < tableColumns.size(); i++ ) {
                String colName = tableColumns.get(i)[0];
                if (geometryColumns != null && colName.equals(geometryColumns.f_geometry_column)) {
                    colName = "ST_AsBinary(" + letter + "." + colName + ") as " + colName;
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
                if (geometryColumns != null && colName.equals(geometryColumns.f_geometry_column)) {
                    colName = "ST_AsBinary(" + letter + "." + colName + ") as " + colName;
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
