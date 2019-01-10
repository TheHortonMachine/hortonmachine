/*******************************************************************************
 * Copyright (C) 2019 HydroloGIS S.r.l. (www.hydrologis.com)
 * 
 * This program is free software: you can redistribute it and/or modify
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
 * 
 * Author: Antonello Andrea (http://www.hydrologis.com)
 ******************************************************************************/
package org.hortonmachine.gpextras.tiles;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.graphics.AwtTileBitmap;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore.DataPolicy;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.FixedTileSizeDisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

/**
 * A tile generator for mapsforge maps.
 * 
 * This can be used as follows:
 * <code>
 *         try (MapsforgeTilesGenerator m = new MapsforgeTilesGenerator(new File("/home/hydrologis/data/openandromaps/"), 1024, 1.5f,
 *               null, null)) {
 *           double lat = 46.0643;
 *           double lon = 11.1321;
 *           int zoom = 13;
 *           BufferedImage image = m.getTileForLatLon(lon, lat, zoom, BufferedImage.class);
 *           ImageIO.write(image, "png", new FileOutputStream(new File("/home/hydrologis/data/openandromaps/tile.png")));
 *       }
 * </code>
 */
public class MapsforgeTilesGenerator implements AutoCloseable {
    private MultiMapDataStore mapDataStore;
    private int tileSize = 256;
    private RenderThemeFuture renderTheme;
    private DisplayModel displayModel;
    private float scaleFactor = 1.0f;
    private DatabaseRenderer renderer;
    private TileCache tileCache;

    /**
     * Constructor.
     * 
     * @param mapsforgeFolder folder containing mapsforge maps.
     * @param tileSize optional tilesize, defaults to 256.
     * @param scaleFactor optional scale factor for labels, defaults to 1.
     * @param cacheDir optional cachdir. If null, in-memory is used.
     * @param theme optional rendertheme.
     * @throws IOException
     */
    public MapsforgeTilesGenerator( File mapsforgeFolder, Integer tileSize, Float scaleFactor, File cacheDir,
            XmlRenderTheme theme ) throws IOException {
        this(getMapFiles(mapsforgeFolder), tileSize, scaleFactor, cacheDir, theme);
    }

    /**
     * Constructor.
     * 
     * @param mapsforgeFiles mapsforge map files.
     * @param tileSize optional tilesize, defaults to 256.
     * @param scaleFactor optional scale factor for labels, defaults to 1.
     * @param cacheDir optional cachdir. If null, in-memory is used.
     * @param theme optional rendertheme.
     * @throws IOException
     */
    public MapsforgeTilesGenerator( File[] mapsforgeFiles, Integer tileSize, Float scaleFactor, File cacheDir,
            XmlRenderTheme theme ) throws IOException {
        if (tileSize != null)
            this.tileSize = tileSize;
        if (scaleFactor != null)
            this.scaleFactor = scaleFactor;
        if (theme == null) {
            theme = InternalRenderTheme.DEFAULT;
        }

        DataPolicy dataPolicy = DataPolicy.RETURN_ALL;
        mapDataStore = new MultiMapDataStore(dataPolicy);
        for( int i = 0; i < mapsforgeFiles.length; i++ )
            mapDataStore.addMapDataStore(new MapFile(mapsforgeFiles[i]), false, false);

        GraphicFactory graphicFactory = AwtGraphicFactory.INSTANCE;
        displayModel = new FixedTileSizeDisplayModel(this.tileSize);
        renderTheme = new RenderThemeFuture(graphicFactory, theme, displayModel);
        if (cacheDir == null) {
            tileCache = new InMemoryTileCache(200);
        } else {
            tileCache = new FileSystemTileCache(10, cacheDir, graphicFactory, false);
        }
        TileBasedLabelStore tileBasedLabelStore = new TileBasedLabelStore(tileCache.getCapacityFirstLevel());
        renderer = new DatabaseRenderer(mapDataStore, graphicFactory, tileCache, tileBasedLabelStore, true, true, null);
        new Thread(renderTheme).start();
    }

    private static File[] getMapFiles( File mapsforgeFolder ) {
        if (!mapsforgeFolder.isDirectory()) {
            // assume it is a single map file
            return new File[] {mapsforgeFolder};
        }
        File[] mapFiles = mapsforgeFolder.listFiles(new FilenameFilter(){
            @Override
            public boolean accept( File dir, String name ) {
                return name.endsWith(".map");
            }
        });
        return mapFiles;
    }

