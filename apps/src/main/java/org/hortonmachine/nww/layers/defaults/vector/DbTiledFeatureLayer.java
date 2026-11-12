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
package org.hortonmachine.nww.layers.defaults.vector;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.BaseSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.nww.layers.defaults.NwwFeatureLayer;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.BasicMercatorTiledImageLayer;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.nww.utils.cache.CacheUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;

/**
 * A vector layer drawn as image tiles from a table of a spatial database, which is never read
 * whole: each tile reads only the features in its extent, through the spatial index of the table.
 * This lets the viewer show tables with millions of features, as a national soil map.
 *
 * <p>A tile with more than {@link #getMaxTileFeatures() a maximum} of features, or, for the
 * geopackages, of {@link #DEFAULT_MAX_TILE_BYTES bytes of geometries}, is not drawn, it shows a
 * hint to zoom in: drawing it would take too long and too much memory. The features are counted
 * through the spatial index, stopping at the maximum, and the bytes are summed without reading
 * the geometries, so also the tiles of the whole extent are quick to tell.</p>
 *
 * <p>The layer keeps the database open, and closes it when disposed. The queries of the tiles,
 * which are drawn by more threads, are serialized on the database.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class DbTiledFeatureLayer extends BasicMercatorTiledImageLayer implements NwwLayer, NwwFeatureLayer {

    /** The features over which a tile is not drawn. */
    public static final int DEFAULT_MAX_TILE_FEATURES = 30_000;

    /**
     * The bytes of the geometries over which a tile is not drawn, for the geopackages, whose
     * features can be huge multipolygons: drawing about 30 MB takes a fraction of a second, a GB
     * half a minute.
     */
    public static final long DEFAULT_MAX_TILE_BYTES = 64L * 1024 * 1024;

    /** The features over which the queries of the tools refuse to read them all. */
    public static final int MAX_READ_FEATURES = 200_000;

    /** The bytes of the geometries over which the queries of the tools refuse to read them. */
    public static final long MAX_READ_BYTES = 256L * 1024 * 1024;

    private static final int TILESIZE = 512;
    /** The pixels added on each side of the read extent, for the strokes of the features near the tile. */
    private static final int READ_BUFFER_PIXELS = 8;
    /** The points added on each side of an extent, to reproject it to the table CRS. */
    private static final int ENVELOPE_DENSIFICATION_POINTS = 10;
    /** Makes the cache folders of the layers created in the same millisecond different. */
    private static final AtomicInteger LAYER_COUNTER = new AtomicInteger();

    private final String layerName;
    private final TableSource source;
    private final Coordinate centre;

    /**
     * @param title the name of the layer.
     * @param db the open database, owned by the layer from now on, which closes it when disposed.
     * @param table the spatial table.
     * @param where a SQL WHERE condition of the features drawn, <code>null</code> for all.
     * @param style the style the features are drawn with.
     * @throws Exception if the table can't be read.
     */
    public DbTiledFeatureLayer( String title, ASpatialDb db, SqlName table, String where, Style style ) throws Exception {
        this(title, new TableSource(db, table, where, style));
    }

    private DbTiledFeatureLayer( String title, TableSource source ) throws Exception {
        super(makeLevels(title, source));
        this.layerName = title;
        this.source = source;
        this.setUseTransparentTextures(true);
        centre = source.extentLL != null ? source.extentLL.centre() : new Coordinate(0, 0);
    }

    /**
     * Tell if a table has more features than a number, reading at most that number of rows, of
     * the spatial index if possible, which is quick also for huge tables.
     *
     * @param db the database.
     * @param table the table.
     * @param max the number of features.
     * @return <code>true</code> if the table has more than <code>max</code> features.
     * @throws Exception if the table can't be read.
     */
    public static boolean hasMoreFeaturesThan( ASpatialDb db, SqlName table, int max ) throws Exception {
        String from = table.fixedDoubleName;
        if (db instanceof GeopackageCommonDb) {
            GeopackageCommonDb gpkg = (GeopackageCommonDb) db;
            FeatureEntry feature = gpkg.feature(table);
            if (feature != null && gpkg.hasSpatialIndex(table)) {
                // one row per feature with geometry, much smaller than the rows of the table
                from = "\"" + gpkg.getSpatialIndexName(feature) + "\"";
            }
        }
        String sql = "SELECT count(*) FROM (SELECT 1 FROM " + from + " LIMIT " + (max + 1) + ") t";
        int count = db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
        return count > max;
    }

    /**
     * @return the database the layer reads from.
     */
    public ASpatialDb getDb() {
        return source.db;
    }

    /**
     * @return the table the layer reads from.
     */
    public SqlName getTable() {
        return source.table;
    }

    /**
     * @return the SQL WHERE condition of the features drawn, <code>null</code> for all.
     */
    public String getWhere() {
        return source.where;
    }

    /**
     * @return the style the features are drawn with.
     */
    public Style getStyle() {
        return source.style;
    }

    /**
     * @return the features over which a tile is not drawn.
     */
    public int getMaxTileFeatures() {
        return source.maxTileFeatures;
    }

    /**
     * @param maxTileFeatures the features over which a tile is not drawn and shows a hint to zoom
     *          in. It applies to the tiles not drawn yet.
     */
    public void setMaxTileFeatures( int maxTileFeatures ) {
        source.maxTileFeatures = maxTileFeatures;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The collection reads the whole table at each use, and refuses to read more than
     * {@link #MAX_READ_FEATURES} features: the tools reading features in an area use
     * {@link #queryFeatures(Geometry)}, which reads only those.</p>
     */
    @Override
    public SimpleFeatureCollection getFeatureCollection() {
        return new TableFeatureCollection(source);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only the features in the envelope of the area are read, through the spatial index.</p>
     *
     * @throws IllegalStateException if more than {@link #MAX_READ_FEATURES} features, or for the
     *          geopackages {@link #MAX_READ_BYTES} bytes of geometries, are in the envelope of the
     *          area.
     */
    @Override
    public List<SimpleFeature> queryFeatures( Geometry area ) {
        try {
            SimpleFeatureCollection inEnvelope = source.readLL(area.getEnvelopeInternal(), true);
            return NwwFeatureLayer.query(inEnvelope, area);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read the features of " + layerName + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        source.close();
    }

    /**
     * Close the database, as when the layer is removed from the viewer.
     */
    @Override
    public void freeResources() {
        source.close();
    }

    @Override
    public String toString() {
        return layerName;
    }

    @Override
    public Coordinate getCenter() {
        return centre;
    }

    @Override
    public Envelope getBounds() {
        return source.extentLL;
    }

    private static LevelSet makeLevels( String title, TableSource source ) throws MalformedURLException {
        AVList params = new AVListImpl();
        int tileSize = TILESIZE;

        /*
         * as for the rasterized feature collections, the tiles drawn for an earlier layer with the
         * same title, as with another filter or style, are deleted and each layer gets its own
         * folder, since the textures are cached in memory by its name
         */
        String titleRelativePath = "db_tiled_features/" + title.replaceAll("[^A-Za-z0-9._-]", "_") + "-tiles";
        File titleFolder = new File(CacheUtils.getCacheRoot(), titleRelativePath);
        if (titleFolder.exists()) {
            try {
                FileUtilities.deleteFileOrDir(titleFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String cacheRelativePath = titleRelativePath + "/" + System.currentTimeMillis() + "-" + LAYER_COUNTER.incrementAndGet();
        params.setValue(AVKey.TILE_WIDTH, tileSize);
        params.setValue(AVKey.TILE_HEIGHT, tileSize);
        params.setValue(AVKey.DATA_CACHE_NAME, cacheRelativePath);
        params.setValue(AVKey.SERVICE, "*");
        params.setValue(AVKey.DATASET_NAME, "*");
        params.setValue(AVKey.FORMAT_SUFFIX, ".png");
        params.setValue(AVKey.NUM_LEVELS, 22);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
        params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));

        final File cacheFolder = new File(CacheUtils.getCacheRoot(), cacheRelativePath);
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }
        params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder(){
            public URL getURL( Tile tile, String altImageFormat ) throws MalformedURLException {
                int zoom = tile.getLevelNumber() + 3;
                Sector sector = tile.getSector();
                double north = sector.getMaxLatitude().degrees;
                double south = sector.getMinLatitude().degrees;
                double east = sector.getMaxLongitude().degrees;
                double west = sector.getMinLongitude().degrees;
                double centerX = west + (east - west) / 2.0;
                double centerY = south + (north - south) / 2.0;
                int[] tileNumber = NwwUtilities.getTileNumber(centerY, centerX, zoom);
                int x = tileNumber[0];
                int y = tileNumber[1];

                File tileImageFolderFile = new File(cacheFolder, zoom + File.separator + x);
                File imgFile = new File(tileImageFolderFile, y + ".png");
                if (imgFile.exists()) {
                    return imgFile.toURI().toURL();
                }
                if (source.closed) {
                    // the layer has been disposed, as when replaced by a new style or filter,
                    // while its tiles were still requested
                    return null;
                }
                try {
                    BufferedImage image = source.drawTile(new Envelope(west, east, south, north), tileSize, title);
                    if (!tileImageFolderFile.exists()) {
                        tileImageFolderFile.mkdirs();
                    }
                    synchronized (source) {
                        if (!imgFile.exists()) {
                            ImageIO.write(image, "png", imgFile);
                        }
                    }
                    return imgFile.toURI().toURL();
                } catch (LayerClosedException e) {
                    // disposed while the tile was drawn
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });

        return new LevelSet(params);
    }

    /**
     * Thrown when the database of a disposed layer is used, as by the tiles still requested.
     */
    private static class LayerClosedException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        LayerClosedException() {
            super("The layer has been disposed.");
        }
    }

    /**
     * The table read by a layer, also by the threads drawing its tiles.
     */
    private static class TableSource {
        private final ASpatialDb db;
        private final SqlName table;
        private final String where;
        private final Style style;
        /** The srid of the table, -1 if unknown. */
        private final int srid;
        /** The CRS of the table, <code>null</code> if unknown. */
        private final CoordinateReferenceSystem crs;
        /** The extent of the table in WGS84 lon/lat, <code>null</code> if unknown. */
        private final ReferencedEnvelope extentLL;
        /** The type of the features in WGS84 lon/lat, <code>null</code> for an empty table. */
        private final SimpleFeatureType typeLL;
        /** The rtree of a geopackage table, to count the features quickly, else <code>null</code>. */
        private final String rtreeName;
        /** The primary key of a geopackage table with rtree, else <code>null</code>. */
        private final String pkName;
        /** The geometry column. */
        private final String geometryColumnName;
        private volatile int maxTileFeatures = DEFAULT_MAX_TILE_FEATURES;
        /** Set when the layer is disposed and the database closed. */
        private volatile boolean closed = false;

        TableSource( ASpatialDb db, SqlName table, String where, Style style ) throws Exception {
            this.db = db;
            this.table = table;
            this.where = where == null || where.isBlank() ? null : where.trim();
            this.style = style;

            GeometryColumn geometryColumn = db.getGeometryColumnsForTable(table);
            if (geometryColumn == null) {
                throw new IllegalArgumentException("The table has no geometry column: " + table.name);
            }
            srid = geometryColumn.srid;
            geometryColumnName = geometryColumn.geometryColumnName;
            crs = srid > 0 ? HMCrsRegistry.INSTANCE.getCrs("EPSG:" + srid, true) : null;

            String rtree = null;
            String pk = null;
            Envelope extent = null;
            if (db.getType() == EDb.GEOPACKAGE && db instanceof GeopackageCommonDb) {
                GeopackageCommonDb gpkg = (GeopackageCommonDb) db;
                FeatureEntry feature = gpkg.feature(table);
                if (feature != null) {
                    extent = feature.getBounds();
                    pk = gpkg.getPrimaryKey(table);
                    if (pk != null && gpkg.hasSpatialIndex(table)) {
                        rtree = gpkg.getSpatialIndexName(feature);
                    }
                }
            }
            rtreeName = rtree;
            pkName = rtree != null ? pk : null;
            extentLL = extent != null && !extent.isNull() ? toLL(extent) : null;

            // the type, from a single record
            SimpleFeatureType type = SpatialDbsImportUtils.tableToFeatureFCollection(db, table, 1, -1, null, null).getSchema();
            if (type != null) {
                SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                builder.init(type);
                builder.setCRS(NwwUtilities.GPS_CRS);
                typeLL = builder.buildFeatureType();
            } else {
                typeLL = null;
            }
        }

        /**
         * Close the database, once the query running, if any, is done: the tiles requested
         * later are not drawn.
         */
        void close() {
            synchronized (db) {
                if (closed) {
                    return;
                }
                closed = true;
                try {
                    db.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * To be called holding the lock on the database, before using it.
         *
         * @throws LayerClosedException if the database has been closed.
         */
        private void checkOpen() {
            if (closed) {
                throw new LayerClosedException();
            }
        }

        private boolean isLL() {
            return crs == null || HMCrsRegistry.crsEquals(crs, NwwUtilities.GPS_CRS);
        }

        private ReferencedEnvelope toLL( Envelope tableEnvelope ) throws Exception {
            Envelope env = tableEnvelope;
            if (!isLL()) {
                env = new HMCrsTransformer("EPSG:" + srid, "EPSG:4326", true, true).transformDensified(tableEnvelope,
                        ENVELOPE_DENSIFICATION_POINTS);
            }
            return new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84);
        }

        private Envelope toTable( Envelope envelopeLL ) throws Exception {
            if (isLL()) {
                return envelopeLL;
            }
            return new HMCrsTransformer("EPSG:4326", "EPSG:" + srid, true, true).transformDensified(envelopeLL,
                    ENVELOPE_DENSIFICATION_POINTS);
        }

        /**
         * Tell if the features in an extent are too many, or too big, to be read and drawn.
         *
         * @param tableEnvelope the extent, in the CRS of the table.
         * @param maxFeatures the features over which they are too many.
         * @param maxBytes the bytes of the geometries over which they are too big, considered
         *          only for the geopackages.
         * @return <code>true</code> if they are too many or too big.
         */
        private boolean isOverBudget( Envelope tableEnvelope, int maxFeatures, long maxBytes ) throws Exception {
            // all under the lock, also the reading of the metadata, as for the bbox condition
            synchronized (db) {
                checkOpen();
                return isOverBudgetLocked(tableEnvelope, maxFeatures, maxBytes);
            }
        }

        private boolean isOverBudgetLocked( Envelope tableEnvelope, int maxFeatures, long maxBytes ) throws Exception {
            String sql;
            String check = "(" + tableEnvelope.getMinX() + " <= maxx and " + tableEnvelope.getMaxX() + " >= minx and "
                    + tableEnvelope.getMinY() + " <= maxy and " + tableEnvelope.getMaxY() + " >= miny)";
            if (rtreeName != null) {
                if (where == null) {
                    // first on the index alone, which is quick also when the features are many
                    String countSql = "SELECT count(*) FROM (SELECT 1 FROM \"" + rtreeName + "\" WHERE " + check + " LIMIT "
                            + (maxFeatures + 1) + ")";
                    int count;
                    synchronized (db) {
                        checkOpen();
                        count = db.execOnConnection(connection -> {
                            try (IHMStatement stmt = connection.createStatement();
                                    IHMResultSet rs = stmt.executeQuery(countSql)) {
                                return rs.next() ? rs.getInt(1) : 0;
                            }
                        });
                    }
                    if (count > maxFeatures) {
                        return true;
                    } else if (count == 0) {
                        return false;
                    }
                }
                /*
                 * the candidates are taken from the index up to the maximum, and the length of
                 * the geometries is read without reading their content
                 */
                sql = "SELECT count(*), sum(length(\"" + geometryColumnName + "\")) FROM " + table.fixedDoubleName + " WHERE \""
                        + pkName + "\" IN (SELECT id FROM \"" + rtreeName + "\" WHERE " + check + " LIMIT " + (maxFeatures + 1)
                        + ")" + (where != null ? " AND (" + where + ")" : "");
            } else {
                List<String> conditions = new ArrayList<>();
                String bboxPiece = db.getSpatialindexBBoxWherePiece(table, null, tableEnvelope.getMinX(),
                        tableEnvelope.getMinY(), tableEnvelope.getMaxX(), tableEnvelope.getMaxY());
                if (bboxPiece != null) {
                    conditions.add(bboxPiece);
                }
                if (where != null) {
                    conditions.add("(" + where + ")");
                }
                sql = "SELECT count(*), coalesce(sum(s), 0) FROM (SELECT " + geometrySizeExpression() + " AS s FROM "
                        + table.fixedDoubleName + (conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions))
                        + " LIMIT " + (maxFeatures + 1) + ") t";
            }
            String _sql = sql;
            synchronized (db) {
                checkOpen();
                return db.execOnConnection(connection -> {
                    try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                        if (!rs.next()) {
                            return false;
                        }
                        return rs.getInt(1) > maxFeatures || rs.getLong(2) > maxBytes;
                    }
                });
            }
        }

        /**
         * @return the SQL expression of the bytes of the geometry of a record, read without
         *          reading the geometry, <code>0</code> for the databases that can't tell it.
         */
        private String geometrySizeExpression() {
            String column = "\"" + geometryColumnName + "\"";
            switch( db.getType() ) {
            case POSTGIS:
                // the stored size, without decompressing the geometry
                return "pg_column_size(" + column + ")";
            case GEOPACKAGE:
            case SPATIALITE:
                // the blobs, whose length sqlite reads without their content
                return "length(" + column + ")";
            default:
                return "0";
            }
        }

        /**
         * Read the features in an extent, in the CRS of the table.
         *
         * @param envelopeLL the extent in WGS84 lon/lat, <code>null</code> for all.
         * @param checked <code>true</code> to refuse to read more than {@link #MAX_READ_FEATURES}
         *          features or {@link #MAX_READ_BYTES} bytes of geometries.
         * @return the features, without type if there are none.
         * @throws IllegalStateException if the features are too many or too big.
         */
        private SimpleFeatureCollection read( Envelope envelopeLL, boolean checked ) throws Exception {
            Envelope tableEnvelope = envelopeLL != null ? toTable(envelopeLL) : null;
            synchronized (db) {
                checkOpen();
                int limit = -1;
                if (checked) {
                    if (tableEnvelope != null && isOverBudget(tableEnvelope, MAX_READ_FEATURES, MAX_READ_BYTES)) {
                        throw new IllegalStateException("The features of " + table.name
                                + " in the area are too many or too big to read at once: zoom in or filter the layer.");
                    }
                    // without extent, the features are only counted while read
                    limit = MAX_READ_FEATURES + 1;
                }
                SimpleFeatureCollection fc = SpatialDbsImportUtils.tableToFeatureFCollection(db, table, limit, -1, tableEnvelope,
                        where);
                if (checked && fc.size() > MAX_READ_FEATURES) {
                    throw new IllegalStateException("More than " + MAX_READ_FEATURES + " features of " + table.name
                            + ": too many to read at once, filter the layer.");
                }
                return fc;
            }
        }

        /**
         * Read the features in an extent, as {@link #read(Envelope, boolean)}, in WGS84 lon/lat.
         */
        private SimpleFeatureCollection readLL( Envelope envelopeLL, boolean checked ) throws Exception {
            SimpleFeatureCollection fc = read(envelopeLL, checked);
            if (fc.getSchema() != null && !isLL()) {
                fc = new HMCrsTransformer(crs, NwwUtilities.GPS_CRS).transform(fc);
            }
            return fc;
        }

        /**
         * Draw a tile.
         *
         * @param tileLL the extent of the tile, in WGS84 lon/lat.
         * @param tileSize the size of the tile, in pixels.
         * @param title the name of the layer, for the hint to zoom in.
         * @return the image of the tile.
         */
        BufferedImage drawTile( Envelope tileLL, int tileSize, String title ) throws Exception {
            BufferedImage image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
            if (extentLL != null && !extentLL.intersects(tileLL)) {
                // a transparent tile
                return image;
            }
            double buffer = tileLL.getWidth() * READ_BUFFER_PIXELS / tileSize;
            Envelope readLL = new Envelope(tileLL);
            readLL.expandBy(buffer, tileLL.getHeight() * READ_BUFFER_PIXELS / tileSize);
            // the queries don't go over the poles and the antimeridian
            readLL = readLL.intersection(new Envelope(-180, 180, -90, 90));

            if (isOverBudget(toTable(readLL), maxTileFeatures, DEFAULT_MAX_TILE_BYTES)) {
                drawZoomInHint(image, title);
                return image;
            }
            SimpleFeatureCollection fc = read(readLL, false);
            if (fc.getSchema() == null || fc.isEmpty()) {
                return image;
            }

            Graphics2D gr = image.createGraphics();
            MapContent mapContent = new MapContent();
            try {
                gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // in the CRS of the table: the renderer reprojects
                mapContent.addLayer(new FeatureLayer(fc, style));
                StreamingRenderer renderer = new StreamingRenderer();
                renderer.setMapContent(mapContent);
                renderer.paint(gr, new Rectangle(0, 0, tileSize, tileSize),
                        new ReferencedEnvelope(tileLL.getMinX(), tileLL.getMaxX(), tileLL.getMinY(), tileLL.getMaxY(),
                                DefaultGeographicCRS.WGS84));
            } finally {
                mapContent.dispose();
                gr.dispose();
            }
            return image;
        }

        private static void drawZoomInHint( BufferedImage image, String title ) {
            int size = image.getWidth();
            Graphics2D gr = image.createGraphics();
            try {
                gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                gr.setColor(new Color(128, 128, 128, 40));
                gr.fillRect(0, 0, size, size);
                gr.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                FontMetrics metrics = gr.getFontMetrics();
                String[] lines = {title, "too much data here, zoom in"};
                int y = size / 2 - metrics.getHeight() / 2;
                for( String line : lines ) {
                    int x = (size - metrics.stringWidth(line)) / 2;
                    gr.setColor(new Color(255, 255, 255, 200));
                    gr.drawString(line, x + 1, y + 1);
                    gr.setColor(new Color(60, 60, 60, 220));
                    gr.drawString(line, x, y);
                    y += metrics.getHeight();
                }
            } finally {
                gr.dispose();
            }
        }
    }

    /**
     * The features of the table, in WGS84 lon/lat, read at each use. The type and the bounds
     * are known without reading the table, the features are read whole only up to
     * {@link DbTiledFeatureLayer#MAX_READ_FEATURES}.
     */
    private static class TableFeatureCollection extends BaseSimpleFeatureCollection {
        private final TableSource source;

        TableFeatureCollection( TableSource source ) {
            super(source.typeLL);
            this.source = source;
        }

        @Override
        public SimpleFeatureIterator features() {
            SimpleFeatureCollection fc;
            try {
                fc = source.readLL(null, true);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (fc.getSchema() == null) {
                return new SimpleFeatureIterator(){
                    public boolean hasNext() {
                        return false;
                    }
                    public SimpleFeature next() throws NoSuchElementException {
                        throw new NoSuchElementException();
                    }
                    public void close() {
                    }
                };
            }
            return fc.features();
        }

        @Override
        public ReferencedEnvelope getBounds() {
            return source.extentLL != null ? source.extentLL : super.getBounds();
        }

        @Override
        public boolean isEmpty() {
            return schema == null;
        }
    }
}
