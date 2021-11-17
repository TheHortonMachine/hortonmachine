package org.hortonmachine.dbs.geopackage;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.h2.upgrade.DbUpgrade;
import org.hortonmachine.dbs.compat.ADb;
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
import org.hortonmachine.dbs.compat.IHmExtrasDb;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.datatypes.EDataType;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.datatypes.ESpatialiteGeometryType;
import org.hortonmachine.dbs.geopackage.Entry.DataType;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.spatialite.SpatialiteCommonMethods;
import org.hortonmachine.dbs.spatialite.SpatialiteGeometryColumns;
import org.hortonmachine.dbs.utils.BasicStyle;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.ITilesProducer;
import org.hortonmachine.dbs.utils.MercatorUtils;
import org.hortonmachine.dbs.utils.ResultSetToObjectFunction;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
public abstract class GeopackageCommonDb extends ASpatialDb implements IHmExtrasDb {

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
    public static final String COL_TILES_ZOOM_LEVEL = "zoom_level";
    public static final String COL_TILES_TILE_COLUMN = "tile_column";
    public static final String COL_TILES_TILE_ROW = "tile_row";
    public static final String COL_TILES_TILE_DATA = "tile_data";
    public static final String SELECTQUERY = "SELECT " + COL_TILES_TILE_DATA + " from %s where " + COL_TILES_ZOOM_LEVEL
            + "=? AND " + COL_TILES_TILE_COLUMN + "=? AND " + COL_TILES_TILE_ROW + "=?";
    public static final String INSERTQUERY = "insert or replace into %s (" + COL_TILES_ZOOM_LEVEL + ", " + COL_TILES_TILE_COLUMN
            + ", " + COL_TILES_TILE_ROW + ", " + COL_TILES_TILE_DATA + ") VALUES (?,?,?,?)";
    public static final int MERCATOR_SRID = 3857;
    public static final int WGS84LL_SRID = 4326;
    static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");
    protected ADb sqliteDb;

    protected boolean isGpgkInitialized = false;
    protected String gpkgVersion;
    /**
     * If true, this forces mobile compatibility, which means that:
     * 
     *  <ul>
     *      <li>tiles: accept only srid 3857</li>
     *      <li>vectors: accept only srid 4326</li>
     *  </ul>
     */
    protected boolean forceMobileCompatibility = false;

    public abstract void createFunctions() throws Exception;

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

