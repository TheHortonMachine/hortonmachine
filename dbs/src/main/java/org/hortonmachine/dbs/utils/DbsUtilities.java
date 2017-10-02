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
package org.hortonmachine.dbs.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.objects.TableLevel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * A simple utils class for the dbs module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DbsUtilities {

    public static final SimpleDateFormat dbDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static GeometryFactory geomFactory;
    private static PrecisionModel precModel;

    public static PrecisionModel basicPrecisionModel() {
        return (pm());
    }

    public static GeometryFactory gf() {
        if (geomFactory == null) {
            geomFactory = new GeometryFactory(pm());
        }
        return (geomFactory);
    }

    public static PrecisionModel pm() {
        if (precModel == null) {
            precModel = new PrecisionModel(PrecisionModel.FLOATING);
        }
        return (precModel);
    }

    /**
     * Join a list of strings by comma.
     * 
     * @param items the list of strings.
     * @return the resulting string.
     */
    public static String joinByComma( List<String> items ) {
        StringBuilder sb = new StringBuilder();
        for( String item : items ) {
            sb.append(",").append(item);
        }
        if (sb.length() == 0) {
            return "";
        }
        return sb.substring(1);
    }

    /**
     * Create a polygon using an envelope.
     * 
     * @param env the envelope to use.
     * @return the created geomerty.
     */
    public static Polygon createPolygonFromEnvelope( Envelope env ) {
        double minX = env.getMinX();
        double minY = env.getMinY();
        double maxY = env.getMaxY();
        double maxX = env.getMaxX();
        Coordinate[] c = new Coordinate[]{new Coordinate(minX, minY), new Coordinate(minX, maxY), new Coordinate(maxX, maxY),
                new Coordinate(maxX, minY), new Coordinate(minX, minY)};
        return gf().createPolygon(c);
    }

    /**
     * Create a polygon using boundaries.
     * 
     * @param minX the min x.
     * @param minY the min y.
     * @param maxX the max x.
     * @param maxY the max y.
     * @return the created geomerty.
     */
    public static Polygon createPolygonFromBounds( double minX, double minY, double maxX, double maxY ) {
        Coordinate[] c = new Coordinate[]{new Coordinate(minX, minY), new Coordinate(minX, maxY), new Coordinate(maxX, maxY),
                new Coordinate(maxX, minY), new Coordinate(minX, minY)};
        return gf().createPolygon(c);
    }

    public static byte[] hexStringToByteArray( String s ) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for( int i = 0; i < len; i += 2 ) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Get a full select query from a table in the db.
     * 
     * @param db the db.
     * @param selectedTable the table to create the query for.
     * @param geomFirst if <code>true</code>, the geometry is places first.
     * @return the query.
     * @throws Exception
     */
    public static String getSelectQuery( ASpatialDb db, final TableLevel selectedTable, boolean geomFirst ) throws Exception {
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
