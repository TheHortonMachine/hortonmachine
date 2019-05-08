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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.ISpatialTableNames;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.DbLevel;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.postgis.PostgisDb;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * A class to make a migration from Spatialite to Postgis.
 * 
 * <p><b>THIS CLASS IS A VERY VERY SIMPLE SOLUTION AND BY NO MEANS THOUGHT FOR 
 * PRODUCTION USE.</b>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class Spatialite2Postgis implements AutoCloseable {

    private PostgisDb postgis;
    private ASpatialDb spatialite;
    private List<TableLevel> finalDoneOrder;

    private GeometryFactory gf = new GeometryFactory();

    public static void main( String[] args ) throws Exception {
        try (Spatialite2Postgis conv = new Spatialite2Postgis("path/to.sqlite", "localhost:5432/database", "", "")) {
            conv.generateSchema();
            conv.copyData();
        }
    }

    public Spatialite2Postgis( String spatialitePath, String postgisUrl, String user, String pwd ) throws Exception {
        spatialite = EDb.SPATIALITE.getSpatialDb();
        spatialite.open(spatialitePath);

        postgis = (PostgisDb) EDb.POSTGIS.getSpatialDb();
        postgis.setCredentials(user, pwd);
        postgis.open(postgisUrl);
        postgis.initSpatialMetadata(null);
    }

    public Spatialite2Postgis( ASpatialDb spatialite, ASpatialDb postgis ) {
        this.spatialite = spatialite;
        this.postgis = (PostgisDb) postgis;
    }

    @Override
    public void close() throws Exception {
        spatialite.close();
        postgis.close();
    }

    public PostgisDb getPostgisDb() {
        return postgis;
    }

    public ASpatialDb getSpatialiteDb() {
        return spatialite;
    }

    public void generateSchema() throws Exception {

        List<TableLevel> tablesList = getTables();

        System.out.println("Order of creation:");
        for( TableLevel tableLevel : tablesList ) {
            System.out.println("->" + tableLevel.tableName);
        }

        List<TableLevel> todo = new ArrayList<>(tablesList);
        List<TableLevel> nextTodo = new ArrayList<>();
        finalDoneOrder = new ArrayList<>();
        int count = 0;
        ADatabaseSyntaxHelper dsh = postgis.getType().getDatabaseSyntaxHelper();
        while( todo.size() > 0 && count < 10 ) {
            for( TableLevel tableLevel : todo ) {
                String tableName = tableLevel.tableName;
                String tableSql = getTableSql(spatialite, tableName);

                System.out.println("Trying to create table: " + tableName);
                String tableSqlChecked = postgis.getType().getDatabaseSyntaxHelper().checkSqlCompatibilityIssues(tableSql);

                /*
                 *  dirty hack: in my db spatialite had integers where long should be
                 *  since spatialite is dynamic in this. In postgis we need longs.
                 */
                tableSqlChecked = tableSqlChecked.replaceAll("INTEGER", dsh.LONG());
                tableSqlChecked = tableSqlChecked.replaceAll("LONG", dsh.LONG());
                tableSqlChecked = tableSqlChecked.replaceAll("DOUBLE", dsh.REAL());
                tableSqlChecked = tableSqlChecked.replaceAll("LINESTRING", "lseg");
                tableSqlChecked = tableSqlChecked.replaceAll("MULTIPOLYGON", "polygon");
                // the the_geom field comes with "" from spatialite
                tableSqlChecked = tableSqlChecked.replaceAll("\"", "");

                try {
                    postgis.executeInsertUpdateDeleteSql(tableSqlChecked);
                    if (tableLevel.isGeo) {
                        GeometryColumn geometryColumn = spatialite.getGeometryColumnsForTable(tableName);
                        try {
                            String columnName = geometryColumn.geometryColumnName;
                            String drop = "ALTER TABLE " + tableName + " DROP COLUMN " + columnName + ";";
                            postgis.executeInsertUpdateDeleteSql(drop);
                            // add spatial
                            EGeometryType type = geometryColumn.geometryType; // TODO
                                                                              // EGeometryType.fromSpatialiteCode(geometryColumn.geometryType);
                            postgis.addGeometryXYColumnAndIndex(tableName, columnName, type.name(), geometryColumn.srid + "",
                                    false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    List<String> indexSqls = getIndexSqls(spatialite, tableName);
                    for( String indexSql : indexSqls ) {
                        if (indexSql == null) {
                            continue;
                        }
                        postgis.executeInsertUpdateDeleteSql(indexSql);
                    }

                    finalDoneOrder.add(tableLevel);
                    System.out.println("Created table: " + tableName);
                } catch (Exception e) {
                    System.out.println("Trying again later for table: " + tableName + " -> " + e.getMessage());
                    e.printStackTrace();
                    // try later in inverse order
                    nextTodo.add(0, tableLevel);
                }
            }
            todo.clear();
            todo.addAll(nextTodo);
            nextTodo.clear();
            count++;
            System.out.println("*************");
        }

    }

    private List<TableLevel> getTables() throws Exception {
        DbLevel dbLevel = DbLevel.getDbLevel(spatialite, ISpatialTableNames.USERDATA);
        List<TableLevel> tablesList = dbLevel.typesList.get(0).tablesList;

        Collections.sort(tablesList, new Comparator<TableLevel>(){
            @Override
            public int compare( TableLevel o1, TableLevel o2 ) {
                if (o1.hasFks()) {
                    List<ColumnLevel> columnsList = o1.columnsList;
                    for( ColumnLevel col : columnsList ) {
                        if (col.references != null) {
                            String refTable = col.tableColsFromFK()[0];
                            if (o2.tableName.equalsIgnoreCase(refTable)) {
                                // linked, create other first
                                return -1;
                            }
                        }
                    }
                    if (!o2.hasFks()) {
                        return 1;
                    }
                }
                if (o2.hasFks()) {
                    List<ColumnLevel> columnsList = o2.columnsList;
                    for( ColumnLevel col : columnsList ) {
                        if (col.references != null) {
                            String refTable = col.tableColsFromFK()[0];
                            if (o1.tableName.equalsIgnoreCase(refTable)) {
                                // linked, create other first
                                return -1;
                            }
                        }
                    }
                    if (!o1.hasFks()) {
                        return -1;
                    }
                }

                return 0;
            }
        });
        return tablesList;
    }

    public void copyData() throws Exception {
        List<TableLevel> tablesList = getTables();
        if (finalDoneOrder != null) {
            tablesList = finalDoneOrder;
        }
        for( TableLevel tableLevel : tablesList ) {
            String tableName = tableLevel.tableName;
            System.out.println("Copy table " + tableName);
            System.out.println("Read data...");
            QueryResult queryResult = spatialite.getTableRecordsMapFromRawSql("select * from " + tableName, -1);
            System.out.println("Done.");

            System.out.println("Insert data...");
            int geometryIndex = queryResult.geometryIndex;

            List<String> names = queryResult.names;
            StringBuilder namesSb = new StringBuilder();
            StringBuilder qmSb = new StringBuilder();
            GeometryColumn gCol = null;
            for( int i = 0; i < names.size(); i++ ) {
                namesSb.append(",").append(names.get(i));

                if (i == geometryIndex) {
                    gCol = spatialite.getGeometryColumnsForTable(tableName);
                    qmSb.append(",ST_GeomFromText(?, " + gCol.srid + ")");
                } else {
                    qmSb.append(",?");
                }
            }
            String namesStr = namesSb.substring(1);
            String qmStr = qmSb.substring(1);
            String prepared = "insert into " + tableName + " (" + namesStr + ") values (" + qmStr + ");";

            GeometryColumn _gCol = gCol;
            postgis.execOnConnection(connection -> {
                String emptyGeomStr = null;
                try (IHMPreparedStatement stmt = connection.prepareStatement(prepared)) {

                    long count = 0;
                    for( Object[] objects : queryResult.data ) {
                        for( int i = 0; i < objects.length; i++ ) {
                            if (i == geometryIndex) {
                                if (objects[i] == null) {

                                    if (emptyGeomStr == null) {
                                        EGeometryType gType = _gCol.geometryType;
//                                        TODO check ESpatialiteGeometryType gType = ESpatialiteGeometryType.forValue(geometryType);
                                        if (gType.isLine()) {
                                            if (gType.isMulti()) {
                                                emptyGeomStr = gf.createLineString((CoordinateSequence) null).toText();
                                            } else {
                                                emptyGeomStr = gf.createMultiLineString(null).toText();
                                            }
                                        } else if (gType.isPoint()) {
                                            if (gType.isMulti()) {
                                                emptyGeomStr = gf.createMultiPoint((CoordinateSequence) null).toText();
                                            } else {
                                                emptyGeomStr = gf.createPoint((Coordinate) null).toText();
                                            }
                                        } else if (gType.isPolygon()) {
                                            if (gType.isMulti()) {
                                                emptyGeomStr = gf.createMultiPolygon(null).toText();
                                            } else {
                                                emptyGeomStr = gf.createPolygon((CoordinateSequence) null).toText();
                                            }
                                        }
                                    }
                                    stmt.setString(i + 1, emptyGeomStr);
                                } else {
                                    stmt.setString(i + 1, (String) objects[i].toString());
                                }
                            } else if (objects[i] == null) {
                                stmt.setObject(i + 1, null);
                            } else if (objects[i] instanceof Boolean) {
                                stmt.setBoolean(i + 1, (boolean) objects[i]);
                            } else if (objects[i] instanceof byte[]) {
                                stmt.setBytes(i + 1, (byte[]) objects[i]);
                            } else if (objects[i] instanceof Double) {
                                stmt.setDouble(i + 1, (double) objects[i]);
                            } else if (objects[i] instanceof Float) {
                                stmt.setFloat(i + 1, (float) objects[i]);
                            } else if (objects[i] instanceof Integer) {
                                stmt.setInt(i + 1, (int) objects[i]);
                            } else if (objects[i] instanceof Long) {
                                stmt.setLong(i + 1, (long) objects[i]);
                            } else if (objects[i] instanceof Short) {
                                stmt.setShort(i + 1, (short) objects[i]);
                            } else if (objects[i] instanceof String) {
                                stmt.setString(i + 1, (String) objects[i]);
                            } else {
                                stmt.setObject(i + 1, objects[i]);
                            }
                        }
                        stmt.addBatch();
                        count++;
                        if (count % 10000 == 0) {
                            System.out.println("Inserted: " + count);
                            stmt.executeBatch();
                        }
                    }
                    int[] executeUpdate = stmt.executeBatch();
                }
                System.out.println("Done.");
                return null;
            });

        }

    }

    public static String getTableSql( ADb db, String tableName ) throws Exception {
        String sql = "SELECT sql FROM sqlite_master WHERE type='table' and tbl_name='" + tableName + "'";

        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            } catch (SQLException e) {
                throw e;
            }
        });

    }

    public static List<String> getIndexSqls( ADb db, String tableName ) throws Exception {
        String sql = "SELECT sql FROM sqlite_master WHERE type='index' and tbl_name='" + tableName + "'";

        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                List<String> indexSqls = new ArrayList<>();
                while( rs.next() ) {
                    indexSqls.add(rs.getString(1));
                }
                return indexSqls;
            } catch (SQLException e) {
                throw e;
            }
        });

    }

}