        // see if we have to create the table structure
        long appId = execOnConnection(connection -> {
            // 1196444487 (the 32-bit integer value of 0x47504B47 or GPKG in ASCII) for GPKG 1.2 and
            // greater
            // 1196437808 (the 32-bit integer value of 0x47503130 or GP10 in ASCII) for GPKG 1.0 or
            // 1.1
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery("PRAGMA application_id")) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0l;
            }
        });
        if (0x47503130 == appId) {
            gpkgVersion = "1.0/1.1";
        } else if (0x47504B47 == appId) {
            gpkgVersion = "1.2";
        }

        createFunctions();

        isGpgkInitialized = gpkgVersion != null;

        try {
            String checkTable = "rtree_test_check";
            String checkRtree = "CREATE VIRTUAL TABLE " + checkTable + " USING rtree(id, minx, maxx, miny, maxy)";
            sqliteDb.executeInsertUpdateDeleteSql(checkRtree);
            String drop = "DROP TABLE " + checkTable;
            sqliteDb.executeInsertUpdateDeleteSql(drop);
            supportsSpatialIndex = true;
        } catch (Exception e) {
            supportsSpatialIndex = false;
        }

        if (!isGpgkInitialized) {
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
            addDefaultSpatialReferences();

            sqliteDb.executeInsertUpdateDeleteSql("PRAGMA application_id = 0x47503130;");
            gpkgVersion = "1.0/1.1";
        }

        if (mPrintInfos) {
            if (gpkgVersion != null)
                Logger.INSTANCE.insertInfo(null, "Geopackage Version: " + gpkgVersion);
        }
        return dbExists;
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {

    }

    public void setForceMobileCompatibility( boolean forceMobileCompatibility ) {
        this.forceMobileCompatibility = forceMobileCompatibility;
    }

    /** Returns list of contents of the geopackage. 
     * @throws Exception */
    public List<Entry> contents() throws Exception {

        return execOnConnection(connection -> {
            List<Entry> contents = new ArrayList<Entry>();
            String sql = "SELECT * FROM " + GEOPACKAGE_CONTENTS;
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String dt = rs.getString("data_type");
                    DataType type = Entry.DataType.of(dt);
                    Entry e = null;
                    switch( type ) {
                    case Feature:
                        e = createFeatureEntry(rs);
                        if (forceMobileCompatibility && e != null && e.srid != null && e.srid != WGS84LL_SRID) {
                            e = null;
                        }
                        break;
                    case Tile:
                        e = createTileEntry(rs);
                        if (forceMobileCompatibility && e != null && e.srid != null && e.srid != MERCATOR_SRID) {
                            e = null;
                        }
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
                    FeatureEntry e = createFeatureEntry(rs);
                    if (forceMobileCompatibility && e != null && e.srid != null && e.srid != WGS84LL_SRID) {
                        e = null;
                    }
                    if (e != null)
                        contents.add(e);
                }

                return contents;
            }
        });
    }

    /** Lists all the tile entries in the geopackage. */
    public List<TileEntry> tiles() throws Exception {
        return execOnConnection(connection -> {
            List<TileEntry> contents = new ArrayList<TileEntry>();
            String sql = format("SELECT a.*, c.organization_coordsys_id, c.definition" //
                    + " FROM %s a, %s c"//
                    + " WHERE a.srs_id = c.srs_id"//
                    + " AND a.data_type = ?", //
                    GEOPACKAGE_CONTENTS, SPATIAL_REF_SYS);

            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                pStmt.setString(1, DataType.Tile.value());

                IHMResultSet rs = pStmt.executeQuery();
                while( rs.next() ) {
                    TileEntry e = createTileEntry(rs);
                    if (forceMobileCompatibility && e != null && e.srid != null && e.srid != MERCATOR_SRID) {
                        e = null;
                    }
                    if (e != null)
                        contents.add(e);
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
        if (!sqliteDb.hasTable(GEOMETRY_COLUMNS)) {
            return null;
        }
        return sqliteDb.execOnConnection(connection -> {
            String sql = format(
                    "SELECT a.*, b.column_name, b.geometry_type_name, b.m, b.z, c.organization_coordsys_id, c.definition"
                            + " FROM %s a, %s b, %s c" + " WHERE a.table_name = b.table_name " + " AND a.srs_id = c.srs_id "
                            + " AND lower(a.table_name) = lower(?)" + " AND a.data_type = ?",
                    GEOPACKAGE_CONTENTS, GEOMETRY_COLUMNS, SPATIAL_REF_SYS);

            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                pStmt.setString(1, name);
                pStmt.setString(2, DataType.Feature.value());

                IHMResultSet rs = pStmt.executeQuery();
                if (rs.next()) {
                    FeatureEntry e = createFeatureEntry(rs);
                    if (forceMobileCompatibility && e != null && e.srid != null && e.srid != WGS84LL_SRID) {
                        return null;
                    }
                    return e;
                }
                return null;
            }
        });
    }

    /**
     * Looks up a tile entry by name.
     *
     * @param name THe name of the tile entry.
     * @return The entry, or <code>null</code> if no such entry exists.
     */
    public TileEntry tile( String name ) throws Exception {
        if (!sqliteDb.hasTable(GEOMETRY_COLUMNS)) {
            return null;
        }
        return sqliteDb.execOnConnection(connection -> {
            String sql = format("SELECT a.*, c.organization_coordsys_id, c.definition"//
                    + " FROM %s a, %s c"//
                    + " WHERE a.srs_id = c.srs_id"//
                    + " AND Lower(a.table_name) = Lower(?)"//
                    + " AND a.data_type = ?", //
                    GEOPACKAGE_CONTENTS, SPATIAL_REF_SYS);

            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                pStmt.setString(1, name);
                pStmt.setString(2, DataType.Tile.value());

                IHMResultSet rs = pStmt.executeQuery();
                if (rs.next()) {
                    TileEntry e = createTileEntry(rs);
                    if (forceMobileCompatibility && e != null && e.srid != null && e.srid != MERCATOR_SRID) {
                        return null;
                    }
                    return e;
                }
                return null;
            }
        });
    }

    /**
     * Get a Tile's image bytes from the database for a given table.
     * 
     * @param tableName the table name to get the image from.
     * @param tx the x tile index.
     * @param ty the y tile index, the osm way.
     * @param zoom the zoom level.
     * @return the tile image bytes.
     * @throws Exception
     */
    public byte[] getTile( String tableName, int tx, int ty, int zoom ) throws Exception {
        // if (tileRowType.equals("tms")) { // if it is not OSM way
        // int[] tmsTileXY = MercatorUtils.osmTile2TmsTile(tx, ty, zoom);
        // ty = tmsTileXY[1];
        // }
        String sql = format(SELECTQUERY, DbsUtilities.fixTableName(tableName));
        return sqliteDb.execOnConnection(connection -> {
            byte[] imageBytes = null;
            try (IHMPreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, zoom);
                statement.setInt(2, tx);
                statement.setInt(3, ty);
                IHMResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    imageBytes = resultSet.getBytes(1);
                }
            }

            return imageBytes;
        });
    }

    /**
     * Get a Tile's image bytes from the database for a given table.
     * 
     * @param tableName the table name to get the image from.
     * @param lon the longitude of interest.
     * @param lat the latitude of interest.
     * @param zoom the zoom level.
     * @return the tile image bytes.
     * @throws Exception
     */
    public byte[] getTile( String tableName, double lon, double lat, int zoom ) throws Exception {
        int[] zxy = MercatorUtils.getTileNumber(lat, lon, zoom);
        return getTile(tableName, zxy[1], zxy[2], zoom);
    }

    /**
     * Get the list of zoomlevels that contain data.
     * 
     * @param tableName the name of the table.
     * @return the list of zoom levels.
     * @throws Exception
     */
    public List<Integer> getTileZoomLevelsWithData( String tableName ) throws Exception {
        String sql = "select distinct " + COL_TILES_ZOOM_LEVEL + " from " + DbsUtilities.fixTableName(tableName) + " order by "
                + COL_TILES_ZOOM_LEVEL;

        return sqliteDb.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                List<Integer> list = new ArrayList<>();
                while( rs.next() ) {
                    int zoomLevel = rs.getInt(1);
                    list.add(zoomLevel);
                }
                return list;
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
        initEntry(rs, e);
        e.setGeometryColumn(rs.getString("column_name"));
        e.setGeometryType(EGeometryType.forTypeName(rs.getString("geometry_type_name")));
        e.setZ(rs.getBoolean("z"));
        e.setM(rs.getBoolean("m"));
        return e;
    }

    private void initEntry( IHMResultSet rs, Entry e ) throws Exception {
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
    }

    private TileEntry createTileEntry( IHMResultSet rs ) throws Exception {
        TileEntry e = new TileEntry();
        initEntry(rs, e);

        // load all the tile matrix entries (and join with the data table to see if a certain level
        // has tiles available, given the indexes in the data table, it should be real quick)
        sqliteDb.execOnConnection(connection -> {
            String sql = format(
                    "SELECT *, exists(SELECT 1 FROM %s data where data.zoom_level = tileMatrix.zoom_level) as has_tiles"
                            + " FROM %s as tileMatrix" + " WHERE table_name = ?" + " ORDER BY zoom_level ASC",
                    DbsUtilities.fixTableName(e.getTableName()), TILE_MATRIX_METADATA);
            try (IHMPreparedStatement pstmt = connection.prepareStatement(sql);) {
                pstmt.setString(1, e.getTableName());
                IHMResultSet rsm = pstmt.executeQuery();
                while( rsm.next() ) {
                    TileMatrix m = new TileMatrix();
                    m.setZoomLevel(rsm.getInt("zoom_level"));
                    m.setMatrixWidth(rsm.getInt("matrix_width"));
                    m.setMatrixHeight(rsm.getInt("matrix_height"));
                    m.setTileWidth(rsm.getInt("tile_width"));
                    m.setTileHeight(rsm.getInt("tile_height"));
                    m.setXPixelSize(rsm.getDouble("pixel_x_size"));
                    m.setYPixelSize(rsm.getDouble("pixel_y_size"));
                    m.setTiles(rsm.getBoolean("has_tiles"));

                    e.getTileMatricies().add(m);
                }
                return "";
            }
        });

        // use the tile matrix set bounds rather that gpkg_contents bounds
        // per spec, the tile matrix set bounds should be exact and used to calculate tile
        // coordinates
        // and in contrast the gpkg_contents is "informational" only
        sqliteDb.execOnConnection(connection -> {
            String sql = format("SELECT * FROM %s a, %s b WHERE lower(a.table_name) = lower(?) AND a.srs_id = b.srs_id LIMIT 1",
                    TILE_MATRIX_SET, SPATIAL_REF_SYS);
            try (IHMPreparedStatement pstmt = connection.prepareStatement(sql);) {
                pstmt.setString(1, e.getTableName());
                IHMResultSet rsm = pstmt.executeQuery();
                if (rsm.next()) {
                    int srid = rsm.getInt("organization_coordsys_id");
                    e.setSrid(srid);
                    e.setTileMatrixSetBounds(new Envelope(rsm.getDouble("min_x"), rsm.getDouble("max_x"), rsm.getDouble("min_y"),
                            rsm.getDouble("max_y")));
                }
                return "";
            }
        });

        return e;
    }

    private void runScript( String filename ) throws Exception {
        InputStream resourceAsStream = GeopackageCommonDb.class.getResourceAsStream(filename);
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

    private void addDefaultSpatialReferences() throws Exception {
        try {
            addCRS(-1, "Undefined cartesian SRS", "NONE", -1, "undefined", "undefined cartesian coordinate reference system");
            addCRS(0, "Undefined geographic SRS", "NONE", 0, "undefined", "undefined geographic coordinate reference system");
            addCRS(WGS84LL_SRID, "WGS 84 geodetic", "EPSG", WGS84LL_SRID,
                    "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\","
                            + "6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],"
                            + "PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,"
                            + "AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]",
                    "longitude/latitude coordinates in decimal degrees on the WGS 84 spheroid");
            addCRS(MERCATOR_SRID, "WGS 84 Pseudo-Mercator", "EPSG", MERCATOR_SRID, "PROJCS[\"WGS 84 / Pseudo-Mercator\", \n"
                    + "  GEOGCS[\"WGS 84\", \n" + "    DATUM[\"World Geodetic System 1984\", \n"
                    + "      SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], \n"
                    + "      AUTHORITY[\"EPSG\",\"6326\"]], \n"
                    + "    PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], \n"
                    + "    UNIT[\"degree\", 0.017453292519943295], \n" + "    AXIS[\"Geodetic longitude\", EAST], \n"
                    + "    AXIS[\"Geodetic latitude\", NORTH], \n" + "    AUTHORITY[\"EPSG\",\"4326\"]], \n"
                    + "  PROJECTION[\"Popular Visualisation Pseudo Mercator\", AUTHORITY[\"EPSG\",\"1024\"]], \n"
                    + "  PARAMETER[\"semi-minor axis\", 6378137.0], \n" + "  PARAMETER[\"Latitude of false origin\", 0.0], \n"
                    + "  PARAMETER[\"Longitude of natural origin\", 0.0], \n"
                    + "  PARAMETER[\"Scale factor at natural origin\", 1.0], \n" + "  PARAMETER[\"False easting\", 0.0], \n"
                    + "  PARAMETER[\"False northing\", 0.0], \n" + "  UNIT[\"m\", 1.0], \n" + "  AXIS[\"Easting\", EAST], \n"
                    + "  AXIS[\"Northing\", NORTH], \n" + "  AUTHORITY[\"EPSG\",\"3857\"]]",
                    "WGS 84 Pseudo-Mercator, often referred to as Webmercator.");
        } catch (IOException ex) {
            throw new SQLException("Unable to add default spatial references.", ex);
        }
    }

    /**
     * Adds a crs to the geopackage, registering it in the spatial_ref_sys table.
     * @throws Exception 
     */
    public void addCRS( String auth, int srid, String wkt ) throws Exception {
        addCRS(srid, auth + ":" + srid, auth, srid, wkt, auth + ":" + srid);
    }

    public void addCRS( int srid, String srsName, String organization, int organizationCoordSysId, String definition,
            String description ) throws Exception {
        try {
            boolean hasAlready = hasCrs(srid);
            if (hasAlready)
                return;

            String sqlPrep1 = String
                    .format("INSERT INTO %s (srs_id, srs_name, organization, organization_coordsys_id, definition, description) "
                            + "VALUES (?,?,?,?,?,?)", SPATIAL_REF_SYS);

            boolean ok = sqliteDb.execOnConnection(connection -> {
                try (IHMPreparedStatement pStmt = connection.prepareStatement(sqlPrep1)) {
                    int i = 1;
                    pStmt.setInt(i++, srid);
                    pStmt.setString(i++, srsName);
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

    public boolean hasCrs( int srid ) throws Exception {
        String sqlPrep = String.format("SELECT srs_id FROM %s WHERE srs_id = ?", SPATIAL_REF_SYS);
        return sqliteDb.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(sqlPrep)) {
                pStmt.setInt(1, srid);
                IHMResultSet resultSet = pStmt.executeQuery();
                if (resultSet.next()) {
                    return true;
                }
                return false;
            }
        });
    }

    public static class Tile {
        public int x;
        public int y;
        public int z;
        public byte[] imageBytes;
    }

    @Override
    public String getJdbcUrlPre() {
        return sqliteDb.getJdbcUrlPre();
    }

    public Connection getJdbcConnection() throws Exception {
        return sqliteDb.getJdbcConnection();
    }

    @Override
    public IHMConnection getConnectionInternal() throws Exception {
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

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(tableName).append("(");
        for( int i = 0; i < fieldData.length; i++ ) {
            if (i != 0)
                sb.append(",");
            sb.append(fieldData[i]);
        }
        sb.append(",").append(geometryFieldData);
        if (foreignKeys != null) {
            for( int i = 0; i < foreignKeys.length; i++ ) {
                sb.append(",");
                sb.append(foreignKeys[i]);
            }
        }
        sb.append(")");

        execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                stmt.execute(sb.toString());
            }
            return null;
        });

        String[] g = geometryFieldData.split("\\s+");
        String dataType = Entry.DataType.Feature.value();
        addGeoPackageContentsEntry(dataType, tableName, tableSrid, null, null);
        addGeometryColumnsEntry(tableName, g[0], g[1], tableSrid, false, false);

        addGeometryXYColumnAndIndex(tableName, g[0], g[1], String.valueOf(tableSrid), avoidIndex);
    }

    @Override
    public Envelope getTableBounds( String tableName ) throws Exception {
        // TODO
        throw new RuntimeException("Not implemented yet...");
    }

    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, int limit, int reprojectSrid, String whereStr )
            throws Exception {
        tableName = DbsUtilities.fixTableName(tableName);
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
            columnName = DbsUtilities.fixColumnName(columnName);

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
                    // FIXME this will not work, needs to be done outside the db
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
            case BOOLEAN: {
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
            case DATETIME:
            case DATE: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            String date = resultSet.getString(index);
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
        if (envelope != null && supportsSpatialIndex) {
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
                    boolean addGeometry = true;
                    Object[] rec = new Object[columnCount];
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (queryResult.geometryIndex == j - 1) {
                            Geometry geometry = gp.fromResultSet(rs, j);
                            if (geometry != null) {
                                if (!supportsSpatialIndex && envelope != null) {
                                    // need to check manually
                                    if (!geometry.getEnvelopeInternal().intersects(envelope)) {
                                        addGeometry = false;
                                        break;
                                    }
                                }
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
                    if (addGeometry)
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
        HashMap<String, List<String>> tablesMap = GeopackageTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }

    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        if (!supportsSpatialIndex)
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

    public String getPrimaryKey( String tableName ) throws Exception {
        String pk = SpatialiteCommonMethods.getPrimaryKey(sqliteDb, tableName);
        return pk;
    }

    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception {
        // this is not possible in gpkg, backing on envelope intersection
        Envelope env = geometry.getEnvelopeInternal();
        return getSpatialindexBBoxWherePiece(tableName, alias, env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
    }

    public GeometryColumn getGeometryColumnsForTable( String tableName ) throws Exception {
        FeatureEntry feature = feature(tableName);
        if (feature == null)
            return null;
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
        return sqliteDb.getTables(doOrder);
        // Stream<String> map = features().stream().map(e -> e.tableName);
        // if (doOrder) {
        // map = map.sorted();
        // }
        // return map.collect(Collectors.toList());
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
        String sql = getType().getSqlTemplates().dropTable(tableName, null);
        sqliteDb.executeInsertUpdateDeleteSql(sql);
    }

    @Override
    public void insertGeometry( String tableName, Geometry geometry, String epsgStr, String where ) throws Exception {
        int epsg = 4326;
        if (epsgStr != null) {
            try {
                epsg = Integer.parseInt(epsgStr);
            } catch (Exception e) {
            }
        }

        IGeometryParser gp = getType().getGeometryParser();
        geometry.setSRID(epsg);
        Object obj = gp.toSqlObject(geometry);
        GeometryColumn gc = getGeometryColumnsForTable(tableName);
        String sql = "INSERT INTO " + tableName + " (" + gc.geometryColumnName + ") VALUES (?)";
        if (where != null) {
            sql = "UPDATE " + tableName + " SET " + gc.geometryColumnName + "=? WHERE " + where;
        }
        String _sql = sql;

        execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(_sql)) {
                pStmt.setObject(1, obj);
                pStmt.executeUpdate();
            }
            return null;
        });

    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg,
            boolean avoidIndex ) throws Exception {
        if (!avoidIndex)
            createSpatialIndex(tableName, geomColName);
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg )
            throws Exception {
        createSpatialIndex(tableName, geomColName);
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
            IGeometryParser gp = getType().getGeometryParser();
            long start = System.currentTimeMillis();
            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (j == geometryIndex) {
                        Geometry geometry = gp.fromResultSet(rs, j);
                        if (geometry != null) {
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
            IGeometryParser gp = getType().getGeometryParser();
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
                        if (j == geometryIndex) {
                            try {
                                Geometry geometry = gp.fromResultSet(rs, j);
                                if (geometry == null) {
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

    /**
     * Create a spatial index
     *
     * @param e feature entry to create spatial index for
     */
    public void createSpatialIndex( String tableName, String geometryName ) throws Exception {
        Map<String, String> properties = new HashMap<String, String>();

        String pk = SpatialiteCommonMethods.getPrimaryKey(sqliteDb, tableName);
        if (pk == null) {
            throw new IOException("Spatial index only supported for primary key of single column.");
        }
        properties.put("t", tableName);
        properties.put("c", geometryName);
        properties.put("i", pk);

        InputStream resourceAsStream = GeopackageCommonDb.class.getResourceAsStream(SPATIAL_INDEX + ".sql");
        runScript(resourceAsStream, getJdbcConnection(), properties);
    }

    public void runScript( InputStream stream, Connection cx, Map<String, String> properties ) throws SQLException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        Statement st = cx.createStatement();
        int insideBlock = 0;

        try {
            StringBuilder buf = new StringBuilder();
            String sql = reader.readLine();
            while( sql != null ) {
                sql = sql.trim();
                if (!sql.isEmpty() && !sql.startsWith("--")) {
                    buf.append(sql).append(" ");

                    if (sql.startsWith("BEGIN")) {
                        insideBlock++;
                    } else if (insideBlock > 0 && sql.startsWith("END")) {
                        insideBlock--;
                    }

                    if (sql.endsWith(";") && insideBlock == 0) {
                        Matcher matcher = PROPERTY_PATTERN.matcher(buf);
                        while( matcher.find() ) {
                            String propertyName = matcher.group(1);
                            String propertyValue = properties.get(propertyName);
                            if (propertyValue == null) {
                                throw new RuntimeException("Missing property " + propertyName + " for sql script");
                            } else {
                                buf.replace(matcher.start(), matcher.end(), propertyValue);
                                matcher.reset();
                            }
                        }

                        String stmt = buf.toString();

                        st.addBatch(stmt);

                        buf.setLength(0);
                    }
                }
                sql = reader.readLine();
            }
            st.executeBatch();
        } catch (IOException e) {
            throw new SQLException(e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                Logger.INSTANCE.insertError("GeopackageDb#runScript", e.getMessage(), e);
            }
            try {
                st.close();
            } catch (SQLException e) {
                Logger.INSTANCE.insertError("GeopackageDb#runScript", e.getMessage(), e);
            }
        }
    }

    private void addGeoPackageContentsEntry( String dataType, String tableName, int srid, String description, Envelope crsBounds )
            throws Exception {
        if (!hasCrs(srid))
            throw new IOException("The srid is not yet present in the package. Please add it before proceeding.");

        final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));

        StringBuilder sb = new StringBuilder();
        StringBuilder vals = new StringBuilder();

        sb.append(format("INSERT INTO %s (table_name, data_type, identifier", GEOPACKAGE_CONTENTS));
        vals.append("VALUES (?,?,?");

        if (description != null) {
            sb.append(", description");
            vals.append(",?");
        }

        sb.append(", min_x, min_y, max_x, max_y");
        vals.append(",?,?,?,?");

        sb.append(", srs_id");
        vals.append(",?");
        sb.append(") ").append(vals.append(")").toString());

        sqliteDb.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(sb.toString())) {
                double minx = 0;
                double miny = 0;
                double maxx = 0;
                double maxy = 0;
                if (crsBounds != null) {
                    minx = crsBounds.getMinX();
                    miny = crsBounds.getMinY();
                    maxx = crsBounds.getMaxX();
                    maxy = crsBounds.getMaxY();
                }

                int i = 1;
                pStmt.setString(i++, tableName);
                pStmt.setString(i++, dataType);
                pStmt.setString(i++, tableName);
                if (description != null)
                    pStmt.setString(i++, description);
                pStmt.setDouble(i++, minx);
                pStmt.setDouble(i++, miny);
                pStmt.setDouble(i++, maxx);
                pStmt.setDouble(i++, maxy);
                pStmt.setInt(i++, srid);

                pStmt.executeUpdate();
                return null;
            }
        });

    }

    private void updateGeoPackageContentsEntry( String tableName, Envelope crsBounds ) throws Exception {
        String sql = "update " + GEOPACKAGE_CONTENTS + " set min_x=?, min_y=?, max_x=?, max_y=? where table_name=?";
        sqliteDb.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                double minx = 0;
                double miny = 0;
                double maxx = 0;
                double maxy = 0;
                if (crsBounds != null) {
                    minx = crsBounds.getMinX();
                    miny = crsBounds.getMinY();
                    maxx = crsBounds.getMaxX();
                    maxy = crsBounds.getMaxY();
                }
                int i = 1;
                pStmt.setDouble(i++, minx);
                pStmt.setDouble(i++, miny);
                pStmt.setDouble(i++, maxx);
                pStmt.setDouble(i++, maxy);
                pStmt.setString(i++, tableName);

                pStmt.executeUpdate();
                return null;
            }
        });

    }

    private void addGeometryColumnsEntry( String tableName, String geometryName, String geometryType, int srid, boolean hasZ,
            boolean hasM ) throws Exception {
        // geometryless tables should not be inserted into this table.
        String sql = format("INSERT INTO %s VALUES (?, ?, ?, ?, ?, ?);", GEOMETRY_COLUMNS);

        sqliteDb.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                int i = 1;
                pStmt.setString(i++, tableName);
                pStmt.setString(i++, geometryName);
                pStmt.setString(i++, geometryType);
                pStmt.setInt(i++, srid);
                pStmt.setInt(i++, hasZ ? 1 : 0);
                pStmt.setInt(i++, hasM ? 1 : 0);

                pStmt.executeUpdate();
                return null;
            }
        });
    }

    public BasicStyle getBasicStyle( String tableName ) throws Exception {
        checkStyleTable(sqliteDb);

        return sqliteDb.execOnConnection(connection -> {
            BasicStyle basicStyle = new BasicStyle();
            String sql = "select simplified from " + HM_STYLES_TABLE + " where lower(tablename)='" + tableName.toLowerCase()
                    + "'";
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    String styleJson = rs.getString(1);
                    basicStyle.setFromJson(styleJson);
                }
            }
            return basicStyle;
        });
    }

    public void updateSimplifiedStyle( String tableName, String simplified ) throws Exception {
        Long count = sqliteDb.getLong(
                "select count(*) from " + HM_STYLES_TABLE + " where tablename='" + DbsUtilities.fixTableName(tableName) + "'");
        String sql;
        if (count == 0) {
            sql = "INSERT INTO " + HM_STYLES_TABLE + "(tablename, simplified) VALUES(?,?)";
            sqliteDb.execOnConnection(connection -> {
                try (IHMPreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, tableName);
                    pstmt.setString(2, simplified);
                    pstmt.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
        } else {
            sql = "update " + HM_STYLES_TABLE + " set simplified=? where tablename=?";
            sqliteDb.execOnConnection(connection -> {
                try (IHMPreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, simplified);
                    pstmt.setString(2, tableName);
                    pstmt.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
        }
    }

    /**
     * Creates a new tile entry in the geopackage.
     * 
     * <p>If the table exists, data are added to that one.
     * <p>Only 3857 tiles are supported. The data need to be in that projection.
     *
     */
    public int addTilestable( String tableName, String description, Envelope dataBounds3857, ITilesProducer tilesProducer )
            throws Exception {
        String fixedTableName = DbsUtilities.fixTableName(tableName);
        int insertedCount = 0;

        TileEntry tileEntry = tile(fixedTableName);
        if (tileEntry == null) {
            String createTableSql = format("CREATE TABLE %s (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
                    + COL_TILES_ZOOM_LEVEL + " INTEGER NOT NULL, " + COL_TILES_TILE_COLUMN + " INTEGER NOT NULL,"
                    + COL_TILES_TILE_ROW + " INTEGER NOT NULL, " + COL_TILES_TILE_DATA + " BLOB NOT NULL)", fixedTableName);
            sqliteDb.executeInsertUpdateDeleteSql(createTableSql);
            // update the metadata tables
            addGeoPackageContentsEntry(DataType.Tile.value, tableName, MERCATOR_SRID, description, dataBounds3857);
        } else {
            updateGeoPackageContentsEntry(fixedTableName, dataBounds3857);
        }

        // insert the tiles
        double w = dataBounds3857.getMinX();
        double s = dataBounds3857.getMinY();
        double e = dataBounds3857.getMaxX();
        double n = dataBounds3857.getMaxY();
        Envelope areaConstraint = tilesProducer.areaConstraint();
        if (areaConstraint != null) {
            w = Math.max(w, areaConstraint.getMinX());
            s = Math.max(s, areaConstraint.getMinY());
            e = Math.min(e, areaConstraint.getMaxX());
            n = Math.min(n, areaConstraint.getMaxY());
        }

        int tileSize = tilesProducer.getTileSize();
        int minZoom = tilesProducer.getMinZoom();
        int maxZoom = tilesProducer.getMaxZoom();

        List<Tile> tilesList = new ArrayList<>();
        for( int z = minZoom; z <= maxZoom; z++ ) {
            if (tilesProducer.cancelled()) {
                break;
            }

            // get ul and lr tile number
            int[] llTileNumber = MercatorUtils.getTileNumberFrom3857(new Coordinate(w, s), z);
            int[] urTileNumber = MercatorUtils.getTileNumberFrom3857(new Coordinate(e, n), z);

            int startXTile = llTileNumber[1];
            int startYTile = urTileNumber[2];
            int endXTile = urTileNumber[1];
            int endYTile = llTileNumber[2];

            int xTileCount = 1 + endXTile - startXTile;
            int yTileCount = 1 + endYTile - startYTile;
            int count = xTileCount * yTileCount;
            tilesProducer.startWorkingOnZoomLevel(z, count);

            final Envelope levelBounds3857 = new Envelope();
            if (tileEntry != null) {
                Envelope bound = tileEntry.getTileMatrixSetBounds();
                levelBounds3857.expandToInclude(bound);
            }

            Envelope sampleTileBounds3857 = null;
            for( int x = startXTile; x <= endXTile; x++ ) {
                if (tilesProducer.cancelled()) {
                    break;
                }
                for( int y = startYTile; y <= endYTile; y++ ) {
                    if (tilesProducer.cancelled()) {
                        break;
                    }

                    Envelope tileBounds3857 = MercatorUtils.tileBounds3857(x, y, z);
                    if (!dataBounds3857.intersects(tileBounds3857)) {
                        continue;
                    }
                    byte[] imageData = tilesProducer.getTileData(tileBounds3857);
                    if (imageData != null) {
                        if (sampleTileBounds3857 == null) {
                            sampleTileBounds3857 = tileBounds3857;
                        }
                        levelBounds3857.expandToInclude(tileBounds3857);

                        Tile tile = new Tile();
                        tile.x = x;
                        tile.y = y;
                        tile.z = z;
                        tile.imageBytes = imageData;
                        tilesList.add(tile);

                        if (tilesList.size() == 1000) {
                            putTiles(fixedTableName, tilesList);
                            insertedCount += tilesList.size();
                            tilesList.clear();
                        }
                    }
                    tilesProducer.worked();
                }
            }
            tilesProducer.done();

            String sql = format("INSERT or replace INTO %s (table_name, srs_id, min_x, min_y, max_x, max_y) VALUES (?,?,?,?,?,?)",
                    TILE_MATRIX_SET);
            sqliteDb.executeInsertUpdateDeletePreparedSql(sql, new Object[]{tableName, MERCATOR_SRID, levelBounds3857.getMinX(),
                    levelBounds3857.getMinY(), levelBounds3857.getMaxX(), levelBounds3857.getMaxY()});

            sql = format(
                    "INSERT or replace INTO %s (table_name, zoom_level, matrix_width, matrix_height, tile_width, tile_height, pixel_x_size, pixel_y_size) VALUES (?,?,?,?,?,?,?,?)",
                    TILE_MATRIX_METADATA);
            if (tileEntry == null) {
                sqliteDb.executeInsertUpdateDeletePreparedSql(sql, new Object[]{tableName, z, xTileCount, yTileCount, tileSize,
                        tileSize, sampleTileBounds3857.getWidth() / tileSize, sampleTileBounds3857.getHeight() / tileSize});
            } else {
                List<TileMatrix> tileMatricies = tileEntry.getTileMatricies();
                int _z = z;
                Optional<TileMatrix> firstZ = tileMatricies.stream().filter(tm -> tm.getZoomLevel() == _z).findFirst();
                if (firstZ.isPresent()) {
                    TileMatrix tm = firstZ.get();
                    sqliteDb.executeInsertUpdateDeletePreparedSql(sql,
                            new Object[]{tableName, z, tm.matrixWidth + xTileCount, tm.matrixHeight + yTileCount, tileSize,
                                    tileSize, sampleTileBounds3857.getWidth() / tileSize,
                                    sampleTileBounds3857.getHeight() / tileSize});
                }
            }
        }

        if (tilesList.size() > 0) {
            putTiles(fixedTableName, tilesList);
            insertedCount += tilesList.size();
        }

        // add index at the end
        if (tileEntry == null) {
            String createIndexSql = format("create index idx%s_zyx_idx on %s(" + COL_TILES_ZOOM_LEVEL + ", "
                    + COL_TILES_TILE_COLUMN + ", " + COL_TILES_TILE_ROW + ");", tableName, fixedTableName);
            sqliteDb.executeInsertUpdateDeleteSql(createIndexSql);
        }

        return insertedCount;
    }

    /**
     * Add a tile to the geopackage.
     *
     * @param tableName the tabel into which to insert the tile.
     * @param tx the col of the tile.
     * @param ty the row of the tile.
     * @param zoom the zoomlevel of the tile.
     * @param tileData the tile image data.
     * @throws Exception
     */
    public void putTile( String tableName, int tx, int ty, int zoom, byte[] tileData ) throws Exception {
        String sql = format(INSERTQUERY, tableName);
        sqliteDb.execOnConnection(connection -> {
            try (IHMPreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, zoom);
                pstmt.setInt(2, tx);
                pstmt.setInt(3, ty);
                pstmt.setBytes(4, tileData);
                pstmt.executeUpdate();
                return null;
            }
        });
    }

    public void putTiles( String tableName, List<Tile> tilesList ) throws Exception {
        String sql = format(INSERTQUERY, tableName);

        sqliteDb.execOnConnection(connection -> {
            boolean autoCommit = connection.getAutoCommit();
            connection.enableAutocommit(false);
            try {
                try (IHMPreparedStatement pstmt = connection.prepareStatement(sql)) {

                    for( Tile tile : tilesList ) {
                        pstmt.setInt(1, tile.z);
                        pstmt.setInt(2, tile.x);
                        pstmt.setInt(3, tile.y);
                        pstmt.setBytes(4, tile.imageBytes);
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                    return null;
                }
            } finally {
                connection.enableAutocommit(autoCommit);
            }
        });
    }

    public GeopackageCommonDb() {
        super();
    }

}