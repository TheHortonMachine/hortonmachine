/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.io.dbs;

import java.util.Date;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.GeometryColumn;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.spatialite.ESpatialiteGeometryType;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Database spatial helper methods.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DbsHelper {
    private static final Logger logger = LoggerFactory.getLogger(DbsHelper.class);

    /**
     * Extractes a featurecollection from an sql statement.
     * 
     * <p>The assumption is made that the first string after the FROM
     * keyword in the select statement is the table that contains the geometry.
     * 
     * @param name the name of the resulting layer.
     * @param db
     * @param simpleSql the sql.
     * @return the features.
     * @throws Exception
     */
    public static DefaultFeatureCollection runRawSqlToFeatureCollection( String name, ASpatialDb db, String simpleSql )
            throws Exception {
        String[] split = simpleSql.split("\\s+");
        String tableName = null;
        for( int i = 0; i < split.length; i++ ) {
            if (split[i].toLowerCase().equals("from")) {
                tableName = split[i + 1];
                break;
            }
        }

        if (tableName == null) {
            throw new RuntimeException("The geometry table name needs to be the first after the FROM keyword.");
        }

        GeometryColumn geometryColumns = db.getGeometryColumnsForTable(tableName);
        if (geometryColumns == null) {
            throw new IllegalArgumentException("The supplied table name doesn't seem to be spatial: " + tableName);
        }
        String geomColumnName = geometryColumns.geometryColumnName;

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        try (IJGTStatement stmt = db.getConnection().createStatement(); IJGTResultSet rs = stmt.executeQuery(simpleSql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int geometryIndex = -1;

            CoordinateReferenceSystem crs = CrsUtilities.getCrsFromEpsg("EPSG:" + geometryColumns.srid);
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            if (name != null)
                b.setName(name);
            b.setCRS(crs);

            for( int i = 1; i <= columnCount; i++ ) {
                String columnTypeName = rsmd.getColumnTypeName(i);
                String columnName = rsmd.getColumnName(i);
                if (geomColumnName.equalsIgnoreCase(columnName) || ESpatialiteGeometryType.isGeometryName(columnTypeName)) {
                    geometryIndex = i;

                    if (rs.next()) {
                        Geometry geometry = db.getGeometryFromResultSet(rs, geometryIndex);
                        b.add("the_geom", geometry.getClass());
                    }

                } else {
                    // Class< ? > forName = Class.forName(columnClassName);
                    switch( columnTypeName ) {
                    case "INTEGER":
                        b.add(columnName, Integer.class);
                        break;
                    case "DOUBLE":
                    case "FLOAT":
                    case "REAL":
                        b.add(columnName, Double.class);
                        break;
                    case "DATE":
                        b.add(columnName, Date.class);
                        break;
                    case "TEXT":
                    default:
                        b.add(columnName, String.class);
                        break;
                    }
                }
            }

            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            do {

                try {
                    Object[] values = new Object[columnCount];
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (j == geometryIndex) {
                            Geometry geometry = db.getGeometryFromResultSet(rs, j);
                            values[j - 1] = geometry;
                        } else {
                            Object object = rs.getObject(j);
                            if (object != null) {
                                values[j - 1] = object;
                            }
                        }
                    }
                    builder.addAll(values);
                    SimpleFeature feature = builder.buildFeature(null);
                    fc.add(feature);
                } catch (Exception e) {
                    logger.error("ERROR", e);
                }
            } while( rs.next() );

        }
        return fc;
    }

}
