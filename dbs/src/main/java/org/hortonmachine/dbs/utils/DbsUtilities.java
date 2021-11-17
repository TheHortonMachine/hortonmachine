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

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.Preferences;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * A simple utils class for the dbs module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DbsUtilities {
    /**
     * Default insert size used for bulk inserts/updates.
     */
    public static final int DEFAULT_BULK_INSERT_CHUNK_SIZE = 10000;

    private static final String PREFS_NODE_NAME = "HM_DBS_TOOLS";
    public static final String SPATIALITE_DYLIB_FOLDER = "SPATIALITE_DYLIB_FOLDER";

    public static List<String> reserverSqlWords = Arrays.asList("ABORT", "ACTION", "ADD", "AFTER", "ALL", "ALTER", "ANALYZE",
            "AND", "AS", "ASC", "ATTACH", "AUTOINCREMENT", "BEFORE", "BEGIN", "BETWEEN", "BY", "CASCADE", "CASE", "CAST", "CHECK",
            "COLLATE", "COLUMN", "COMMIT", "CONFLICT", "CONSTRAINT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME",
            "CURRENT_TIMESTAMP", "DATABASE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE", "DESC", "DETACH", "DISTINCT", "DROP",
            "EACH", "ELSE", "END", "ESCAPE", "EXCEPT", "EXCLUSIVE", "EXISTS", "EXPLAIN", "FAIL", "FOR", "FOREIGN", "FROM", "FULL",
            "GLOB", "GROUP", "HAVING", "IF", "IGNORE", "IMMEDIATE", "IN", "INDEX", "INDEXED", "INITIALLY", "INNER", "INSERT",
            "INSTEAD", "INTERSECT", "INTO", "IS", "ISNULL", "JOIN", "LEFT", "LIKE", "LIMIT", "MATCH", "NATURAL", "NO", "NOT",
            "NOTNULL", "NULL", "OF", "OFFSET", "ON", "OR", "ORDER", "OUTER", "PLAN", "PRAGMA", "PRIMARY", "QUERY", "RAISE",
            "RECURSIVE", "REFERENCES", "REGEXP", "REINDEX", "RELEASE", "RENAME", "REPLACE", "RESTRICT", "RIGHT", "ROLLBACK",
            "ROW", "SAVEPOINT", "SELECT", "SET", "TABLE", "TEMP", "TEMPORARY", "THEN", "TO", "TRANSACTION", "TRIGGER", "UNION",
            "UNIQUE", "UPDATE", "USING", "VACUUM", "VALUES", "VIEW", "VIRTUAL", "WHEN", "WHERE", "WITH", "WITHOUT");

    public static final SimpleDateFormat dbDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final SimpleDateFormat SQLITE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat SQLITE_DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
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

    public static boolean isReservedName( String name ) {
        return reserverSqlWords.indexOf(name.toUpperCase()) != -1;
    }

    public static String fixReservedNameForQuery( String name ) {
        int index = name.indexOf('.');
        if (index == -1) {
            return "[" + name + "]";
        } else {
            String alias = name.substring(0, index + 1);
            String tname = name.substring(index + 1);
            return alias + "[" + tname + "]";
        }
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
     * Join a list of strings by string.
     * 
     * @param items the list of strings.
     * @param separator the separator to use.
     * @return the resulting string.
     */
    public static String joinBySeparator( List<String> items, String separator ) {
        int size = items.size();
        if (size == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(items.get(0));
        for( int i = 1; i < size; i++ ) {
            sb.append(separator).append(items.get(i));
        }
        return sb.toString();
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
    public static String getSelectQuery( ADb db, final TableLevel selectedTable, boolean geomFirst ) throws Exception {
        String tableName = selectedTable.tableName;
        String letter = "tbl";
        if (!Character.isDigit(tableName.charAt(0))) {
            letter = tableName.substring(0, 1);
        }
        List<String[]> tableColumns = db.getTableColumns(tableName);
        GeometryColumn geometryColumns = null;
        try {
            if (db instanceof ASpatialDb) {
                geometryColumns = ((ASpatialDb) db).getGeometryColumnsForTable(tableName);
            }
        } catch (Exception e) {
            // ignore
        }

        EDb type = db.getType();
        boolean isSpatialite = type == EDb.SPATIALITE || type == EDb.SPATIALITE4ANDROID;

        String query = "SELECT ";
        if (geomFirst) {
            // first geom
            List<String> nonGeomCols = new ArrayList<String>();
            for( int i = 0; i < tableColumns.size(); i++ ) {
                String colName = tableColumns.get(i)[0];
                if (DbsUtilities.isReservedName(colName)) {
                    colName = DbsUtilities.fixReservedNameForQuery(colName);
                }
                if (geometryColumns != null && colName.equalsIgnoreCase(geometryColumns.geometryColumnName)) {
                    if (!isSpatialite) {
                        colName = letter + "." + colName + " as " + colName;
                    } else {
                        colName = "ST_asBinary(" + letter + "." + colName + ") as " + colName;
                    }
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
                if (DbsUtilities.isReservedName(colName)) {
                    colName = DbsUtilities.fixReservedNameForQuery(colName);
                }
                if (geometryColumns != null && colName.equalsIgnoreCase(geometryColumns.geometryColumnName)) {
                    if (!isSpatialite) {
                        colName = letter + "." + colName + " as " + colName;
                    } else {
                        colName = "ST_asBinary(" + letter + "." + colName + ") as " + colName;
                    }
                    query += colName;
                } else {
                    query += letter + "." + colName;
                }
            }
        }
        query += " FROM " + fixTableName(tableName) + " " + letter;
        return query;
    }

    /**
     * Returns the name of the file without the extension.
     * <p/>
     * <p>Note that if the file has no extension, the name is returned.
     *
     * @param file the file to trim.
     *
     * @return the name without extension.
     */
    public static String getNameWithoutExtention( File file ) {
        String name = file.getName();
        int lastDot = name.lastIndexOf("."); //$NON-NLS-1$
        if (lastDot == -1) {
            // file has no extension, return the name
            return name;
        }
        name = name.substring(0, lastDot);
        return name;
    }

    /**
     * Quick method to convert a query to a map.
     * 
     * @param db the db to use.
     * @param sql the query to run. It has to have at least 2 parameters. The first will be used as key, the second as value.
     * @param optionalType can be null. Optional parameter in case one needs a {@link TreeMap} or something the like.
     * @return the map of values from the query.
     * @throws Exception
     */
    public static Map<String, String> queryToMap( ADb db, String sql, Map<String, String> optionalType ) throws Exception {
        Map<String, String> map = optionalType;
        if (map == null) {
            map = new HashMap<>();
        }
        Map<String, String> _map = map;
        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String key = rs.getObject(1).toString();
                    String value = rs.getObject(2).toString();
                    _map.put(key, value);
                }
                return _map;
            }
        });
    }

    /**
     * Quick method to convert a query to a map with geometry values.
     * 
     * @param db the db to use.
     * @param sql the query to run. It has to have at least 2 parameters, of which the second has to be a geometry field. The first will be used as key, the second as value.
     * @param optionalType can be null. Optional parameter in case one needs a {@link TreeMap} or something the like.
     * @return the map of values from the query.
     * @throws Exception
     */
    public static Map<String, Geometry> queryToGeomMap( ADb db, String sql, Map<String, Geometry> optionalType )
            throws Exception {
        Map<String, Geometry> map = optionalType;
        if (map == null) {
            map = new HashMap<>();
        }
        IGeometryParser gp = db.getType().getGeometryParser();
        Map<String, Geometry> _map = map;
        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String key = rs.getObject(1).toString();
                    Geometry geometry = gp.fromResultSet(rs, 2);
                    _map.put(key, geometry);
                }
                return _map;
            }
        });
    }

    /**
     * Extract strings form a stream.
     * 
     * @param stream the stream.
     * @param delimiter the delimiter used to split. If <code>null</code>, newline is used.
     * @return the list of string pieces.
     */
    public static List<String> streamToStringList( InputStream stream, String delimiter ) {
        if (delimiter == null) {
            delimiter = "\n";
        }
        List<String> pieces = new ArrayList<>();
        try (java.util.Scanner scanner = new java.util.Scanner(stream)) {
            scanner.useDelimiter(delimiter);
            while( scanner.hasNext() ) {
                pieces.add(scanner.next());
            }
        }
        return pieces;
    }

    /**
     * Check the tablename and fix it if necessary.
     * 
     * @param tableName the name to check.
     * @return the fixed name.
     */
    public static String fixTableName( String tableName ) {
        if (tableName.charAt(0) == '\'') {
            // already fixed
            return tableName;
        }
        if ( //
        Character.isDigit(tableName.charAt(0)) || //
                tableName.matches(".*\\s+.*") || //
                tableName.contains("-") //
        ) {
            return "'" + tableName + "'";
        }
        return tableName;
    }

    public static String fixColumnName( String columnName ) {
        if (columnName.charAt(0) == '\'') {
            // already fixed
            return columnName;
        }
        if ( //
        Character.isDigit(columnName.charAt(0)) || //
                columnName.matches(".*\\s+.*") || //
                columnName.contains("-") //
        ) {
            return "\"" + columnName + "\"";
        }
        return columnName;
    }
    
    /**
     * Get from preference.
     * 
     * @param preferenceKey
     *            the preference key.
     * @param defaultValue
     *            the default value in case of <code>null</code>.
     * @return the string preference asked.
     */
    public static String getPreference( String preferenceKey, String defaultValue ) {
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        String preference = preferences.get(preferenceKey, defaultValue);
        return preference;
    }

    /**
     * Set a preference.
     * 
     * @param preferenceKey
     *            the preference key.
     * @param value
     *            the value to set.
     */
    public static void setPreference( String preferenceKey, String value ) {
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        if (value != null) {
            preferences.put(preferenceKey, value);
        } else {
            preferences.remove(preferenceKey);
        }
    }

    /**
     * Get only the alphanumeric field names from a table. 
     * 
     * @param db the database.
     * @param tableName the name of the table to check.
     * @return the list of names.
     * @throws Exception
     */
    public static List<String> getTableAlphanumericFields( ADb db, String tableName ) throws Exception {
        List<String[]> tableColumns = db.getTableColumns(tableName);
        List<String> names = new ArrayList<>();
        for( String[] item : tableColumns ) {
            String name = item[0];
            String type = item[1];

            EGeometryType geomType = EGeometryType.forTypeName(type);
            if (geomType == EGeometryType.UNKNOWN) {
                names.add(name);
            }
        }
        return names;
    }
}
