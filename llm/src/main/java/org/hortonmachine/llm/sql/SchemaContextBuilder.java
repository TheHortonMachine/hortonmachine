/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.llm.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.geopackage.GeopackageTableNames;
import org.hortonmachine.dbs.h2gis.H2GisTableNames;
import org.hortonmachine.dbs.spatialite.SpatialiteTableNames;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TableName;

/**
 * Describes the schema of a database for a model: tables and views with their columns, types,
 * primary keys, geometry columns (type, SRID, spatial index) and foreign keys, then, for the
 * tables without geometry, the foreign keys that lead to their location.
 *
 * <p>The metadata and index tables of the spatial extensions are left out. Rows of data are
 * added only when asked, since they leave the machine with a remote server.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class SchemaContextBuilder {

    /**
     * The maximum number of tables described, to keep the prompt in the context of small models.
     */
    public static final int MAX_TABLES = 150;

    /**
     * The number of rows shown per table when sample rows are asked.
     */
    public static final int SAMPLE_ROWS = 3;

    private static final int MAX_VALUE_CHARS = 60;

    private static final List<String> POSTGIS_TABLES = Arrays.asList("spatial_ref_sys", "geometry_columns",
            "geography_columns", "raster_columns", "raster_overviews");

    private static final Pattern WORD = Pattern.compile("[a-z_][a-z0-9_]*");

    private SchemaContextBuilder() {
    }

    /**
     * Describe the schema of a database.
     *
     * @param db the database.
     * @param includeSampleRows if <code>true</code>, add the first {@value #SAMPLE_ROWS} rows of each table.
     * @return the description.
     * @throws Exception if the metadata can't be read.
     */
    public static String build( ADb db, boolean includeSampleRows ) throws Exception {
        List<TableName> tables = listTables(db);

        StringBuilder sb = new StringBuilder();
        int count = Math.min(tables.size(), MAX_TABLES);
        List<TableInfo> infos = new ArrayList<>();
        for( int i = 0; i < count; i++ ) {
            TableInfo info = readTable(db, tables.get(i));
            describeTable(db, info, includeSampleRows, sb);
            infos.add(info);
        }
        if (tables.size() > MAX_TABLES) {
            sb.append("(").append(tables.size() - MAX_TABLES).append(" more tables are not listed)\n");
        }
        if (tables.isEmpty()) {
            sb.append("(the database has no tables)\n");
        }
        if (db.getType().isSpatial()) {
            Map<String, TableInfo> byName = new HashMap<>();
            for( TableInfo info : infos ) {
                byName.put(info.name().getName().toLowerCase(), info);
                byName.put(info.name().getFullName().toLowerCase(), info);
            }
            StringBuilder locations = new StringBuilder();
            for( TableInfo info : infos ) {
                for( String location : locationsOf(info, name -> byName.get(name.toLowerCase())) ) {
                    locations.append("- ").append(info.name().getName()).append(" has no geometry; its location: ")
                            .append(location).append("\n");
                }
            }
            if (locations.length() > 0) {
                sb.append("Locations of tables without geometry (join through the foreign key to place them):\n")
                        .append(locations);
            }
        }
        return sb.toString();
    }

    /**
     * Describe, compactly, the tables a piece of SQL uses: all their columns with the primary key,
     * the geometry and, for tables without geometry, where their location is.
     *
     * <p>Given to the model with the error of SQL the database rejected: small models use what is
     * right next to the error much better than what is somewhere in a long schema.</p>
     *
     * @param db the database.
     * @param sql the SQL.
     * @return one line per table (more for the locations), empty if no table of the database is used.
     * @throws Exception if the metadata can't be read.
     */
    public static String describeTablesUsedIn( ADb db, String sql ) throws Exception {
        Set<String> words = new HashSet<>();
        Matcher matcher = WORD.matcher(sql.toLowerCase());
        while( matcher.find() ) {
            words.add(matcher.group());
        }
        Map<String, TableName> allTables = new HashMap<>();
        List<TableName> used = new ArrayList<>();
        for( TableName tableName : listTables(db) ) {
            String name = tableName.getName().toLowerCase();
            allTables.put(name, tableName);
            allTables.put(tableName.getFullName().toLowerCase(), tableName);
            if (words.contains(name)) {
                used.add(tableName);
            }
        }

        boolean spatial = db.getType().isSpatial();
        StringBuilder sb = new StringBuilder();
        for( TableName tableName : used ) {
            TableInfo info = readTable(db, tableName);
            List<String> columns = new ArrayList<>();
            for( String[] column : info.columns() ) {
                String name = column[0];
                GeometryColumn geometry = info.geometry();
                if (geometry != null && name.equalsIgnoreCase(geometry.geometryColumnName)) {
                    columns.add(name + " (geometry " + (geometry.geometryType == null ? "" : geometry.geometryType + ", ")
                            + "SRID " + geometry.srid + ")");
                } else if (isPrimaryKey(column)) {
                    columns.add(name + " (primary key)");
                } else {
                    columns.add(name);
                }
            }
            sb.append(tableName.getName()).append(": ").append(String.join(", ", columns)).append("\n");
            if (spatial && info.geometry() == null) {
                List<String> locations = locationsOf(info, name -> {
                    TableName target = allTables.get(name.toLowerCase());
                    try {
                        return target == null ? null : readTable(db, target);
                    } catch (Exception e) {
                        return null;
                    }
                });
                if (locations.isEmpty()) {
                    sb.append("  ").append(tableName.getName()).append(" has no geometry\n");
                }
                for( String location : locations ) {
                    sb.append("  ").append(tableName.getName()).append(" has no geometry; its location: ").append(location)
                            .append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * @return the tables and views of the database, without the system ones.
     */
    private static List<TableName> listTables( ADb db ) throws Exception {
        EDb type = db.getType();
        List<TableName> tables = new ArrayList<>();
        for( TableName tableName : db.getTables() ) {
            if (tableName.getTableType() != ETableType.OTHER && !isSystemTable(type, tableName.getName())) {
                tables.add(tableName);
            }
        }
        return tables;
    }

    /**
     * What is known of a table.
     *
     * @param name the table.
     * @param columns the [name, type, pk] of the columns.
     * @param geometry the geometry column, or <code>null</code>.
     * @param foreignKeys the foreign keys.
     */
    private record TableInfo( TableName name, List<String[]> columns, GeometryColumn geometry, List<ForeignKey> foreignKeys ) {
    }

    private static TableInfo readTable( ADb db, TableName tableName ) throws Exception {
        SqlName sqlName = tableName.toSqlName();
        GeometryColumn geometryColumn = null;
        if (db instanceof ASpatialDb && db.getType().isSpatial()) {
            try {
                geometryColumn = ((ASpatialDb) db).getGeometryColumnsForTable(sqlName);
            } catch (Exception e) {
                // not a spatial table
            }
        }
        List<ForeignKey> foreignKeys = new ArrayList<>();
        try {
            foreignKeys.addAll(db.getForeignKeys(sqlName));
        } catch (Exception e) {
            // not supported for this table
        }
        return new TableInfo(tableName, db.getTableColumns(sqlName), geometryColumn, foreignKeys);
    }

    private static boolean isPrimaryKey( String[] column ) {
        String pk = column.length > 2 ? column[2] : null;
        return pk != null && !pk.equals("0") && !pk.isBlank();
    }

    /**
     * Tell, for a table without geometry, which foreign keys lead to a table with one: small
     * models otherwise invent geometry columns or take text columns for them.
     *
     * @param info the table.
     * @param tables gives the table of a name, or <code>null</code>.
     * @return the locations, e.g. <code>event.gpspoint_id -&gt; gpspoint.id, geometry gpspoint.the_geom (POINT, SRID 4326)</code>;
     *          empty for tables with geometry.
     */
    private static List<String> locationsOf( TableInfo info, Function<String, TableInfo> tables ) {
        List<String> locations = new ArrayList<>();
        if (info.geometry() != null) {
            return locations;
        }
        for( ForeignKey foreignKey : info.foreignKeys() ) {
            TableInfo target = foreignKey.toTable == null ? null : tables.apply(foreignKey.toTable);
            if (target == null || target.geometry() == null) {
                continue;
            }
            GeometryColumn geometry = target.geometry();
            String targetName = target.name().getName();
            locations.add(info.name().getName() + "." + foreignKey.from + " -> " + targetName + "." + foreignKey.to
                    + ", geometry " + targetName + "." + geometry.geometryColumnName + " ("
                    + (geometry.geometryType == null ? "" : geometry.geometryType + ", ") + "SRID " + geometry.srid + ")");
        }
        return locations;
    }

    private static void describeTable( ADb db, TableInfo info, boolean includeSampleRows, StringBuilder sb ) {
        TableName tableName = info.name();
        GeometryColumn geometryColumn = info.geometry();
        String kind = tableName.getTableType() == ETableType.VIEW ? "VIEW" : "TABLE";
        sb.append(kind).append(" ").append(tableName.getFullName()).append("\n");

        for( String[] column : info.columns() ) {
            String name = column[0];
            sb.append("  - ").append(name);
            if (geometryColumn != null && name.equalsIgnoreCase(geometryColumn.geometryColumnName)) {
                sb.append(" GEOMETRY ").append(geometryColumn.geometryType == null ? "" : geometryColumn.geometryType + " ");
                sb.append("SRID ").append(geometryColumn.srid);
                sb.append(geometryColumn.isSpatialIndexEnabled == 1 ? ", spatial index" : ", no spatial index");
            } else {
                String columnType = column[1];
                if (columnType != null && !columnType.isBlank()) {
                    sb.append(" ").append(columnType);
                }
            }
            if (isPrimaryKey(column)) {
                sb.append(" PRIMARY KEY");
            }
            sb.append("\n");
        }

        for( ForeignKey foreignKey : info.foreignKeys() ) {
            sb.append("  foreign key ").append(foreignKey.from).append(" -> ").append(foreignKey.toTable).append("(")
                    .append(foreignKey.to).append(")\n");
        }

        if (includeSampleRows) {
            appendSampleRows(db, tableName.toSqlName(), sb);
        }
    }

    private static void appendSampleRows( ADb db, SqlName sqlName, StringBuilder sb ) {
        try {
            QueryResult result = db.getTableRecordsMapFromRawSql("SELECT * FROM " + sqlName.fixedDoubleName, SAMPLE_ROWS);
            if (result.data.isEmpty()) {
                return;
            }
            sb.append("  sample rows (").append(String.join(", ", result.names)).append("):\n");
            for( Object[] row : result.data ) {
                List<String> values = new ArrayList<>();
                for( int i = 0; i < row.length; i++ ) {
                    values.add(i == result.geometryIndex ? "<geometry>" : formatValue(row[i]));
                }
                sb.append("    ").append(String.join(" | ", values)).append("\n");
            }
        } catch (Exception e) {
            // the rows are only an aid
        }
    }

    private static String formatValue( Object value ) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof byte[]) {
            return "<blob>";
        }
        String text = value.toString().replace('\n', ' ');
        if (text.length() > MAX_VALUE_CHARS) {
            text = text.substring(0, MAX_VALUE_CHARS) + "...";
        }
        return text;
    }

    /**
     * @param type the type of database.
     * @param name the name of the table.
     * @return <code>true</code> if the table belongs to the database or its spatial extension.
     */
    static boolean isSystemTable( EDb type, String name ) {
        String lower = name.toLowerCase();
        if (lower.startsWith("sqlite_")) {
            return true;
        }
        switch( type ) {
        case SPATIALITE:
        case SPATIALITE4ANDROID:
        case SQLITE:
        case MBTILES:
            return lower.startsWith(SpatialiteTableNames.startsWithIndexTables.toLowerCase())
                    || containsIgnoreCase(SpatialiteTableNames.metadataTables, name)
                    || containsIgnoreCase(SpatialiteTableNames.internalDataTables, name);
        case GEOPACKAGE:
        case GEOPACKAGE4ANDROID:
            return lower.startsWith(GeopackageTableNames.startsWithIndexTables) || lower.startsWith("gpkg_")
                    || containsIgnoreCase(GeopackageTableNames.metadataTables, name)
                    || containsIgnoreCase(GeopackageTableNames.internalDataTables, name);
        case H2GIS:
        case H2:
            return lower.startsWith(H2GisTableNames.startsWithIndexTables.toLowerCase())
                    || containsIgnoreCase(H2GisTableNames.spatialindexTables, name)
                    || containsIgnoreCase(H2GisTableNames.metadataTables, name)
                    || containsIgnoreCase(H2GisTableNames.internalDataTables, name);
        case POSTGIS:
        case POSTGRES:
            return POSTGIS_TABLES.contains(lower);
        default:
            return false;
        }
    }

    private static boolean containsIgnoreCase( List<String> names, String name ) {
        for( String item : names ) {
            if (item.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
