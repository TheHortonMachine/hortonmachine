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
package org.jgrasstools.nww.layers.defaults.raster;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.images.ImageUtilities;
import org.jgrasstools.nww.layers.defaults.NwwLayer;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.jgrasstools.nww.utils.cache.CacheUtils;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
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

import com.vividsolutions.jts.geom.Coordinate;

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
 * Procedural layer for mapsforge files
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsforgeNwwLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

	private String layerName = "unknown layer";

	private static final int TILESIZE = 1024;

	private Coordinate centerCoordinate;

	public MapsforgeNwwLayer(String layerName, File[] mapsforgeFiles, Integer tileSize) throws Exception {
		this(layerName, mapsforgeFiles, tileSize, null);
	}

	public MapsforgeNwwLayer(String layerName, File[] mapsforgeFiles, Integer tileSize, Color colorToMakeTransparent)
			throws Exception {
		super(makeLevels(layerName, getTilegenerator(mapsforgeFiles, tileSize), tileSize, colorToMakeTransparent));
		this.layerName = FileUtilities.getNameWithoutExtention(mapsforgeFiles[0]);
		this.setUseTransparentTextures(true);

		MultiMapDataStore mapDatabase = new MultiMapDataStore(DataPolicy.RETURN_ALL);
		for (int i = 0; i < mapsforgeFiles.length; i++)
			mapDatabase.addMapDataStore(new MapFile(mapsforgeFiles[i]), false, false);
		BoundingBox boundingBox = mapDatabase.boundingBox();
		LatLong centerPoint = boundingBox.getCenterPoint();
		centerCoordinate = new Coordinate(centerPoint.longitude, centerPoint.latitude);
		mapDatabase.close();
	}

	private static OsmTilegenerator getTilegenerator(File[] mapsforgeFiles, Integer tileSize) {
		if (tileSize == null || tileSize < 256) {
			tileSize = TILESIZE;
		}

		MapWorkerPool.NUMBER_OF_THREADS = 4;
		// Map buffer size
		ReadBuffer.setMaximumBufferSize(6500000);
		// Square frame buffer
		FrameBufferController.setUseSquareFrameBuffer(false);

		DisplayModel model = new DisplayModel();
		model.setUserScaleFactor(1.5f);
		model.setFixedTileSize(tileSize); // TODO check

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

		// DatabaseRenderer dbRenderer = null;
		// XmlRenderTheme xmlRenderTheme = null;
		// DisplayModel displayModel = null;
		// if (mapsforgeFile.exists()) {
		// mapDatabase.openFile(mapsforgeFile);
		// dbRenderer = new DatabaseRenderer(mapDatabase, graphicFactory);
		//
		// String mapName =
		// FileUtilities.getNameWithoutExtention(mapsforgeFiles[0]);
		// File xmlStyleFile = new File(mapsforgeFiles[0].getParentFile(),
		// mapName + ".xml");
		// if (xmlStyleFile.exists()) {
		// try {
		// xmlRenderTheme = new ExternalRenderTheme(xmlStyleFile);
		// } catch (Exception e) {
		// xmlRenderTheme = InternalRenderTheme.OSMARENDER;
		// }
		// } else {
		// xmlRenderTheme = InternalRenderTheme.OSMARENDER;
		// }
		// displayModel = new DisplayModel();
		// displayModel.setUserScaleFactor(tileSize / 256f);
		// }

		return new OsmTilegenerator(mapDatabase, renderer, theme, model, tileSize);
	}

	private static LevelSet makeLevels(String layerName, OsmTilegenerator osmTilegenerator, Integer tileSize,
			Color colorToMakeTransparent) throws MalformedURLException {
		AVList params = new AVListImpl();
		String cacheRelativePath = "mapsforge/" + layerName + "-tiles";

		if (tileSize == null || tileSize < 256) {
			tileSize = TILESIZE;
		}

		int finalTileSize = tileSize;

		params.setValue(AVKey.URL, cacheRelativePath);
		params.setValue(AVKey.TILE_WIDTH, finalTileSize);
		params.setValue(AVKey.TILE_HEIGHT, finalTileSize);
		params.setValue(AVKey.DATA_CACHE_NAME, cacheRelativePath);
		params.setValue(AVKey.SERVICE, "*");
		params.setValue(AVKey.DATASET_NAME, "*");
		params.setValue(AVKey.FORMAT_SUFFIX, ".png");
		params.setValue(AVKey.NUM_LEVELS, 22);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
		params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));
		File cacheRoot = CacheUtils.getCacheRoot();
		final File cacheFolder = new File(cacheRoot, cacheRelativePath);
		if (!cacheFolder.exists()) {
			cacheFolder.mkdirs();
		}

		params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder() {

			public URL getURL(Tile tile, String altImageFormat) throws MalformedURLException {
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
				if (!tileImageFolderFile.exists()) {
					tileImageFolderFile.mkdirs();
				}
				File imgFile = new File(tileImageFolderFile, y + ".png");
				try {
					if (!imgFile.exists()) {
						BufferedImage bImg = osmTilegenerator.getImage(zoom, x, y);
						// PrintUtilities.printRenderedImageData(bImg);

						if (colorToMakeTransparent != null) {
							bImg = ImageUtilities.makeColorTransparent(bImg, colorToMakeTransparent);
						}
						ImageIO.write(bImg, "png", imgFile);
					}
					return imgFile.toURI().toURL();
				} catch (IOException e) {
					return null;
				}
			}
		});

		return new LevelSet(params);
	}

	public String toString() {
		return layerName;
	}

	@Override
	public Coordinate getCenter() {
		return centerCoordinate;
	}

}
