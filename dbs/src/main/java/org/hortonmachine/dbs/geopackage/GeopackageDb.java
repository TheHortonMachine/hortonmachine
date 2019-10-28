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
package org.hortonmachine.dbs.geopackage;

import static java.lang.String.format;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.HMTransactionExecuter;
import org.hortonmachine.dbs.compat.IDbVisitor;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.datatypes.EDataType;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.datatypes.ESpatialiteGeometryType;
import org.hortonmachine.dbs.geopackage.Entry.DataType;
import org.hortonmachine.dbs.geopackage.geom.GeoPkgGeomReader;
import org.hortonmachine.dbs.geopackage.geom.GeometryFunction;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.spatialite.SpatialiteCommonMethods;
import org.hortonmachine.dbs.spatialite.SpatialiteGeometryColumns;
import org.hortonmachine.dbs.spatialite.SpatialiteTableNames;
import org.hortonmachine.dbs.spatialite.SpatialiteWKBReader;
import org.hortonmachine.dbs.spatialite.hm.SqliteDb;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.ResultSetToObjectFunction;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.sqlite.Function;

/**
 * A spatialite database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopackageDb extends ASpatialDb {
    public static final String GEOPACKAGE_CONTENTS = "gpkg_contents";

    public static final String GEOMETRY_COLUMNS = "gpkg_geometry_columns";

    public static final String SPATIAL_REF_SYS = "gpkg_spatial_ref_sys";

    public static final String RASTER_COLUMNS = "gpkg_data_columns";

    public static final String TILE_MATRIX_METADATA = "gpkg_tile_matrix";

    public static final String METADATA = "gpkg_metadata";

    public static final String METADATA_REFERENCE = "gpkg_metadata_reference";

    public static final String TILE_MATRIX_SET = "gpkg_tile_matrix_set";

    public static final String DATA_COLUMN_CONSTRAINTS = "gpkg_data_column_constraints";

    public static final String EXTENSIONS = "gpkg_extensions";

    public static final String SPATIAL_INDEX = "gpkg_spatial_index";

    static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private SqliteDb sqliteDb;

    private boolean supportsRtree = true;

    public GeopackageDb() {
        sqliteDb = new SqliteDb();
    }

    @Override
    public EDb getType() {
        return EDb.GEOPACKAGE;
    }

    public void setCredentials( String user, String password ) {
        this.user = user;
        this.password = password;
    }

    @Override
    public ConnectionData getConnectionData() {
        return sqliteDb.getConnectionData();
    }

    @Override
    public boolean open( String dbPath, String user, String password ) throws Exception {
        setCredentials(user, password);
        return open(dbPath);
    }

    public boolean open( String dbPath ) throws Exception {
        sqliteDb.setCredentials(user, password);
        boolean dbExists = sqliteDb.open(dbPath);

        sqliteDb.getConnectionData().dbType = getType().getCode();

        this.mDbPath = sqliteDb.getDatabasePath();

//        if (mPrintInfos) {
//            String[] dbInfo = getDbInfo();
//            Logger.INSTANCE.insertInfo(null, "Spatialite Version: " + dbInfo[0]);
//            Logger.INSTANCE.insertInfo(null, "Spatialite Target CPU: " + dbInfo[1]);
//        }
        return dbExists;
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        Connection cx = sqliteDb.getJdbcConnection();
        createFunctions(cx);
        // see if we have to create the table structure
        boolean initialized = false;
        try (Statement st = cx.createStatement(); ResultSet rs = st.executeQuery("PRAGMA application_id")) {
            if (rs.next()) {
                int applicationId = rs.getInt(1);
                initialized = (0x47503130 == applicationId);
            }
        }

        try {
            String checkTable = "rtree_test_check";
            String checkRtree = "CREATE VIRTUAL TABLE " + checkTable + " USING rtree(id, minx, maxx, miny, maxy)";
            sqliteDb.executeInsertUpdateDeleteSql(checkRtree);
            String drop = "DROP TABLE " + checkTable;
            sqliteDb.executeInsertUpdateDeleteSql(drop);
            supportsRtree = true;
        } catch (Exception e) {
            supportsRtree = false;
        }

        if (!initialized) {
            runScript(SPATIAL_REF_SYS + ".sql");
            runScript(GEOMETRY_COLUMNS + ".sql");
            runScript(GEOPACKAGE_CONTENTS + ".sql");
            runScript(TILE_MATRIX_SET + ".sql");
            runScript(TILE_MATRIX_METADATA + ".sql");
            runScript(RASTER_COLUMNS + ".sql");
            runScript(METADATA + ".sql");
            runScript(METADATA_REFERENCE + ".sql");
            runScript(DATA_COLUMN_CONSTRAINTS + ".sql");
            runScript(EXTENSIONS + ".sql");
            addDefaultSpatialReferences(cx);

            sqliteDb.executeInsertUpdateDeleteSql("PRAGMA application_id = 0x47503130;");
        }
    }

    /** Returns list of contents of the geopackage. 
     * @throws Exception */
    public List<Entry> contents() throws Exception {

        return execOnConnection(connection -> {
            List<Entry> contents = new ArrayList<Entry>();
            String sql = "SELECT c.*, g.column_name, g.geometry_type_name, g.z , g.m FROM " + GEOPACKAGE_CONTENTS + " c, "
                    + GEOMETRY_COLUMNS + " g where c.table_name=g.table_name";
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String dt = rs.getString("data_type");
                    DataType type = Entry.DataType.of(dt);
                    Entry e = null;
                    switch( type ) {
                    case Feature:
                        e = createFeatureEntry(rs);
                        break;
                    case Tile:
//                        e = createTileEntry(rs, cx);
                        break;
                    default:
                        throw new IllegalStateException("unexpected type in GeoPackage");
                    }
                    if (e != null) {
                        contents.add(e);
                    }

                }
                return contents;
            }
        });

    }

    /** Lists all the feature entries in the geopackage. */
    public List<FeatureEntry> features() throws Exception {
        return execOnConnection(connection -> {
            List<FeatureEntry> contents = new ArrayList<FeatureEntry>();
            String sql = format(
                    "SELECT a.*, b.column_name, b.geometry_type_name, b.z, b.m, c.organization_coordsys_id, c.definition"
                            + " FROM %s a, %s b, %s c" + " WHERE a.table_name = b.table_name" + " AND a.srs_id = c.srs_id"
                            + " AND a.data_type = ?",
                    GEOPACKAGE_CONTENTS, GEOMETRY_COLUMNS, SPATIAL_REF_SYS);

            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                pStmt.setString(1, DataType.Feature.value());

                IHMResultSet rs = pStmt.executeQuery();
                while( rs.next() ) {
                    contents.add(createFeatureEntry(rs));
                }

                return contents;
            }
        });
    }

    /**
     * Looks up a feature entry by name.
     *
     * @param name THe name of the feature entry.
     * @return The entry, or <code>null</code> if no such entry exists.
     */
    public FeatureEntry feature( String name ) throws Exception {
        return sqliteDb.execOnConnection(connection -> {
            String sql = format(
                    "SELECT a.*, b.column_name, b.geometry_type_name, b.m, b.z, c.organization_coordsys_id, c.definition"
                            + " FROM %s a, %s b, %s c" + " WHERE a.table_name = b.table_name " + " AND a.srs_id = c.srs_id "
                            + " AND a.table_name = ?" + " AND a.data_type = ?",
                    GEOPACKAGE_CONTENTS, GEOMETRY_COLUMNS, SPATIAL_REF_SYS);

            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                pStmt.setString(1, name);
                pStmt.setString(2, DataType.Feature.value());

                IHMResultSet rs = pStmt.executeQuery();
                if (rs.next()) {
                    return createFeatureEntry(rs);
                }
                return null;
            }
        });
    }

    /**
     * Verifies if a spatial index is present
     *
     * @param entry The feature entry.
     * @return whether this feature entry has a spatial index available.
     * @throws IOException
     */
    public boolean hasSpatialIndex( String table ) throws Exception {
        FeatureEntry feature = feature(table);
        return sqliteDb.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection
                    .prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=? ")) {
                pStmt.setString(1, getSpatialIndexName(feature));
                IHMResultSet resultSet = pStmt.executeQuery();
                return resultSet.next();
            }
        });
    }

    private String getSpatialIndexName( FeatureEntry feature ) {
        return "rtree_" + feature.tableName + "_" + feature.geometryColumn;
    }

    private FeatureEntry createFeatureEntry( IHMResultSet rs ) throws Exception {
        FeatureEntry e = new FeatureEntry();
        e.setIdentifier(rs.getString("identifier"));
        e.setDescription(rs.getString("description"));
        e.setTableName(rs.getString("table_name"));
        try {
            final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);
            DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
            e.setLastChange(DATE_FORMAT.parse(rs.getString("last_change")));
        } catch (ParseException ex) {
            throw new IOException(ex);
        }

        int srid = rs.getInt("srs_id");
        e.setSrid(srid);
        e.setBounds(new Envelope(rs.getDouble("min_x"), rs.getDouble("max_x"), rs.getDouble("min_y"), rs.getDouble("max_y")));

        e.setGeometryColumn(rs.getString("column_name"));
        e.setGeometryType(EGeometryType.forTypeName(rs.getString("geometry_type_name")));
        e.setZ(rs.getBoolean("z"));
        e.setM(rs.getBoolean("m"));
        return e;
    }

    private void runScript( String filename ) throws Exception {
        InputStream resourceAsStream = GeopackageDb.class.getResourceAsStream(filename);
        List<String> sqlStatements = DbsUtilities.streamToStringList(resourceAsStream, ";");

        HMTransactionExecuter transactionExecuter = new HMTransactionExecuter(sqliteDb){
            @Override
            public void executeInTransaction( IHMConnection conn ) throws Exception {
                for( String sqlStatement : sqlStatements ) {
                    try (IHMStatement stmt = conn.createStatement()) {
                        stmt.executeUpdate(sqlStatement);
                    }
                }
            }
        };
        transactionExecuter.execute();
    }

    private void addDefaultSpatialReferences( Connection cx ) throws Exception {
        try {
            addCRS(cx, -1, "Undefined cartesian SRS", "NONE", -1, "undefined", "undefined cartesian coordinate reference system");
            addCRS(cx, 0, "Undefined geographic SRS", "NONE", 0, "undefined", "undefined geographic coordinate reference system");
            addCRS(cx, 4326, "WGS 84 geodetic", "EPSG", 4326,
                    "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\","
                            + "6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],"
                            + "PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,"
                            + "AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]",
                    "longitude/latitude coordinates in decimal degrees on the WGS 84 spheroid");
        } catch (IOException ex) {
            throw new SQLException("Unable to add default spatial references.", ex);
        }
    }

    private void addCRS( Connection cx, int srid, String srsName, String organization, int organizationCoordSysId,
            String definition, String description ) throws Exception {
        try {
            String sqlPrep = String.format("SELECT srs_id FROM %s WHERE srs_id = ?", SPATIAL_REF_SYS);
            boolean hasAlready = sqliteDb.execOnConnection(connection -> {
                try (IHMPreparedStatement pStmt = connection.prepareStatement(sqlPrep)) {
                    pStmt.setInt(1, srid);
                    IHMResultSet resultSet = pStmt.executeQuery();
                    if (resultSet.next()) {
                        return true;
                    }
                    return false;
                }
            });
            if (hasAlready)
                return;

            String sqlPrep1 = String
                    .format("INSERT INTO %s (srs_id, srs_name, organization, organization_coordsys_id, definition, description) "
                            + "VALUES (?,?,?,?,?,?)", SPATIAL_REF_SYS);

            boolean ok = sqliteDb.execOnConnection(connection -> {
                try (IHMPreparedStatement pStmt = connection.prepareStatement(sqlPrep1)) {
                    int i = 1;
                    pStmt.setInt(i++, srid);
                    pStmt.setString(i++, organization);
                    pStmt.setInt(i++, organizationCoordSysId);
                    pStmt.setString(i++, definition);
                    pStmt.setString(i++, description);

                    pStmt.executeUpdate();

                    return true;
                }
            });

            if (!ok) {
                throw new IOException("Unable to insert CRS: " + srid);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static void createFunctions( Connection cx ) throws SQLException {
        // minx
        Function.create(cx, "ST_MinX", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws IOException {
                return reader.getEnvelope().getMinX();
            }
        });

        // maxx
        Function.create(cx, "ST_MaxX", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws IOException {
                return reader.getEnvelope().getMaxX();
            }
        });

        // miny
        Function.create(cx, "ST_MinY", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws IOException {
                return reader.getEnvelope().getMinY();
            }
        });

        // maxy
        Function.create(cx, "ST_MaxY", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws IOException {
                return reader.getEnvelope().getMaxY();
            }
        });

        // empty
        Function.create(cx, "ST_IsEmpty", new GeometryFunction(){
            @Override
            public Object execute( GeoPkgGeomReader reader ) throws IOException {
                return reader.getHeader().getFlags().isEmpty();
            }
        });
    }

    @Override
    public String getJdbcUrlPre() {
        return sqliteDb.getJdbcUrlPre();
    }

    public Connection getJdbcConnection() {
        return sqliteDb.getJdbcConnection();
    }

    @Override
    protected IHMConnection getConnectionInternal() throws Exception {
        return sqliteDb.getConnectionInternal();
    }

    public void close() throws Exception {
        sqliteDb.close();
    }

    public String[] getDbInfo() {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT spatialite_version(), spatialite_target_cpu()";
        try {
            IHMConnection mConn = sqliteDb.getConnectionInternal();
            try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                String[] info = new String[2];
                while( rs.next() ) {
                    // read the result set
                    info[0] = rs.getString(1);
                    info[1] = rs.getString(2);
                }
                return info;
            }
        } catch (Exception e) {
            return new String[]{"no version info available", "no version info available"};
        }
    }

    public void createSpatialTable( String tableName, int tableSrid, String geometryFieldData, String[] fieldData,
            String[] foreignKeys, boolean avoidIndex ) throws Exception {
//        SpatialiteCommonMethods.createSpatialTable(this, tableName, tableSrid, geometryFieldData, fieldData, foreignKeys,
//                avoidIndex);
        throw new RuntimeException("Not implemented yet...");
    }

    @Override
    public Envelope getTableBounds( String tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableBounds(this, tableName);
    }

    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, int limit, int reprojectSrid, String whereStr )
            throws Exception {
        QueryResult queryResult = new QueryResult();
        GeometryColumn gCol = null;
        String geomColLower = null;
        try {
            gCol = getGeometryColumnsForTable(tableName);
            if (gCol != null)
                geomColLower = gCol.geometryColumnName.toLowerCase();
        } catch (Exception e) {
            // ignore
        }

        List<String[]> tableColumnsInfo = getTableColumns(tableName);
        int columnCount = tableColumnsInfo.size();

        int index = 0;
        List<String> items = new ArrayList<>();
        List<ResultSetToObjectFunction> funct = new ArrayList<>();
        for( String[] columnInfo : tableColumnsInfo ) {
            String columnName = columnInfo[0];
            if (DbsUtilities.isReservedName(columnName)) {
                columnName = DbsUtilities.fixReservedNameForQuery(columnName);
            }

            String columnTypeName = columnInfo[1];

            queryResult.names.add(columnName);
            queryResult.types.add(columnTypeName);

            String isPk = columnInfo[2];
            if (isPk.equals("1")) {
                queryResult.pkIndex = index;
            }
            if (geomColLower != null && columnName.toLowerCase().equals(geomColLower)) {
                queryResult.geometryIndex = index;

                if (reprojectSrid == -1 || reprojectSrid == gCol.srid) {
                    items.add(geomColLower);
                } else {
                    items.add("ST_Transform(" + geomColLower + "," + reprojectSrid + ") AS " + geomColLower);
                }
            } else {
                items.add(columnName);
            }
            index++;

            EDataType type = EDataType.getType4Name(columnTypeName);
            switch( type ) {
            case TEXT: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getString(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case INTEGER: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getInt(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case FLOAT: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getFloat(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case DOUBLE: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getDouble(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case LONG: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getLong(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case BLOB: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getBytes(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case DATE: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            Date date = resultSet.getDate(index);
                            return date;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            default:
                funct.add(null);
                break;
            }
        }

        String sql = "SELECT ";
        sql += DbsUtilities.joinByComma(items);
        sql += " FROM " + tableName;

        List<String> whereStrings = new ArrayList<>();
        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            String spatialindexBBoxWherePiece = getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
            if (spatialindexBBoxWherePiece != null)
                whereStrings.add(spatialindexBBoxWherePiece);
        }
        if (whereStr != null) {
            whereStrings.add(whereStr);
        }
        if (whereStrings.size() > 0) {
            sql += " WHERE "; //
            sql += DbsUtilities.joinBySeparator(whereStrings, " AND ");
        }

        if (limit > 0) {
            sql += " LIMIT " + limit;
        }

        IGeometryParser gp = getType().getGeometryParser();
        String _sql = sql;
        return execOnConnection(connection -> {
            long start = System.currentTimeMillis();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                while( rs.next() ) {
                    Object[] rec = new Object[columnCount];
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (queryResult.geometryIndex == j - 1) {
                            Geometry geometry = gp.fromResultSet(rs, j);
                            if (geometry != null) {
                                rec[j - 1] = geometry;
                            }
                        } else {
                            ResultSetToObjectFunction function = funct.get(j - 1);
                            Object object = function.getObject(rs, j);
                            if (object instanceof Clob) {
                                object = rs.getString(j);
                            }
                            rec[j - 1] = object;
                        }
                    }
                    queryResult.data.add(rec);
                }
                long end = System.currentTimeMillis();
                queryResult.queryTimeMillis = end - start;
                return queryResult;
            }
        });

    }

    @Override
    protected void logWarn( String message ) {
        Logger.INSTANCE.insertWarning(null, message);
    }

    @Override
    protected void logInfo( String message ) {
        Logger.INSTANCE.insertInfo(null, message);
    }

    @Override
    protected void logDebug( String message ) {
        Logger.INSTANCE.insertDebug(null, message);
    }

    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception {
        List<String> tableNames = getTables(doOrder);
        HashMap<String, List<String>> tablesMap = SpatialiteTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }

    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        if (!supportsRtree)
            return null;
        FeatureEntry feature = feature(tableName);
        String spatial_index = getSpatialIndexName(feature);

        String pk = SpatialiteCommonMethods.getPrimaryKey(sqliteDb, tableName);
        if (pk == null) {
            // can't use spatial index
            return null;
        }

        String check = "(" + x1 + " <= maxx and " + x2 + " >= minx and " + y1 + " <= maxy and " + y2 + " >= miny)";
        // Make Sure the table name is escaped
        String sql = pk + " IN ( SELECT id FROM \"" + spatial_index + "\"  WHERE " + check + ")";
        return sql;
    }

    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception {
        // this is not possible in gpkg, backing on envelope intersection
        Envelope env = geometry.getEnvelopeInternal();
        return getSpatialindexBBoxWherePiece(tableName, alias, env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
    }

    public GeometryColumn getGeometryColumnsForTable( String tableName ) throws Exception {
        FeatureEntry feature = feature(tableName);
        SpatialiteGeometryColumns gc = new SpatialiteGeometryColumns();
        gc.tableName = tableName;
        gc.geometryColumnName = feature.geometryColumn;
        gc.geometryType = feature.geometryType;
        int dim = 2;
        if (feature.z)
            dim++;
        if (feature.m)
            dim++;
        gc.coordinatesDimension = dim;
        gc.srid = feature.srid;
        gc.isSpatialIndexEnabled = hasSpatialIndex(tableName) ? 1 : 0;
        return gc;
    }

    @Override
    public List<String> getTables( boolean doOrder ) throws Exception {
        Stream<String> map = features().stream().map(e -> e.tableName);
        if (doOrder) {
            map = map.sorted();
        }
        return map.collect(Collectors.toList());
    }

    @Override
    public boolean hasTable( String tableName ) throws Exception {
        return sqliteDb.hasTable(tableName);
    }

    public ETableType getTableType( String tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableType(this, tableName);
    }

    @Override
    public List<String[]> getTableColumns( String tableName ) throws Exception {
        List<String[]> tableColumns = SpatialiteCommonMethods.getTableColumns(this, tableName);
        return tableColumns;
    }

    @Override
    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        return sqliteDb.getForeignKeys(tableName);
    }

    public String getGeojsonIn( String tableName, String[] fields, String wherePiece, Integer precision ) throws Exception {
        return SpatialiteCommonMethods.getGeojsonIn(this, tableName, fields, wherePiece, precision);
    }

    /**
     * Delete a geo-table with all attached indexes and stuff.
     * 
     * @param tableName
     * @throws Exception
     */
    public void deleteGeoTable( String tableName ) throws Exception {
        String sql = "SELECT DropGeoTable('" + tableName + "');";

        try (IHMStatement stmt = sqliteDb.getConnectionInternal().createStatement()) {
            stmt.execute(sql);
        }
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg,
            boolean avoidIndex ) throws Exception {
        throw new RuntimeException("Not implemented yet...");
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg )
            throws Exception {
        throw new RuntimeException("Not implemented yet...");
    }

    public QueryResult getTableRecordsMapFromRawSql( String sql, int limit ) throws Exception {
        QueryResult queryResult = new QueryResult();
        try (IHMStatement stmt = sqliteDb.getConnectionInternal().createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            IHMResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int geometryIndex = -1;
            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (ESpatialiteGeometryType.isGeometryName(columnTypeName)) {
                    geometryIndex = i;
                    queryResult.geometryIndex = i - 1;
                }
            }
            int count = 0;
            SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
            long start = System.currentTimeMillis();
            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (j == geometryIndex) {
                        byte[] geomBytes = rs.getBytes(j);
                        if (geomBytes != null) {
                            Geometry geometry = wkbReader.read(geomBytes);
                            rec[j - 1] = geometry;
                        }
                    } else {
                        Object object = rs.getObject(j);
                        if (object instanceof Clob) {
                            object = rs.getString(j);
                        }
                        rec[j - 1] = object;
                    }
                }
                queryResult.data.add(rec);
                if (limit > 0 && ++count > (limit - 1)) {
                    break;
                }
            }
            long end = System.currentTimeMillis();
            queryResult.queryTimeMillis = end - start;
            return queryResult;
        }
    }

    /**
     * Execute a query from raw sql and put the result in a csv file.
     * 
     * @param sql
     *            the sql to run.
     * @param csvFile
     *            the output file.
     * @param doHeader
     *            if <code>true</code>, the header is written.
     * @param separator
     *            the separator (if null, ";" is used).
     * @throws Exception
     */
    public void runRawSqlToCsv( String sql, File csvFile, boolean doHeader, String separator ) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
            SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
            try (IHMStatement stmt = sqliteDb.getConnectionInternal().createStatement();
                    IHMResultSet rs = stmt.executeQuery(sql)) {
                IHMResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int geometryIndex = -1;
                for( int i = 1; i <= columnCount; i++ ) {
                    if (i > 1) {
                        bw.write(separator);
                    }
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    String columnName = rsmd.getColumnName(i);
                    bw.write(columnName);
                    if (ESpatialiteGeometryType.isGeometryName(columnTypeName)) {
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
                                if (object instanceof Clob) {
                                    object = rs.getString(j);
                                }
                                if (object != null) {
                                    bw.write(object.toString());
                                } else {
                                    bw.write("");
                                }
                            }
                        } else {
                            Object object = rs.getObject(j);
                            if (object instanceof Clob) {
                                object = rs.getString(j);
                            }
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

    @Override
    public List<Index> getIndexes( String tableName ) throws Exception {
        return sqliteDb.getIndexes(tableName);
    }

    @Override
    public void accept( IDbVisitor visitor ) throws Exception {
        sqliteDb.accept(visitor);
    }

}
