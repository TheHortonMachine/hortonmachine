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
package org.hortonmachine.dbs.compat.objects;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.nosql.INosqlCollection;
import org.hortonmachine.dbs.nosql.INosqlDb;
import org.hortonmachine.dbs.nosql.INosqlDocument;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * Class representing a db level.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DbLevel {
    public ADb parent;
    public String dbName;

    public List<SchemaLevel> schemasList = new ArrayList<SchemaLevel>();

    @Override
    public String toString() {
        return dbName;
    }

    /**
     * Get the {@link DbLevel} for a database.
     * 
     * @param db the database.
     * @return the {@link DbLevel}.
     * @throws Exception
     */
    public static DbLevel getDbLevel( ADb db ) throws Exception {
        DbLevel dbLevel = new DbLevel();
        String databasePath = db.getDatabasePath();
        File dbFile = new File(databasePath);

        String dbName = DbsUtilities.getNameWithoutExtention(dbFile);
        dbLevel.dbName = dbName;
        dbLevel.parent = db;

        HashMap<String, HashMap<String, List<String>>> currentDatabaseSchema2Type2TablesMap = db.getTablesMap();

        for( String schemaName : currentDatabaseSchema2Type2TablesMap.keySet() ) {
            SchemaLevel schemaLevel = new SchemaLevel();
            schemaLevel.schemaName = schemaName;

            var type2TablesMap = currentDatabaseSchema2Type2TablesMap.get(schemaName);
            if (type2TablesMap == null) {
                continue;
            }
            
            for (Entry<String, List<String>> entry : type2TablesMap.entrySet()) {
                String typeName = entry.getKey();
                List<String> tablesMap = entry.getValue();
                TableTypeLevel tableTypeLevel = new TableTypeLevel();
                tableTypeLevel.parent = schemaLevel;
                tableTypeLevel.typeName = typeName;
                for( String tableName : tablesMap ) {
                    TableLevel tableLevel = new TableLevel();
                    tableLevel.parent = tableTypeLevel;
                    tableLevel.tableName = tableName;
                    tableLevel.tableType = ETableType.fromType(typeName);
                    getColumnsList(db, tableLevel);
                    tableTypeLevel.tablesList.add(tableLevel);
                }
                schemaLevel.tableTypesList.add(tableTypeLevel);
            }
            dbLevel.schemasList.add(schemaLevel);
        }
        return dbLevel;
    }

    private static void getColumnsList(ADb db, TableLevel tableLevel) {
        String tableName = tableLevel.tableName;
        var columnsListTmp = new ArrayList<ColumnLevel>();
        SqlName sqlName = SqlName.m(tableName);
        GeometryColumn geometryColumns = null;
        try {
            if (db instanceof ASpatialDb) {
                geometryColumns = ((ASpatialDb) db).getGeometryColumnsForTable(sqlName);
            }
        } catch (Exception e1) {
            // ignore
            e1.printStackTrace();
        }
        tableLevel.isGeo = geometryColumns != null;
        List<ForeignKey> foreignKeys = new ArrayList<>();
        try {
            foreignKeys = db.getForeignKeys(sqlName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Index> indexes = new ArrayList<>();
        try {
            indexes = db.getIndexes(sqlName);
        } catch (Exception e) {
        }

        List<String[]> tableInfo;
        try {
            tableInfo = db.getTableColumns(sqlName);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        for (String[] columnInfo : tableInfo) {
            ColumnLevel columnLevel = new ColumnLevel();
            columnLevel.parent = tableLevel;
            String columnName = columnInfo[0];
            String columnType = columnInfo[1];
            String columnPk = columnInfo[2];
            columnLevel.columnName = columnName;
            columnLevel.columnType = columnType;
            columnLevel.isPK = columnPk.equals("1") ? true : false;
            if (geometryColumns != null && columnName.equalsIgnoreCase(geometryColumns.geometryColumnName)) {
                columnLevel.geomColumn = geometryColumns;
                if (columnType.equals("USER-DEFINED")) {
                    EGeometryType guessedType = geometryColumns.geometryType;// TODO check
                                                                                // EGeometryType.fromSpatialiteCode(geometryColumns.geometryType);
                    if (guessedType != null) {
                        columnLevel.columnType = guessedType.name();
                    }
                }
            }
            for (ForeignKey fKey : foreignKeys) {
                if (fKey.from.equals(columnName)) {
                    columnLevel.setFkReferences(fKey);
                }
            }
            for (Index index : indexes) {
                if (index.columns.contains(columnName)) {
                    columnLevel.setIndex(index);
                }
            }
            columnsListTmp.add(columnLevel);
        }
        tableLevel.columnsList = columnsListTmp;
    }


    public static DbLevel getDbLevel( INosqlDb db, String... types ) throws Exception {
        DbLevel dbLevel = new DbLevel();
        dbLevel.dbName = db.getDbName();

        // List<String> collectionsNames = db.getCollections(true);

        // HashMap<String, List<String>> currentDatabaseTablesMap = new HashMap<>();
        // currentDatabaseTablesMap.put(SchemaLevel.FALLBACK_SCHEMA, collectionsNames);
        // for( String typeName : types ) {
        //     TypeLevel typeLevel = new TypeLevel();
        //     typeLevel.typeName = typeName;
        //     List<String> collectionsList = currentDatabaseTablesMap.get(typeName);
        //     if (collectionsList == null) {
        //         continue;
        //     }
        //     for( String collectionName : collectionsList ) {
        //         TableLevel tableLevel = new TableLevel();
        //         tableLevel.parent = dbLevel;
        //         tableLevel.tableName = collectionName;

        //         tableLevel.tableType = ETableType.OTHER;
        //         tableLevel.isGeo = false;
        //         typeLevel.tablesList.add(tableLevel);

        //         INosqlCollection collection = db.getCollection(collectionName);

        //         HashMap<String, GeometryColumn> spatialIndexes = collection.getSpatialIndexes();

        //         INosqlDocument first = collection.getFirst();
        //         if (first != null) {
        //             LinkedHashMap<String, Object> schema = first.getSchema();

        //             for( Entry<String, Object> entry : schema.entrySet() ) {
        //                 String key = entry.getKey();
        //                 Object value = entry.getValue();
        //                 String type = value.toString();
        //                 ColumnLevel columnLevel = new ColumnLevel();
        //                 if (value instanceof Map) {
        //                     type = "Document";
        //                     handleDocument(columnLevel, (Map) value, spatialIndexes);
        //                 }

        //                 columnLevel.geomColumn = spatialIndexes.get(key);
        //                 columnLevel.parent = tableLevel;
        //                 columnLevel.columnName = key;
        //                 columnLevel.columnType = type;
        //                 columnLevel.isPK = key.equals("_id");
        //                 tableLevel.columnsList.add(columnLevel);
        //             }

        //         }
        //     }
        //     dbLevel.schemasList.add(typeLevel);
        // }

        return dbLevel;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void handleDocument( Object parent, Map<String, Object> map, HashMap<String, GeometryColumn> spatialIndexes ) {
        for( Entry<String, Object> entry : map.entrySet() ) {
            String key = entry.getKey();
            Object value = entry.getValue().toString();
            String type = value.getClass().getSimpleName();
            LeafLevel leafLevel = new LeafLevel();
            if (value instanceof Map) {
                type = "Document";
                handleDocument(leafLevel, (Map) value, spatialIndexes);
            } else {
                type = value.toString();
            }
            leafLevel.parent = parent;
            leafLevel.columnName = key;
            leafLevel.columnType = type;
            leafLevel.isPK = key.equals("_id");
            if (parent instanceof LeafLevel) {
                ((LeafLevel) parent).leafsList.add(leafLevel);
                leafLevel.geomColumn = spatialIndexes.get(key);;
            } else if (parent instanceof ColumnLevel) {
                ((ColumnLevel) parent).leafsList.add(leafLevel);
                leafLevel.geomColumn = spatialIndexes.get(key);;
            }
        }

    }
}
