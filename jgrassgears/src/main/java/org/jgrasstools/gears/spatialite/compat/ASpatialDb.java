package org.jgrasstools.gears.spatialite.compat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jgrasstools.gears.spatialite.ForeignKey;
import org.jgrasstools.gears.spatialite.QueryResult;
import org.jgrasstools.gears.spatialite.RasterCoverage;
import org.jgrasstools.gears.spatialite.SpatialiteGeometryColumns;
import org.jgrasstools.gears.spatialite.SpatialiteGeometryType;
import org.jgrasstools.gears.spatialite.SpatialiteTableNames;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

public abstract class ASpatialDb implements AutoCloseable {

    public static String PK_UID = "PK_UID";
    public static String defaultGeomFieldName = "the_geom";

    protected IJGTConnection mConn = null;

    protected String mDbPath;

    public boolean mPrintInfos = true;

    /**
     * Open the connection to a database.
     * 
     * <b>Make sure the connection object is created here.</b>
     * 
     * @param dbPath the database path. If <code>null</code>, an in-memory db is created.
     * @return <code>true</code> if the database did already exist.
     * @throws Exception
     */
    public abstract boolean open( String dbPath ) throws Exception;

    /**
     * @return the path to the database. 
     */
    public String getDatabasePath() {
        return mDbPath;
    }

    /**
     * Create Spatial Metadata initialize SPATIAL_REF_SYS and GEOMETRY_COLUMNS.
     * 
     * @param options optional tweaks.
     * @throws Exception 
     */
    public abstract void initSpatialMetadata( String options ) throws Exception;

    /**
     * Toggle autocommit mode.
     * 
     * @param enable if <code>true</code>, autocommit is enabled if not already enabled.
     *          Vice versa if <code>false</code>.
     * @throws SQLException
     */
    public void enableAutocommit( boolean enable ) throws Exception {
        boolean autoCommitEnabled = mConn.getAutoCommit();
        if (enable && !autoCommitEnabled) {
            // do enable if not already enabled
            mConn.setAutoCommit(true);
        } else if (!enable && autoCommitEnabled) {
            // disable if not already disabled
            mConn.setAutoCommit(false);
        }
    }