    /**
     * Get tile data for a given lat/lon/zoomlevel.
     *  
     * @param lon the WGS84 longitude.
     * @param lat the WGS84 latitude.
     * @param zoom the zoomlevel
     * @param adaptee the class to adapt to.
     * @return the generated data.
     * @throws IOException
     */
    public <T> T getTile4LatLon( double lon, double lat, int zoom, Class<T> adaptee ) throws IOException {
        final int ty = MercatorProjection.latitudeToTileY(lat, (byte) zoom);
        final int tx = MercatorProjection.longitudeToTileX(lon, (byte) zoom);
        return getTile4TileCoordinate(ty, tx, zoom, adaptee);
    }

    /**
     * Get tile data for a given tile schema coordinate.
     *  
     * @param tx the x tile index.
     * @param ty the y tile index.
     * @param zoom the zoomlevel
     * @param adaptee the class to adapt to.
     * @return the generated data.
     * @throws IOException
     */
    public <T> T getTile4TileCoordinate( final int tx, final int ty, int zoom, Class<T> adaptee ) throws IOException {
        //System.out.println("https://tile.openstreetmap.org/" + zoom + "/" + tx + "/" + ty + ".png");
        Tile tile = new Tile(tx, ty, (byte) zoom, tileSize);

        RendererJob mapGeneratorJob = new RendererJob(tile, mapDataStore, renderTheme, displayModel, scaleFactor, false, false);

        TileBitmap tb = renderer.executeJob(mapGeneratorJob);
        if (!(tileCache instanceof InMemoryTileCache)) {
            tileCache.put(mapGeneratorJob, tb);
        }
        if (tb instanceof AwtTileBitmap) {
            AwtTileBitmap bmp = (AwtTileBitmap) tb;
            if (bmp != null) {
                BufferedImage bitmap = AwtGraphicFactory.getBitmap(bmp);
                if (adaptee.isAssignableFrom(BufferedImage.class)) {
                    return adaptee.cast(bitmap);
                } else if (adaptee.isAssignableFrom(byte[].class)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bitmap, "png", baos);
                    baos.flush();
                    byte[] imageInByte = baos.toByteArray();
                    baos.close();
                    return adaptee.cast(imageInByte);
                }
            }
        }
        throw new RuntimeException("Can't handle tilebitmap of type -> " + tb.getClass());
    }

    public int[] tileCoordinate4LatLong( final double lat, final double lon, final int zoom ) {
        final int ty = MercatorProjection.latitudeToTileY(lat, (byte) zoom);
        final int tx = MercatorProjection.longitudeToTileX(lon, (byte) zoom);
        return new int[]{tx, ty};
    }

    /**
     * Convert a tile coordinate at a given zoom to its lat/lon bounds.
     * 
     * @param tx tile x.
     * @param ty tile y.
     * @param zoom the zoom level.
     * @return the bounds as [n,s,w,e].
     */
    public double[] tile2Bounds( final int tx, final int ty, final int zoom ) {
        double[] nswe = new double[4];
        nswe[0] = tile2lat(ty, zoom);
        nswe[1] = tile2lat(ty + 1, zoom);
        nswe[2] = tile2lon(tx, zoom);
        nswe[3] = tile2lon(tx + 1, zoom);
        return nswe;
    }

    /**
     * Get lat/lon bounds of map.
     * 
     * @return the bounds as [n,s,w,e].
     */
    public double[] getMapBounds() {
        BoundingBox bb = mapDataStore.boundingBox();
        double[] nswe = new double[4];
        nswe[0] = bb.maxLatitude;
        nswe[1] = bb.minLatitude;
        nswe[2] = bb.minLongitude;
        nswe[3] = bb.maxLongitude;
        return nswe;
    }

    private static double tile2lon( int x, int z ) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    private static double tile2lat( int y, int z ) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    @Override
    public void close() throws Exception {
        mapDataStore.close();
    }

    public static void main( String[] args ) throws Exception {
        try (org.hortonmachine.gpextras.tiles.MapsforgeTilesGenerator m = new org.hortonmachine.gpextras.tiles.MapsforgeTilesGenerator(
                new File("/home/hydrologis/data/openandromaps/"), 1024, 1.5f, null, null)) {
            double lat = 46.0643;
            double lon = 11.1321;
            int zoom = 13;
            BufferedImage image = m.getTile4LatLon(lon, lat, zoom, BufferedImage.class);
            ImageIO.write(image, "png", new FileOutputStream(new File("/home/hydrologis/data/openandromaps/tile.png")));
        }
    }
}
