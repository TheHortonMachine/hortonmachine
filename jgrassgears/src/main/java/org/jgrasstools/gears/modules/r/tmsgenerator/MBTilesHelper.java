package org.jgrasstools.gears.modules.r.tmsgenerator;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.utils.images.ImageUtilities;

public class MBTilesHelper implements AutoCloseable {

    static {
        try {
            // make sure sqlite drivers are there
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * We have a fixed tile size.
     */
    public final static int TILESIZE = 256;

    // TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB);
    public final static String TABLE_TILES = "tiles";
    public final static String COL_TILES_ZOOM_LEVEL = "zoom_level";
    public final static String COL_TILES_TILE_COLUMN = "tile_column";
    public final static String COL_TILES_TILE_ROW = "tile_row";
    public final static String COL_TILES_TILE_DATA = "tile_data";

    public final static String SELECTQUERY = "SELECT " + COL_TILES_TILE_DATA + " from " + TABLE_TILES + " where "
        + COL_TILES_ZOOM_LEVEL + "=? AND " + COL_TILES_TILE_COLUMN + "=? AND " + COL_TILES_TILE_ROW + "=?";

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

    private final static String SELECT_BOUNDS = //
        "select " + COL_METADATA_VALUE + " from " + TABLE_METADATA + " where " + COL_METADATA_NAME + "='bounds'";

    private final static String SELECT_IMAGEFORMAT = //
        "select " + COL_METADATA_VALUE + " from " + TABLE_METADATA + " where " + COL_METADATA_NAME + "='format'";

    // INDEXES on Metadata and Tiles tables
    private final static String INDEX_TILES = "CREATE UNIQUE INDEX tile_index ON " + TABLE_TILES + " ("
        + COL_TILES_ZOOM_LEVEL + ", " + COL_TILES_TILE_COLUMN + ", " + COL_TILES_TILE_ROW + ")";
    private final static String INDEX_METADATA =
        "CREATE UNIQUE INDEX name ON " + TABLE_METADATA + "( " + COL_METADATA_NAME + ")";

    private Connection connection;

    private volatile int addedTiles = 0;

    private String imageFormat;

    public void open(File dbFile) throws SQLException {
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

    public void createTables(boolean makeIndexes) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.addBatch("DROP TABLE IF EXISTS " + TABLE_TILES);
            statement.addBatch("DROP TABLE IF EXISTS " + TABLE_METADATA);
            statement.addBatch(CREATE_TILES);
            statement.addBatch(CREATE_METADATA);
            if (makeIndexes) {
                statement.addBatch(INDEX_TILES);
                statement.addBatch(INDEX_METADATA);
            }
            statement.executeBatch();
        }
        connection.setAutoCommit(false);
    }

    public void createIndexes() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.addBatch(INDEX_TILES);
            statement.addBatch(INDEX_METADATA);
            statement.executeBatch();
        }
        connection.commit();
    }

    public void fillMetadata(float n, float s, float w, float e, String name, String format, int minZoom, int maxZoom)
        throws SQLException {
        // type = baselayer
        // version = 1.1
        // descritpion = name
        try (Statement statement = connection.createStatement()) {
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
        }

        connection.commit();

    }

    private String toMetadataQuery(String key, String value) {
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

    public void addTile(int x, int y, int z, BufferedImage image, String format) throws Exception {
        addedTiles++;

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

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setBytes(1, res);
            statement.execute();
        }

        if (addedTiles % 20 == 0) {
            connection.commit();
        }
    }

    /**
     * Get a Tile image from the database.
     * 
     * @param x
     * @param y
     * @param z
     * @return
     * @throws Exception
     */
    public BufferedImage getTile(int x, int y, int z) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(SELECTQUERY)) {
            statement.setInt(1, z);
            statement.setInt(2, x);
            statement.setInt(3, y);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                byte[] imageBytes = resultSet.getBytes(1);
                boolean orig = ImageIO.getUseCache();
                ImageIO.setUseCache(false);
                InputStream in = new ByteArrayInputStream(imageBytes);
                BufferedImage bufferedImage = ImageIO.read(in);
                ImageIO.setUseCache(orig);
                return bufferedImage;
            }
        }
        return null;
    }

    /**
     * Get the bounds as [w,s,e,n]
     * 
     * @return
     * @throws Exception
     */
    public double[] getBounds() throws Exception {
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(SELECT_BOUNDS);
            if (resultSet.next()) {
                String boundsWSEN = resultSet.getString(1);
                String[] split = boundsWSEN.split(",");
                double[] bounds = { Double.parseDouble(split[0]), Double.parseDouble(split[1]),
                    Double.parseDouble(split[2]), Double.parseDouble(split[3]) };

                return bounds;
            }
        }
        return null;
    }

    /**
     * @return the image format (jpg, png).
     * @throws Exception
     */
    public String getImageFormat() throws Exception {
        if (imageFormat == null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery(SELECT_IMAGEFORMAT);
                if (resultSet.next()) {
                    imageFormat = resultSet.getString(1);
                }
            }
        }
        return imageFormat;
    }

    /**
     * Read the image of a tile from a generic geotools coverage reader.
     * 
     * @param readerEPSG3857 the reader, expected to be in CRS 3857.
     * @param x the tile x.
     * @param y the tile y.
     * @param zoom the zoomlevel.
     * @return the image.
     * @throws IOException 
     */
    public static BufferedImage readGridcoverageImageForTile(AbstractGridCoverage2DReader readerEPSG3857, int x, int y,
        int zoom) throws IOException {
        double north = tile2lat(y, zoom);
        double south = tile2lat(y + 1, zoom);
        double west = tile2lon(x, zoom);
        double east = tile2lon(x + 1, zoom);
        BufferedImage image =
            ImageUtilities.imageFromReader(readerEPSG3857, TILESIZE, TILESIZE, west, east, south, north);
        return image;
    }

    public static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180.0;
    }

    public static double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    public static void main(String[] args) throws IOException {
        String ctp = "/home/hydrologis/data/CTP/trentino_ctp/ctp.shp";
        File file = new File(ctp);
        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        AbstractGridCoverage2DReader reader = format.getReader(file);
        BufferedImage image = MBTilesHelper.readGridcoverageImageForTile(reader, 1, 1, 19);

    }

}