    /**
     * Get database infos.
     * 
     * @return the string array of [sqlite_version, spatialite_version, spatialite_target_cpu]
     * @throws SQLException
     */
    public String[] getDbInfo() throws Exception {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT sqlite_version(), spatialite_version(), spatialite_target_cpu()";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            String[] info = new String[3];
            while( rs.next() ) {
                // read the result set
                info[0] = rs.getString(1);
                info[1] = rs.getString(2);
                info[2] = rs.getString(3);
            }
            return info;
        }
    }

    /**
     * Create a new table.
     * 
     * @param tableName the table name.
     * @param fieldData the data for each the field (ex. id INTEGER NOT NULL PRIMARY KEY).
     * @throws SQLException
     */
    public void createTable( String tableName, String... fieldData ) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(tableName).append("(");
        for( int i = 0; i < fieldData.length; i++ ) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(fieldData[i]);
        }
        sb.append(")");

        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.execute(sb.toString());
        }
    }

    /**
     * Delete a geo-table with all attached indexes and stuff.
     * 
     * @param tableName
     * @throws Exception 
     */
    public void deleteGeoTable( String tableName ) throws Exception {
        String sql = "SELECT DropGeoTable('" + tableName + "');";

        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Create an single column index.
     * 
     * @param tableName the table.
     * @param column the column. 
     * @param isUnique if <code>true</code>, a unique index will be created.
     * @throws Exception 
     */
    public void createIndex( String tableName, String column, boolean isUnique ) throws Exception {
        String sql = getIndexSql(tableName, column, isUnique);
        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            String message = e.getMessage();
            if (message.contains("index") && message.contains("already exists")) {
                logWarn(message);
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get the sql to create an index.
     * 
     * @param tableName the table.
     * @param column the column. 
     * @param isUnique if <code>true</code>, a unique index will be created.
     * @return the index sql.
     */
    public String getIndexSql( String tableName, String column, boolean isUnique ) {
        String unique = "UNIQUE ";
        if (!isUnique) {
            unique = "";
        }
        String indexName = tableName + "__" + column + "_idx";
        String sql = "CREATE " + unique + "INDEX " + indexName + " on " + tableName + "(" + column + ");";
        return sql;
    }

    /**
     * Adds a geometry column to a table. 
     * 
     * @param tableName the table name.
     * @param geomColName the geometry column name.
     * @param geomType the geometry type (ex. LINESTRING);
     * @param epsg the optional epsg code (default is 4326);
     * @throws Exception 
     */
    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg )
            throws Exception {
        String epsgStr = "4326";
        if (epsg != null) {
            epsgStr = epsg;
        }
        String geomTypeStr = "LINESTRING";
        if (geomType != null) {
            geomTypeStr = geomType;
        }

        if (geomColName == null) {
            geomColName = defaultGeomFieldName;
        }

        try (IJGTStatement stmt = mConn.createStatement()) {
            String sql = "SELECT AddGeometryColumn('" + tableName + "','" + geomColName + "', " + epsgStr + ", '" + geomTypeStr
                    + "', 'XY')";
            stmt.execute(sql);

            sql = "SELECT CreateSpatialIndex('" + tableName + "', '" + geomColName + "');";
            stmt.execute(sql);
        }
    }

    /**
     * Insert a geometry into a table.
     * 
     * @param tableName the table to use.
     * @param geometry the geometry to insert.
     * @param epsg the optional epsg.
     * @throws Exception 
     */
    public void insertGeometry( String tableName, Geometry geometry, String epsg ) throws Exception {
        String epsgStr = "4326";
        if (epsg == null) {
            epsgStr = epsg;
        }

        SpatialiteGeometryColumns gc = getGeometryColumnsForTable(tableName);
        String sql = "INSERT INTO " + tableName + " (" + gc.f_geometry_column + ") VALUES (GeomFromText(?, " + epsgStr + "))";
        try (IJGTPreparedStatement pStmt = mConn.prepareStatement(sql)) {
            pStmt.setString(1, geometry.toText());
            pStmt.executeUpdate();
        }
    }

    /**
     * Get the list of available tables.
     * 
     * @param doOrder if <code>true</code>, the names are ordered.
     * @return the list of names.
     * @throws Exception 
     */
    public List<String> getTables( boolean doOrder ) throws Exception {
        List<String> tableNames = new ArrayList<String>();
        String orderBy = " ORDER BY name";
        if (!doOrder) {
            orderBy = "";
        }
        String sql = "SELECT name FROM sqlite_master WHERE type='table' or type='view'" + orderBy;
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String tabelName = rs.getString(1);
                tableNames.add(tabelName);
            }
            return tableNames;
        }
    }

    /**
     * Get the list of available raster coverages.
     * 
     * @param doOrder if <code>true</code>, the names are ordered.
     * @return the list of raster coverages.
     * @throws Exception 
     */
    public List<RasterCoverage> getRasterCoverages( boolean doOrder ) throws Exception {
        List<RasterCoverage> rasterCoverages = new ArrayList<RasterCoverage>();
        String orderBy = " ORDER BY name";
        if (!doOrder) {
            orderBy = "";
        }

        String sql = "SELECT " + RasterCoverage.COVERAGE_NAME + ", " + RasterCoverage.TITLE + ", " + RasterCoverage.SRID + ", "
                + RasterCoverage.COMPRESSION + ", " + RasterCoverage.EXTENT_MINX + ", " + RasterCoverage.EXTENT_MINY + ", "
                + RasterCoverage.EXTENT_MAXX + ", " + RasterCoverage.EXTENT_MAXY + " FROM " + RasterCoverage.TABLENAME + orderBy;
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                RasterCoverage rc = new RasterCoverage();
                int i = 1;
                rc.coverage_name = rs.getString(i++);
                rc.title = rs.getString(i++);
                rc.srid = rs.getInt(i++);
                rc.compression = rs.getString(i++);
                rc.extent_minx = rs.getDouble(i++);
                rc.extent_miny = rs.getDouble(i++);
                rc.extent_maxx = rs.getDouble(i++);
                rc.extent_maxy = rs.getDouble(i++);
                rasterCoverages.add(rc);
            }
            return rasterCoverages;
        }
    }

    /**
     * Get the list of available tables, mapped by type.
     * 
     * <p>
     * Supported types are:
     * <ul>
     * <li>{@value SpatialiteTableNames#INTERNALDATA} </li>
     * <li>{@value SpatialiteTableNames#METADATA} </li>
     * <li>{@value SpatialiteTableNames#SPATIALINDEX} </li>
     * <li>{@value SpatialiteTableNames#STYLE} </li>
     * <li>{@value SpatialiteTableNames#USERDATA} </li>
     * <li></li>
     * <li></li>
     * <li></li>
     * </ul>
     * 
     * @param doOrder
     * @return the map of tables sorted by aggregated type:
     * @throws Exception 
     */
    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception {
        List<String> tableNames = getTables(doOrder);
        HashMap<String, List<String>> tablesMap = SpatialiteTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }

    /**
     * Checks if the table is available.
     * 
     * @param tableName the name of the table.
     * @return <code>true</code> if the table exists.
     * @throws Exception 
     */
    public boolean hasTable( String tableName ) throws Exception {
        String sql = "SELECT name FROM sqlite_master WHERE type='table'";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String name = rs.getString(1);
                if (name.equals(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get the column [name, type, pk] values of a table.
     * 
     * @param tableName the table to check.
     * @return the list of column [name, type, pk].
     * @throws SQLException
     */
    public List<String[]> getTableColumns( String tableName ) throws Exception {
        List<String[]> columnNames = new ArrayList<String[]>();
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int nameIndex = -1;
            int typeIndex = -1;
            int pkIndex = -1;
            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                if (columnName.equals("name")) {
                    nameIndex = i;
                } else if (columnName.equals("type")) {
                    typeIndex = i;
                } else if (columnName.equals("pk")) {
                    pkIndex = i;
                }
            }

            while( rs.next() ) {
                String name = rs.getString(nameIndex);
                String type = rs.getString(typeIndex);
                String pk = "0";
                if (pkIndex > 0)
                    pk = rs.getString(pkIndex);
                columnNames.add(new String[]{name, type, pk});
            }
            return columnNames;
        }
    }

    /**
     * Get the geometry column definition for a given table.
     * 
     * @param tableName the table to check.
     * @return the {@link SpatialiteGeometryColumns column info}.
     * @throws Exception
     */
    public SpatialiteGeometryColumns getGeometryColumnsForTable( String tableName ) throws Exception {
        String attachedStr = "";
        if (tableName.indexOf('.') != -1) {
            // if the tablename contains a dot, then it comes from an attached database
            
            // get the database name
            String[] split = tableName.split("\\.");
            attachedStr = split[0] + ".";
            tableName = split[1];
//            logger.debug(MessageFormat.format("Considering attached database: {0}", attachedStr));
        }
        
        String sql = "select " + SpatialiteGeometryColumns.F_TABLE_NAME + ", " //
                + SpatialiteGeometryColumns.F_GEOMETRY_COLUMN + ", " //
                + SpatialiteGeometryColumns.GEOMETRY_TYPE + "," //
                + SpatialiteGeometryColumns.COORD_DIMENSION + ", " //
                + SpatialiteGeometryColumns.SRID + ", " //
                + SpatialiteGeometryColumns.SPATIAL_INDEX_ENABLED + " from " //
                + attachedStr + SpatialiteGeometryColumns.TABLENAME + " where " + SpatialiteGeometryColumns.F_TABLE_NAME + "='" + tableName
                + "'";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                SpatialiteGeometryColumns gc = new SpatialiteGeometryColumns();
                gc.f_table_name = rs.getString(1);
                gc.f_geometry_column = rs.getString(2);
                gc.geometry_type = rs.getInt(3);
                gc.coord_dimension = rs.getInt(4);
                gc.srid = rs.getInt(5);
                gc.spatial_index_enabled = rs.getInt(6);
                return gc;
            }
            return null;
        }
    }

    /**
     * Checks if a table is spatial.
     * 
     * @param tableName the table to check.
     * @return <code>true</code> if a geometry column is present.
     * @throws Exception 
     */
    public boolean isTableSpatial( String tableName ) throws Exception {
        SpatialiteGeometryColumns geometryColumns = getGeometryColumnsForTable(tableName);
        return geometryColumns != null;
    }

    /**
     * Get the foreign keys from a table.
     * 
     * @param tableName the table to check on.
     * @return the list of keys.
     * @throws Exception 
     */
    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        List<ForeignKey> fKeys = new ArrayList<ForeignKey>();
        String sql = "PRAGMA foreign_key_list(" + tableName + ")";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int fromIndex = -1;
            int toIndex = -1;
            int toTableIndex = -1;
            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                if (columnName.equals("from")) {
                    fromIndex = i;
                } else if (columnName.equals("to")) {
                    toIndex = i;
                } else if (columnName.equals("table")) {
                    toTableIndex = i;
                }
            }
            while( rs.next() ) {
                ForeignKey fKey = new ForeignKey();
                Object fromObj = rs.getObject(fromIndex);
                Object toObj = rs.getObject(toIndex);
                Object toTableObj = rs.getObject(toTableIndex);
                if (fromObj != null && toObj != null && toTableObj != null) {
                    fKey.from = fromObj.toString();
                    fKey.to = toObj.toString();
                    fKey.table = toTableObj.toString();
                } else {
                    continue;
                }
                fKeys.add(fKey);
            }
            return fKeys;
        }
    }

    /**
     * Get the record count of a table.
     * 
     * @param tableName the name of the table.
     * @return the record count or -1.
     * @throws Exception 
     */
    public long getCount( String tableName ) throws Exception {
        String sql = "select count(*) from " + tableName;
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                long count = rs.getLong(1);
                return count;
            }
            return -1;
        }
    }

    /**
     * Get the table records map with geometry in the given envelope.
     * 
     * <p>If the table is not geometric, the geom is set to null.
     * 
     * @param tableName the table name.
     * @param envelope the envelope to check.
     * @param limit if > 0 a limit is set.
     * @param alsoPK_UID if <code>true</code>, also the PK_UID column is considered.
     * @return the list of found records.
     * @throws SQLException
     * @throws ParseException
     */
    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, boolean alsoPK_UID, int limit,
            int reprojectSrid ) throws Exception {
        QueryResult queryResult = new QueryResult();

        SpatialiteGeometryColumns gCol = null;
        try {
            gCol = getGeometryColumnsForTable(tableName);
        } catch (Exception e) {
            // ignore
        }
        boolean hasGeom = gCol != null;

        List<String[]> tableColumnsInfo = getTableColumns(tableName);
        List<String> tableColumns = new ArrayList<>();
        for( String[] info : tableColumnsInfo ) {
            tableColumns.add(info[0]);
        }
        if (hasGeom) {
            tableColumns.remove(gCol.f_geometry_column);
        }
        if (!alsoPK_UID)
            tableColumns.remove(PK_UID);

        String sql = "SELECT ";
        if (hasGeom) {
            if (reprojectSrid == -1 || reprojectSrid == gCol.srid) {
                sql += "ST_AsBinary(" + gCol.f_geometry_column + ") AS " + gCol.f_geometry_column;
            } else {
                sql += "ST_AsBinary(ST_Transform(" + gCol.f_geometry_column + "," + reprojectSrid + ")) AS "
                        + gCol.f_geometry_column;
            }
        }
        for( int i = 0; i < tableColumns.size(); i++ ) {
            if (hasGeom || i != 0)
                sql += ",";
            sql += tableColumns.get(i);
        }
        sql += " FROM " + tableName;
        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE "; //
            sql += getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        WKBReader wkbReader = new WKBReader();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (hasGeom && columnName.equals(gCol.f_geometry_column)) {
                    queryResult.geometryIndex = i - 1;
                }
            }

            while( rs.next() ) {
                int i = 1;
                Object[] rec = new Object[columnCount];
                if (hasGeom) {
                    byte[] geomBytes = rs.getBytes(i);
                    Geometry geometry = wkbReader.read(geomBytes);
                    rec[i - 1] = geometry;
                    i++;
                }
                for( int j = i; j <= columnCount; j++ ) {
                    Object object = rs.getObject(j);
                    rec[j - 1] = object;
                }
                queryResult.data.add(rec);
            }
            return queryResult;
        }
    }

    /**
     * Execute a query from raw sql.
     * 
     * @param sql the sql to run.
     * @param limit a limit, ignored if < 1
     * @return the resulting records.
     * @throws Exception 
     */
    public QueryResult getTableRecordsMapFromRawSql( String sql, int limit ) throws Exception {
        QueryResult queryResult = new QueryResult();
        WKBReader wkbReader = new WKBReader();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int geometryIndex = -1;
            for( int i = 1; i <= columnCount; i++ ) {
                int columnType = rsmd.getColumnType(i);
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (columnTypeName.equals("BLOB") && SpatialiteGeometryType.forValue(columnType) != null) {
                    geometryIndex = i;
                    queryResult.geometryIndex = i - 1;
                }
            }
            int count = 0;
            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (j == geometryIndex) {
                        byte[] geomBytes = rs.getBytes(j);
                        try {
                            Geometry geometry = wkbReader.read(geomBytes);
                            rec[j - 1] = geometry;
                        } catch (Exception e) {
                            // ignore this, it could be missing ST_AsBinary() in the sql
                        }
                    } else {
                        Object object = rs.getObject(j);
                        rec[j - 1] = object;
                    }
                }
                queryResult.data.add(rec);
                if (limit > 0 && ++count > (limit - 1)) {
                    break;
                }
            }
            return queryResult;
        }
    }

    /**
     * Execute a query from raw sql and put the result in a csv file.
     * 
     * @param sql the sql to run.
     * @param csvFile the output file.
     * @param doHeader if <code>true</code>, the header is written.
     * @param separator the separator (if null, ";" is used).
     * @throws Exception 
     */
    public void runRawSqlToCsv( String sql, File csvFile, boolean doHeader, String separator ) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
            WKBReader wkbReader = new WKBReader();
            try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
                IJGTResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int geometryIndex = -1;
                for( int i = 1; i <= columnCount; i++ ) {
                    if (i > 1) {
                        bw.write(separator);
                    }
                    int columnType = rsmd.getColumnType(i);
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    String columnName = rsmd.getColumnName(i);
                    bw.write(columnName);
                    if (columnTypeName.equals("BLOB") && SpatialiteGeometryType.forValue(columnType) != null) {
                        geometryIndex = i;
                    }
                }
                bw.write("\n");
                while( rs.next() ) {
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (j > 1) {
                            bw.write(separator);
                        }
                        byte[] geomBytes = null;
                        if (j == geometryIndex) {
                            geomBytes = rs.getBytes(j);
                        }
                        if (geomBytes != null) {
                            try {
                                Geometry geometry = wkbReader.read(geomBytes);
                                bw.write(geometry.toText());
                            } catch (Exception e) {
                                // write it as it comes
                                Object object = rs.getObject(j);
                                if (object != null) {
                                    bw.write(object.toString());
                                } else {
                                    bw.write("");
                                }
                            }
                        } else {
                            Object object = rs.getObject(j);
                            if (object != null) {
                                bw.write(object.toString());
                            } else {
                                bw.write("");
                            }
                        }
                    }
                    bw.write("\n");
                }
            }
        }
    }

    /**
     * Execute a insert/update sql file. 
     * 
     * @param file the file to run on this db.
     * @param chunks commit interval.
     * @throws Exception 
     */
    public abstract void executeSqlFile( File file, int chunks, boolean eachLineAnSql ) throws Exception;

    /**
     * Execute an update, insert or delete by sql.
     * 
     * @param sql the sql to run.
     * @return the result code of the update.
     * @throws Exception 
     */
    public int executeInsertUpdateDeleteSql( String sql ) throws Exception {
        try (IJGTStatement stmt = mConn.createStatement()) {
            int executeUpdate = stmt.executeUpdate(sql);
            return executeUpdate;
        }
    }

    /**
     * Get the geometries of a table inside a given envelope.
     * 
     * @param tableName the table name.
     * @param envelope the envelope to check.
     * @return The list of geometries intersecting the envelope.
     * @throws Exception 
     */
    public List<Geometry> getGeometriesIn( String tableName, Envelope envelope ) throws Exception {
        List<Geometry> geoms = new ArrayList<Geometry>();

        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        String sql = "SELECT ST_AsBinary(" + gCol.f_geometry_column + ") FROM " + tableName;

        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE " + getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }
        WKBReader wkbReader = new WKBReader();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                byte[] geomBytes = rs.getBytes(1);
                Geometry geometry = wkbReader.read(geomBytes);
                geoms.add(geometry);
            }
            return geoms;
        }
    }

    /**
     * Get the where cause of a Spatialindex based BBOX query.
     * 
     * @param tableName the name of the table.
     * @param x1 west bound.
     * @param y1 south bound.
     * @param x2 east bound.
     * @param y2 north bound.
     * @return the sql piece.
     * @throws Exception 
     */
    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = tableName + ".ROWID";
        } else {
            rowid = alias + ".ROWID";
            alias = alias + ".";
        }
        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        if (tableName.indexOf('.') != -1) {
            // if the tablename contains a dot, then it comes from an attached database
            tableName = "DB="+ tableName;
        }
        
        String sql = "ST_Intersects(" + alias + gCol.f_geometry_column + ", BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2
                + ")) = 1 AND " + rowid + " IN ( SELECT ROWID FROM SpatialIndex WHERE "//
                + "f_table_name = '" + tableName + "' AND " //
                + "search_frame = BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "))";
        return sql;
    }

    /**
     * Get the where query piece based on a geometry intersection.
     * 
     * @param tableName the table to query.
     * @param alias optinal alias.
     * @param geometry the geometry to intersect.
     * @return the query piece.
     * @throws Exception 
     */
    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = tableName + ".ROWID";
        } else {
            rowid = alias + ".ROWID";
            alias = alias + ".";
        }

        Envelope envelope = geometry.getEnvelopeInternal();
        double x1 = envelope.getMinX();
        double x2 = envelope.getMaxX();
        double y1 = envelope.getMinY();
        double y2 = envelope.getMaxY();
        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        if (tableName.indexOf('.') != -1) {
            // if the tablename contains a dot, then it comes from an attached database
            tableName = "DB="+ tableName;
        }
        String sql = "ST_Intersects(" + alias + gCol.f_geometry_column + ", " + "GeomFromText('" + geometry.toText() + "')"
                + ") = 1 AND " + rowid + " IN ( SELECT ROWID FROM SpatialIndex WHERE "//
                + "f_table_name = '" + tableName + "' AND " //
                + "search_frame = BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "))";
        return sql;
    }

    /**
     * Get the bounds of a table.
     * 
     * @param tableName the table to query.
     * @return the {@link Envelope} of the table.
     * @throws Exception 
     */
    public abstract Envelope getTableBounds( String tableName ) throws Exception;
    
    /**
     * @return the connection to the database.
     */
    public IJGTConnection getConnection() {
        return mConn;
    }

    public void close() throws Exception {
        if (mConn != null) {
            mConn.close();
        }
    }

    /**
     * Escape sql.
     * 
     * @param sql the sql code to escape. 
     * @return the escaped sql.
     */
    public static String escapeSql( String sql ) {
        // ' --> ''
        sql = sql.replaceAll("'", "''");
        // " --> ""
        sql = sql.replaceAll("\"", "\"\"");
        // \ --> (remove backslashes)
        sql = sql.replaceAll("\\\\", "");
        return sql;
    }

    /**
     * Composes the formatter for unix timstamps in queries.
     * 
     * <p>The default format is: <b>2015-06-11 03:14:51</b>, as
     * given by pattern: <b>%Y-%m-%d %H:%M:%S</b>.</p>
     * 
     * @param columnName the timestamp column in the db.
     * @param datePattern the datepattern.
     * @return the query piece.
     */
    public static String getTimestampQuery( String columnName, String datePattern ) {
        if (datePattern == null)
            datePattern = "%Y-%m-%d %H:%M:%S";
        String sql = "strftime('" + datePattern + "', " + columnName + " / 1000, 'unixepoch')";
        return sql;
    }

    protected abstract void logWarn( String message );

    protected abstract void logInfo( String message );

    protected abstract void logDebug( String message );

}