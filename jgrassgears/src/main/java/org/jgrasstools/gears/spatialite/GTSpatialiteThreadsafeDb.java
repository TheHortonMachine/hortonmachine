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
package org.jgrasstools.gears.spatialite;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.dbs.compat.GeometryColumn;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.spatialite.ESpatialiteGeometryType;
import org.jgrasstools.dbs.spatialite.SpatialiteWKBReader;
import org.jgrasstools.dbs.spatialite.jgt.SpatialiteThreadsafeDb;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

/**
 * A spatialite database threadsafe on writing (see package javadoc for more info).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GTSpatialiteThreadsafeDb extends SpatialiteThreadsafeDb {
    private static final Logger logger = LoggerFactory.getLogger(GTSpatialiteThreadsafeDb.class);

    /**
     * Extractes a featurecollection from an sql statement.
     * 
     * <p>The assumption is made that the first string after the FROM
     * keyword in the select statement is the table that contains the geometry.
     * 
     * @param simpleSql the sql.
     * @return the features.
     * @throws Exception
     */
    public DefaultFeatureCollection runRawSqlToFeatureCollection( String simpleSql ) throws Exception {
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

        GeometryColumn geometryColumns = getGeometryColumnsForTable(tableName);
        if (geometryColumns == null) {
            throw new IllegalArgumentException("The supplied table name doesn't seem to be spatial: " + tableName);
        }
        String geomColumnName = geometryColumns.geometryColumnName;

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(simpleSql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int geometryIndex = -1;

            CoordinateReferenceSystem crs = CrsUtilities.getCrsFromEpsg("EPSG:" + geometryColumns.srid);
            ESpatialiteGeometryType geomType = ESpatialiteGeometryType.forValue(geometryColumns.geometryType);

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("sql");
            b.setCRS(crs);

            for( int i = 1; i <= columnCount; i++ ) {
                int columnType = rsmd.getColumnType(i);
                String columnTypeName = rsmd.getColumnTypeName(i);
                String columnName = rsmd.getColumnName(i);

                if (geomColumnName.equalsIgnoreCase(columnName)
                        || (geomType != null && columnType > 999 && columnTypeName.toLowerCase().equals("blob"))) {
                    geometryIndex = i;

                    if (rs.next()) {
                        byte[] geomBytes = rs.getBytes(geometryIndex);
                        Geometry geometry = wkbReader.read(geomBytes);
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
                            byte[] geomBytes = rs.getBytes(j);
                            Geometry geometry = wkbReader.read(geomBytes);
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

    @Override
    public ReferencedEnvelope getTableBounds( String tableName ) throws Exception {
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        String geomFieldName = gCol.geometryColumnName;

        int srid = gCol.srid;
        CoordinateReferenceSystem crs = CrsUtilities.getCrsFromSrid(srid);

        String trySql = "SELECT extent_min_x, extent_min_y, extent_max_x, extent_max_y FROM vector_layers_statistics WHERE table_name='"
                + tableName + "' AND geometry_column='" + geomFieldName + "'";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(trySql)) {
            if (rs.next()) {
                double minX = rs.getDouble(1);
                double minY = rs.getDouble(2);
                double maxX = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                ReferencedEnvelope env = new ReferencedEnvelope(minX, maxX, minY, maxY, crs);
                if (env.getWidth() != 0.0 && env.getHeight() != 0.0) {
                    return env;
                }
            }
        }

        // OR DO FULL GEOMETRIES SCAN

        String sql = "SELECT Min(MbrMinX(" + geomFieldName + ")) AS min_x, Min(MbrMinY(" + geomFieldName + ")) AS min_y,"
                + "Max(MbrMaxX(" + geomFieldName + ")) AS max_x, Max(MbrMaxY(" + geomFieldName + ")) AS max_y " + "FROM "
                + tableName;

        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                double minX = rs.getDouble(1);
                double minY = rs.getDouble(2);
                double maxX = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                ReferencedEnvelope env = new ReferencedEnvelope(minX, maxX, minY, maxY, crs);
                return env;
            }
            return null;
        }
    }

    /**
     * Execute a insert/update sql file. 
     * 
     * @param file the file to run on this db.
     * @param chunks commit interval.
     * @throws Exception 
     */
    public void executeSqlFile( File file, int chunks, boolean eachLineAnSql ) throws Exception {
        boolean autoCommit = mConn.getAutoCommit();
        mConn.setAutoCommit(false);

        Predicate<String> validSqlLine = s -> s.length() != 0 //
                && !s.startsWith("BEGIN") //
                && !s.startsWith("COMMIT") //
        ;
        Predicate<String> commentPredicate = s -> !s.startsWith("--");

        try (IJGTStatement pStmt = mConn.createStatement()) {
            final int[] counter = {1};
            Stream<String> linesStream = null;
            if (eachLineAnSql) {
                linesStream = Files.lines(Paths.get(file.getAbsolutePath())).map(s -> s.trim()).filter(commentPredicate)
                        .filter(validSqlLine);
            } else {
                linesStream = Arrays.stream(Files.lines(Paths.get(file.getAbsolutePath())).filter(commentPredicate)
                        .collect(Collectors.joining()).split(";")).filter(validSqlLine);
            }

            Consumer<String> executeAction = s -> {
                try {
                    pStmt.executeUpdate(s);
                    counter[0]++;
                    if (counter[0] % chunks == 0) {
                        mConn.commit();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            linesStream.forEach(executeAction);
            mConn.commit();
        }
        mConn.setAutoCommit(autoCommit);
    }

}
