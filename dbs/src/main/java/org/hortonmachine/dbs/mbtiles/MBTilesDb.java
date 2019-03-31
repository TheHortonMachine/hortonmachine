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
package org.hortonmachine.dbs.mbtiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.utils.MercatorUtils;
import org.locationtech.jts.geom.Envelope;

/**
 * An mbtiles wrapper class to read and write mbtiles databases.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MBTilesDb {

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

    private String CREATE_TILES;

    // TABLE METADATA (name TEXT, value TEXT);
    public final static String TABLE_METADATA = "metadata";
    public final static String COL_METADATA_NAME = "name";
    public final static String COL_METADATA_VALUE = "value";

    private String CREATE_METADATA;

    private final static String SELECT_METADATA = //
            "select " + COL_METADATA_NAME + "," + COL_METADATA_VALUE + " from " + TABLE_METADATA;

    // INDEXES on Metadata and Tiles tables
    private final static String INDEX_TILES = "CREATE UNIQUE INDEX tile_index ON " + TABLE_TILES + " (" + COL_TILES_ZOOM_LEVEL
            + ", " + COL_TILES_TILE_COLUMN + ", " + COL_TILES_TILE_ROW + ")";
    private final static String INDEX_METADATA = "CREATE UNIQUE INDEX name ON " + TABLE_METADATA + "( " + COL_METADATA_NAME + ")";

    private String insertTileSql;

    private ADb database;

    private HashMap<String, String> metadataMap = null;

    private String tileRowType = "osm"; // could be tms in some cases

    /**
     * Constructor based on an existing ADb object.
     * 
     * @param database the {@link ADb} database.
     */
    public MBTilesDb( ADb database ) {
        this.database = database;
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
        sb.append(") values (?,?,?,?)");
        insertTileSql = sb.toString();

        ADatabaseSyntaxHelper dbs = database.getType().getDatabaseSyntaxHelper();
        String text = dbs.TEXT();
        CREATE_METADATA = //
                "CREATE TABLE " + TABLE_METADATA + "( " + //
                        COL_METADATA_NAME + " " + text + ", " + //
                        COL_METADATA_VALUE + " " + text + " " + //
                        ")";

        String intStr = dbs.INTEGER();
        String blobStr = dbs.BLOB();
        CREATE_TILES = //
                "CREATE TABLE " + TABLE_TILES + "( " + //
                        COL_TILES_ZOOM_LEVEL + " " + intStr + ", " + //
                        COL_TILES_TILE_COLUMN + " " + intStr + ", " + //
                        COL_TILES_TILE_ROW + " " + intStr + ", " + //
                        COL_TILES_TILE_DATA + " " + blobStr + //
                        ")";

    }

    /**
     * Set the row type. 
     * 
     * @param tileRowType can be "osm" (default) or "tms".
     */
    public void setTileRowType( String tileRowType ) {
        this.tileRowType = tileRowType;
    }

    /**
     * Create the mbtiles tables in the db.
     * 
     * <p><b>This removes existing tables!</b>
     * 
     * @param makeIndexes if true, indexes are made on creation.
     * @throws Exception
     */
    public void createTables( boolean makeIndexes ) throws Exception {
        database.executeInsertUpdateDeleteSql("DROP TABLE IF EXISTS " + TABLE_TILES);
        database.executeInsertUpdateDeleteSql("DROP TABLE IF EXISTS " + TABLE_METADATA);
        database.executeInsertUpdateDeleteSql(CREATE_TILES);
        database.executeInsertUpdateDeleteSql(CREATE_METADATA);
        if (makeIndexes) {
            createIndexes();
        }
    }

    /**
     * CReate the indexes.
     * 
     * @throws Exception
     */
    public void createIndexes() throws Exception {
        database.executeInsertUpdateDeleteSql(INDEX_TILES);
        database.executeInsertUpdateDeleteSql(INDEX_METADATA);
    }

    /**
     * Populate the metadata table.
     * 
     * @param n nord bound.
     * @param s south bound.
     * @param w west bound.
     * @param e east bound.
     * @param name name of the dataset.
     * @param format format of the images. png or jpg.
     * @param minZoom lowest zoomlevel.
     * @param maxZoom highest zoomlevel.
     * @throws Exception
     */
    public void fillMetadata( float n, float s, float w, float e, String name, String format, int minZoom, int maxZoom )
            throws Exception {
        // type = baselayer
        // version = 1.1
        // descritpion = name
        String query = toMetadataQuery("name", name);
        database.executeInsertUpdateDeleteSql(query);
        query = toMetadataQuery("description", name);
        database.executeInsertUpdateDeleteSql(query);
        query = toMetadataQuery("format", format);
        database.executeInsertUpdateDeleteSql(query);
        query = toMetadataQuery("minZoom", minZoom + "");
        database.executeInsertUpdateDeleteSql(query);
        query = toMetadataQuery("maxZoom", maxZoom + "");
        database.executeInsertUpdateDeleteSql(query);
        query = toMetadataQuery("type", "baselayer");
        database.executeInsertUpdateDeleteSql(query);
        query = toMetadataQuery("version", "1.1");
        database.executeInsertUpdateDeleteSql(query);
        // left, bottom, right, top
        query = toMetadataQuery("bounds", w + "," + s + "," + e + "," + n);
        database.executeInsertUpdateDeleteSql(query);
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

    /**
     * Add a single tile.
     * 
     * @param x the x tile index.
     * @param y the y tile index.
     * @param z the zoom level.
     * @return the tile image bytes.
     * @throws Exception
     */
    public synchronized void addTile( int x, int y, int z, byte[] imageBytes ) throws Exception {
        database.execOnConnection(connection -> {
            try (IHMPreparedStatement pstmt = connection.prepareStatement(insertTileSql);) {
                pstmt.setInt(1, z);
                pstmt.setInt(2, x);
                pstmt.setInt(3, y);
                pstmt.setBytes(4, imageBytes);
                pstmt.executeUpdate();
                return "";
            }
        });
    }

    /**
     * Add a list of tiles in batch mode.
     * 
     * @param tilesList the list of tiles.
     * @throws Exception
     */
    public synchronized void addTilesInBatch( List<Tile> tilesList ) throws Exception {
        database.execOnConnection(connection -> {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (IHMPreparedStatement pstmt = connection.prepareStatement(insertTileSql);) {
                for( Tile tile : tilesList ) {
                    pstmt.setInt(1, tile.z);
                    pstmt.setInt(2, tile.x);
                    pstmt.setInt(3, tile.y);
                    pstmt.setBytes(4, tile.imageBytes);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                return "";
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        });
    }

    /**
     * Get a Tile's image bytes from the database.
     * 
     * @param tx the x tile index.
     * @param tyOsm the y tile index, the osm way.
     * @param zoom the zoom level.
     * @return the tile image bytes.
     * @throws Exception
     */
    public byte[] getTile( int tx, int tyOsm, int zoom ) throws Exception {
        int ty = tyOsm;
        if (tileRowType.equals("tms")) {
            int[] tmsTileXY = MercatorUtils.osmTile2TmsTile(tx, tyOsm, zoom);
            ty = tmsTileXY[1];
        }
        int _ty = ty;
        return database.execOnConnection(connection -> {
            try (IHMPreparedStatement statement = connection.prepareStatement(SELECTQUERY)) {
                statement.setInt(1, zoom);
                statement.setInt(2, tx);
                statement.setInt(3, _ty);
                IHMResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    byte[] imageBytes = resultSet.getBytes(1);
                    return imageBytes;
                }
            }
            return null;
        });
    }

    /**
     * Get the db envelope.
     * 
     * @return the Envelope of the dataset.
     * @throws Exception
     */
    public Envelope getBounds() throws Exception {
        checkMetadata();
        String boundsWSEN = metadataMap.get("bounds");
        String[] split = boundsWSEN.split(",");
        double w = Double.parseDouble(split[0]);
        double s = Double.parseDouble(split[1]);
        double e = Double.parseDouble(split[2]);
        double n = Double.parseDouble(split[3]);
        return new Envelope(w, e, s, n);
    }

    /**
     * Get the bounds of a zoomlevel in tile indexes.
     * 
     * <p>This comes handy when one wants to navigate all tiles of a zoomlevel.
     * 
     * @param zoomlevel the zoom level.
     * @return the tile indexes as [minTx, maxTx, minTy, maxTy].
     * @throws Exception
     */
    public int[] getBoundsInTileIndex( int zoomlevel ) throws Exception {
        String sql = "select min(tile_column), max(tile_column), min(tile_row), max(tile_row) from tiles where zoom_level="
                + zoomlevel;
        return database.execOnConnection(connection -> {
            try (IHMStatement statement = connection.createStatement(); IHMResultSet resultSet = statement.executeQuery(sql);) {
                if (resultSet.next()) {
                    int minTx = resultSet.getInt(1);
                    int maxTx = resultSet.getInt(2);
                    int minTy = resultSet.getInt(3);
                    int maxTy = resultSet.getInt(4);
                    return new int[]{minTx, maxTx, minTy, maxTy};
                }
            }
            return null;
        });
    }

    public List<Integer> getAvailableZoomLevels() throws Exception {
        String sql = "select distinct zoom_level from tiles order by zoom_level";
        return database.execOnConnection(connection -> {
            List<Integer> zoomLevels = new ArrayList<>();
            try (IHMStatement statement = connection.createStatement(); IHMResultSet resultSet = statement.executeQuery(sql);) {
                while( resultSet.next() ) {
                    int z = resultSet.getInt(1);
                    zoomLevels.add(z);
                }
            }
            return zoomLevels;
        });
    }

    /**
     * Get the image format of the db.
     * 
     * @return the image format (jpg, png).
     * @throws Exception
     */
    public String getImageFormat() throws Exception {
        checkMetadata();
        return metadataMap.get("format");
    }

    public String getName() throws Exception {
        checkMetadata();
        return metadataMap.get("name");
    }
    public String getDescription() throws Exception {
        checkMetadata();
        return metadataMap.get("description");
    }

    public String getAttribution() throws Exception {
        checkMetadata();
        return metadataMap.get("attribution");
    }

    public String getVersion() throws Exception {
        checkMetadata();
        return metadataMap.get("version");
    }

    public int getMinZoom() throws Exception {
        checkMetadata();
        String minZoomStr = metadataMap.get("minzoom");
        if (minZoomStr != null) {
            return Integer.parseInt(minZoomStr);
        }
        return -1;
    }

    public int getMaxZoom() throws Exception {
        checkMetadata();
        String maxZoomStr = metadataMap.get("maxzoom");
        if (maxZoomStr != null) {
            return Integer.parseInt(maxZoomStr);
        }
        return -1;
    }

    private void checkMetadata() throws Exception {
        if (metadataMap == null) {
            metadataMap = new HashMap<>();
            database.execOnConnection(connection -> {
                try (IHMStatement statement = connection.createStatement()) {
                    IHMResultSet resultSet = statement.executeQuery(SELECT_METADATA);
                    while( resultSet.next() ) {
                        String name = resultSet.getString(1).toLowerCase();
                        String value = resultSet.getString(2);
                        metadataMap.put(name, value);
                    }
                }
                return null;
            });
        }
    }

    /**
     * A simple tile utility class.
     */
    public static class Tile {
        public int x;
        public int y;
        public int z;
        public byte[] imageBytes;
    }

//    /**
//     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
//     *
//     * @param latlong_bounds [minx,miny,maxx,minx]
//     * @param i_zoom
//     * @return [zoom, minx, miny, maxx, maxy of tile_bounds]
//     */
//    public static int[] LatLonBounds_to_TileBounds( double[] latlong_bounds, int i_zoom ) {
//        int[] min_tile_bounds = getTileNumber(latlong_bounds[1], latlong_bounds[0], i_zoom);
//        int[] max_tile_bounds = getTileNumber(latlong_bounds[3], latlong_bounds[2], i_zoom);
//        return new int[]{i_zoom, min_tile_bounds[1], min_tile_bounds[2], max_tile_bounds[1], max_tile_bounds[2]};
//    }
//
//    /**
//     * <p>Code adapted from: LatLonBounds_to_TileBounds</p>
//     *
//     * @param tile_bounds [minx, miny_osm, maxx, maxy_osm of tile_bounds]
//     * @param i_zoom
//     * @return latlong_bounds [minx,miny,maxx,minx]
//     */
//    public static double[] TileBounds_to_LatLonBounds( int[] tile_bounds, int i_zoom ) {
//        int i_min_x = tile_bounds[0];
//        int i_min_y_osm = tile_bounds[1];
//        int i_max_x = tile_bounds[2];
//        int i_max_y_osm = tile_bounds[3];
//        double[] bounds = tileLatLonBounds(i_min_x, i_min_y_osm, i_zoom, 256);
//        double d_min_x = bounds[0];
//        double d_min_y = bounds[1];
//        bounds = tileLatLonBounds(i_max_x, i_max_y_osm, i_zoom, 256);
//        double d_max_x = bounds[2];
//        double d_max_y = bounds[3];
//        return new double[]{d_min_x, d_min_y, d_max_x, d_max_y};
//    }

}
