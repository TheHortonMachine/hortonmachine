package org.jgrasstools.gears.modules.r.tmsgenerator;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.imageio.ImageIO;

import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;

public class MBTilesHelper {
    static {
        try {
            // make sure sqlite drivers are there
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB);
    public final static String TABLE_TILES = "tiles";
    public final static String COL_TILES_ZOOM_LEVEL = "zoom_level";
    public final static String COL_TILES_TILE_COLUMN = "tile_column";
    public final static String COL_TILES_TILE_ROW = "tile_row";
    public final static String COL_TILES_TILE_DATA = "tile_data";

    private final static String CREATE_TILES = //
    "CREATE TABLE " + TABLE_TILES + "( " + //
            COL_TILES_ZOOM_LEVEL + " INTEGER, " + //
            COL_TILES_TILE_COLUMN + " INTEGER, " + //
            COL_TILES_TILE_ROW + " INTEGER, " + //
            COL_TILES_TILE_DATA + " BLOB" + //
            ")";

    // TABLE METADATA (name TEXT, value TEXT);
    public final static String TABLE_METADATA = "metadata";
    public final static String COL_METADATA_NAME = "name";
    public final static String COL_METADATA_VALUE = "value";

    private final static String CREATE_METADATA = //
    "CREATE TABLE " + TABLE_METADATA + "( " + //
            COL_METADATA_NAME + " TEXT, " + //
            COL_METADATA_VALUE + " TEXT " + //
            ")";

    // INDEXES on Metadata and Tiles tables
    private final static String INDEX_TILES = "CREATE UNIQUE INDEX tile_index ON " + TABLE_TILES + " (" + COL_TILES_ZOOM_LEVEL
            + ", " + COL_TILES_TILE_COLUMN + ", " + COL_TILES_TILE_ROW + ")";
    private final static String INDEX_METADATA = "CREATE UNIQUE INDEX name ON " + TABLE_METADATA + "( " + COL_METADATA_NAME + ")";
    private Connection connection;

    public void open( File dbFile ) throws SQLException {
        // create a database connection
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    public void close() {
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            // connection close failed.
            throw new ModelsRuntimeException("An error occurred while closing the database connection.", this);
        }
    }

    public void createTables( boolean makeIndexes ) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.addBatch("DROP TABLE IF EXISTS " + TABLE_TILES);
            statement.addBatch("DROP TABLE IF EXISTS " + TABLE_METADATA);
            statement.addBatch(CREATE_TILES);
            statement.addBatch(CREATE_METADATA);
            if (makeIndexes) {
                statement.addBatch(INDEX_TILES);
                statement.addBatch(INDEX_METADATA);
            }
            statement.executeBatch();
        } finally {
            if (statement != null)
                statement.close();
        }
    }

    public void createIndexes() throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.addBatch(INDEX_TILES);
            statement.addBatch(INDEX_METADATA);
            statement.executeBatch();
        } finally {
            if (statement != null)
                statement.close();
        }
    }

    public void fillMetadata( float n, float s, float w, float e, String name, String format, int minZoom, int maxZoom )
            throws SQLException {
        // type = baselayer
        // version = 1.1
        // descritpion = name
        Statement statement = null;
        try {
            statement = connection.createStatement();
            String query = toMetadataQuery("name", name);
            statement.addBatch(query);
            query = toMetadataQuery("description", name);
            statement.addBatch(query);
            query = toMetadataQuery("format", format);
            statement.addBatch(query);
            query = toMetadataQuery("minZoom", minZoom + "");
            statement.addBatch(query);
            query = toMetadataQuery("maxZoom", maxZoom + "");
            statement.addBatch(query);
            query = toMetadataQuery("type", "baselayer");
            statement.addBatch(query);
            query = toMetadataQuery("version", "1.1");
            statement.addBatch(query);
            // left, bottom, right, top
            query = toMetadataQuery("bounds", w + "," + s + "," + e + "," + n);
            statement.addBatch(query);

            statement.executeBatch();
        } finally {
            if (statement != null)
                statement.close();
        }

    }

    private String toMetadataQuery( String key, String value ) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO " + TABLE_METADATA + " ");
        sb.append("(");
        sb.append(COL_METADATA_NAME);
        sb.append(",");
        sb.append(COL_METADATA_VALUE);
        sb.append(") values ('");
        sb.append(key);
        sb.append("','");
        sb.append(value);
        sb.append("')");
        String query = sb.toString();
        return query;
    }

    public void addTile( int x, int y, int z, BufferedImage image, String format ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        byte[] res = baos.toByteArray();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO " + TABLE_TILES + " ");
        sb.append("(");
        sb.append(COL_TILES_ZOOM_LEVEL);
        sb.append(",");
        sb.append(COL_TILES_TILE_COLUMN);
        sb.append(",");
        sb.append(COL_TILES_TILE_ROW);
        sb.append(",");
        sb.append(COL_TILES_TILE_DATA);
        sb.append(") values (");
        sb.append(z);
        sb.append(",");
        sb.append(x);
        sb.append(",");
        sb.append(y);
        sb.append(",");
        sb.append("?");
        sb.append(")");
        String query = sb.toString();

        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query);
            statement.setBytes(1, res);
            statement.execute();
        } finally {
            if (statement != null)
                statement.close();
        }
    }

    // public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
    // db.execSQL("DROP TABLE IF EXISTS " + TABLE_TILES);
    // db.execSQL("DROP TABLE IF EXISTS " + TABLE_METADATA);
    // onCreate(db);
    // }

}
