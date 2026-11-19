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
package org.hortonmachine.gears.io.dbs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.datatypes.ESpatialiteGeometryType;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.utils.crs.CrsUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * Database spatial helper methods.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class DbsHelper {
    private static final Logger logger = Logger.INSTANCE;

    /**
     * Extractes a featurecollection from an sql statement.
     * 
     * <p>The assumption is made that the first string after the FROM
     * keyword in the select statement is the table that contains the geometry.
     * 
     * @param name the name of the resulting layer.
     * @param db
     * @param simpleSql the sql.
     * @param roi an optional region to limit the query to. 
     * @return the features.
     * @throws Exception
     */
    public static DefaultFeatureCollection runRawSqlToFeatureCollection( String name, ASpatialDb db, String simpleSql,
            Polygon roi ) throws Exception {
        int indexOf = indexOfMainFrom(simpleSql);
        if (indexOf < 0) {
            throw new RuntimeException("No FROM clause found in the query.");
        }
        String afterFrom = simpleSql.substring(indexOf + 4).trim();

        String tableName = extractFirstTableToken(afterFrom);

        if (tableName == null) {
            throw new RuntimeException("The geometry table name needs to be the first after the FROM keyword.");
        }

        GeometryColumn geometryColumns = db.getGeometryColumnsForTable(SqlName.m(tableName));
        if (geometryColumns == null) {
            throw new IllegalArgumentException("The supplied table name doesn't seem to be spatial: " + tableName);
        }
        if (geometryColumns.srid == 0) {
            geometryColumns.srid = 4326; // fallback with hope
        }
        String geomColumnName = geometryColumns.geometryColumnName;

        if (roi != null) {
            String where = db.getSpatialindexGeometryWherePiece(SqlName.m(tableName), null, roi);
            simpleSql += " where " + where;
        }

        String _simpleSql = simpleSql;
        return db.execOnConnection(connection -> {
            DefaultFeatureCollection fc = new DefaultFeatureCollection();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_simpleSql)) {
                IHMResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int geometryIndex = -1;

                CoordinateReferenceSystem crs = CrsUtilities.getCrsFromEpsg("EPSG:" + geometryColumns.srid);
                SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

                String _name = "shpexport";
                if (name != null)
                    _name = name;
                b.setName(_name);
                b.setCRS(crs);

                EDb eDb = db.getType();
                ADatabaseSyntaxHelper syntaxHelper = eDb.getDatabaseSyntaxHelper();
                IGeometryParser gp = eDb.getGeometryParser();

                for( int i = 1; i <= columnCount; i++ ) {
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    String columnName = rsmd.getColumnName(i);
                    if (geomColumnName.equalsIgnoreCase(columnName) || ESpatialiteGeometryType.isGeometryName(columnTypeName)) {
                        geometryIndex = i;
                        if (rs.next()) {
                            Geometry geometry = gp.fromResultSet(rs, geometryIndex);
                            b.add("the_geom", geometry.getClass());
                            break;
                        }
                    }
                }
                for( int i = 1; i <= columnCount; i++ ) {
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    String columnName = rsmd.getColumnName(i);
                    if (i != geometryIndex) {
                        if (columnName.toLowerCase().equals("id")) {
                            columnName = "origid";
                        }
                        if (columnName.length() > 9) {
                            columnName = columnName.substring(0, 9);
                        }
                        if ("INTEGER".equals(columnTypeName) || syntaxHelper.INTEGER().equals(columnTypeName)) {
                            b.add(columnName, Integer.class);
                        } else if (syntaxHelper.LONG().equals(columnTypeName)) {
                            b.add(columnName, Long.class);
                        } else if ("DOUBLE".equals(columnTypeName) || "FLOAT".equals(columnTypeName)
                                || syntaxHelper.REAL().equals(columnTypeName)) {
                            b.add(columnName, Double.class);
                        } else if ("DATE".equals(columnTypeName)) {
                            b.add(columnName, Date.class);
                        } else if (syntaxHelper.TEXT().equals(columnTypeName) || true) {
                            b.add(columnName, String.class);
                        }
                    }
                }

                SimpleFeatureType type = b.buildFeatureType();
                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
                do {
                    try {
                        List<Object> values = new ArrayList<>();
                        Geometry geometry = gp.fromResultSet(rs, geometryIndex);
                        values.add(geometry);

                        for( int j = 1; j <= columnCount; j++ ) {
                            if (j != geometryIndex) {
                                Object object = rs.getObject(j);
                                values.add(object);
                            }
                        }
                        builder.addAll(values);
                        SimpleFeature feature = builder.buildFeature(null);
                        fc.add(feature);
                    } catch (Exception e) {
                        logger.insertError("DbsHelper", "ERROR", e);
                    }
                } while( rs.next() );

            }
            return fc;
        });

    }

    /**
     * Find the FROM keyword of the main query: the first one outside parentheses, quotes and
     * comments. This skips the FROMs inside a WITH clause (CTE) or a subquery in the select
     * list, which do not name the table of the returned geometry.
     *
     * @param sql the query.
     * @return the index of the main FROM keyword, or -1 if there is none.
     */
    static int indexOfMainFrom( String sql ) {
        int depth = 0;
        int n = sql.length();
        for( int i = 0; i < n; i++ ) {
            char c = sql.charAt(i);
            if (c == '\'' || c == '"' || c == '[' || c == '`') {
                char close = c == '[' ? ']' : c;
                i++;
                while( i < n ) {
                    if (sql.charAt(i) == close) {
                        // a doubled closing char is an escaped one
                        if (i + 1 < n && sql.charAt(i + 1) == close) {
                            i++;
                        } else {
                            break;
                        }
                    }
                    i++;
                }
            } else if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                int eol = sql.indexOf('\n', i);
                i = eol < 0 ? n : eol;
            } else if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int end = sql.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 1;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && (c == 'f' || c == 'F') && sql.regionMatches(true, i, "from", 0, 4)) {
                boolean startsWord = i == 0 || !isIdentifierChar(sql.charAt(i - 1));
                boolean endsWord = i + 4 >= n || !isIdentifierChar(sql.charAt(i + 4));
                if (startsWord && endsWord) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isIdentifierChar( char c ) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    static String extractFirstTableToken( String afterFrom ) {
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean inBrackets = false;
        for( int i = 0; i < afterFrom.length(); i++ ) {
            char c = afterFrom.charAt(i);
            if (c == '\'' && !inDoubleQuotes && !inBrackets) {
                if (inSingleQuotes && i + 1 < afterFrom.length() && afterFrom.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }
                inSingleQuotes = !inSingleQuotes;
            } else if (c == '"' && !inSingleQuotes && !inBrackets) {
                if (inDoubleQuotes && i + 1 < afterFrom.length() && afterFrom.charAt(i + 1) == '"') {
                    i++;
                    continue;
                }
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '[' && !inSingleQuotes && !inDoubleQuotes) {
                inBrackets = true;
            } else if (c == ']' && inBrackets) {
                if (i + 1 < afterFrom.length() && afterFrom.charAt(i + 1) == ']') {
                    i++;
                    continue;
                }
                inBrackets = false;
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes && !inBrackets) {
                return afterFrom.substring(0, i);
            }
        }
        return afterFrom.isEmpty() ? null : afterFrom;
    }

}
