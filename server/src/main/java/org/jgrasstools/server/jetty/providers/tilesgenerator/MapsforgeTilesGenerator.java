/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.server.jetty.providers.tilesgenerator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.nww.layers.defaults.raster.OsmTilegenerator;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore.DataPolicy;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

/**
 * Tiles generator for mapsforge files.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsforgeTilesGenerator implements ITilesGenerator {

    private static final int TILESIZE = 256;
    private OsmTilegenerator osmTilegenerator;
    private File cacheFolder;
    private String title;
    private boolean isVisible;
    private boolean isDefault;
    private String url;
    
    private int tileSize = TILESIZE;

    public MapsforgeTilesGenerator( String title, File[] mapsforgeFiles, Integer tileSize, boolean isVisible, boolean isDefault, Float scaleFactor )
            throws Exception {
        this.title = title;
        this.isVisible = isVisible;
        this.isDefault = isDefault;
        if (tileSize != null && tileSize >= TILESIZE) {
            this.tileSize = tileSize;
        }
        
        
        if (scaleFactor == null)
			scaleFactor = 1.5f;

		MapWorkerPool.NUMBER_OF_THREADS = 4;
		// Map buffer size
		ReadBuffer.setMaximumBufferSize(6500000);
		// Square frame buffer
		FrameBufferController.setUseSquareFrameBuffer(false);

		DisplayModel model = new DisplayModel();
		model.setUserScaleFactor(scaleFactor);
		model.setFixedTileSize(tileSize);

		DataPolicy dataPolicy = DataPolicy.RETURN_ALL;
		MultiMapDataStore mapDatabase = new MultiMapDataStore(dataPolicy);
		for (int i = 0; i < mapsforgeFiles.length; i++)
			mapDatabase.addMapDataStore(new MapFile(mapsforgeFiles[i]), false, false);

		InMemoryTileCache tileCache = new InMemoryTileCache(200);
		DatabaseRenderer renderer = new DatabaseRenderer(mapDatabase, AwtGraphicFactory.INSTANCE, tileCache,
				new TileBasedLabelStore(tileCache.getCapacityFirstLevel()), true, true);
		InternalRenderTheme xmlRenderTheme = InternalRenderTheme.DEFAULT;
		RenderThemeFuture theme = new RenderThemeFuture(AwtGraphicFactory.INSTANCE, xmlRenderTheme, model);
		// super important!! without the following line, all rendering
		// activities will block until the theme is created.
		new Thread(theme).start();
        
        osmTilegenerator = new OsmTilegenerator(mapDatabase, renderer, theme, model, tileSize);

        url = "gettile?z={z}&x={x}&y={y}&id=" + title;

    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void getTile( int xtile, int yTile, int zoom, OutputStream outputStream ) throws Exception {
        File tileImageFolderFile = new File(cacheFolder, zoom + File.separator + xtile);
        if (!tileImageFolderFile.exists()) {
            tileImageFolderFile.mkdirs();
        }
        File imgFile = new File(tileImageFolderFile, yTile + ".png");
        BufferedImage bImg;
        if (!imgFile.exists()) {
            bImg = osmTilegenerator.getImage(zoom, xtile, yTile);
        } else {
            bImg = ImageIO.read(imgFile);
        }
        ImageIO.write(bImg, "png", outputStream);
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

}
