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
package org.hortonmachine.gears.spatialite;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.spatialite.hm.SpatialiteThreadsafeDb;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A spatialite database threadsafe on writing (see package javadoc for more info).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GTSpatialiteThreadsafeDb extends SpatialiteThreadsafeDb {

    @Override
    public ReferencedEnvelope getTableBounds( String tableName ) throws Exception {
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        String geomFieldName = gCol.geometryColumnName;

        int srid = gCol.srid;
        CoordinateReferenceSystem crs = CrsUtilities.getCrsFromSrid(srid);

        String trySql = "SELECT extent_min_x, extent_min_y, extent_max_x, extent_max_y FROM vector_layers_statistics WHERE table_name='"
                + tableName + "' AND geometry_column='" + geomFieldName + "'";

        ReferencedEnvelope resEnv = execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(trySql)) {
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
                return null;
            }
        });

        if (resEnv != null) {
            return resEnv;
        }
        // OR DO FULL GEOMETRIES SCAN

        String sql = "SELECT Min(MbrMinX(" + geomFieldName + ")) AS min_x, Min(MbrMinY(" + geomFieldName + ")) AS min_y,"
                + "Max(MbrMaxX(" + geomFieldName + ")) AS max_x, Max(MbrMaxY(" + geomFieldName + ")) AS max_y " + "FROM "
                + tableName;

        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
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
        });

    }

    /**
     * Execute a insert/update sql file. 
     * 
     * @param file the file to run on this db.
     * @param chunks commit interval.
     * @throws Exception 
     */
    public void executeSqlFile( File file, int chunks, boolean eachLineAnSql ) throws Exception {
        execOnConnection(mConn -> {
            boolean autoCommit = mConn.getAutoCommit();
            mConn.setAutoCommit(false);

            Predicate<String> validSqlLine = s -> s.length() != 0 //
                    && !s.startsWith("BEGIN") //
                    && !s.startsWith("COMMIT") //
            ;
            Predicate<String> commentPredicate = s -> !s.startsWith("--");

            try (IHMStatement pStmt = mConn.createStatement()) {
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

            return null;
        });

    }

}
