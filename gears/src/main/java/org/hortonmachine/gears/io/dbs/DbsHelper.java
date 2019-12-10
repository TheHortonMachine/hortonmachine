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
import org.hortonmachine.gears.utils.CrsUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Database spatial helper methods.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
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
        int indexOf = simpleSql.toLowerCase().indexOf("from");
        String afterFrom = simpleSql.substring(indexOf + 5).trim();

        String tableName = null;
        if (afterFrom.startsWith("'")) {
            int nextAp = afterFrom.indexOf("'", 1) + 1;
            tableName = afterFrom.substring(0, nextAp);
        } else {
            int nextSpace = afterFrom.indexOf(' ');
            tableName = afterFrom.substring(0, nextSpace);
        }

        if (tableName == null) {
            throw new RuntimeException("The geometry table name needs to be the first after the FROM keyword.");
        }

        GeometryColumn geometryColumns = db.getGeometryColumnsForTable(tableName);
        if (geometryColumns == null) {
            throw new IllegalArgumentException("The supplied table name doesn't seem to be spatial: " + tableName);
        }
        if (geometryColumns.srid == 0) {
            geometryColumns.srid = 4326; // fallback with hope
        }
        String geomColumnName = geometryColumns.geometryColumnName;

        if (roi != null) {
            String where = db.getSpatialindexGeometryWherePiece(tableName, null, roi);
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

}
