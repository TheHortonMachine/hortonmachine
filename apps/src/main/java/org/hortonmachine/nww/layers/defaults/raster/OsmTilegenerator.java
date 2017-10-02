package org.hortonmachine.nww.layers.defaults.raster;

import java.awt.image.BufferedImage;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.graphics.AwtTileBitmap;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

public class OsmTilegenerator {

    private DatabaseRenderer renderer;
    private RenderThemeFuture theme;
    private DisplayModel model;
    private MapDataStore mapDatabase;
    private int tileSize;

    public OsmTilegenerator( MapDataStore mapDatabase, final DatabaseRenderer renderer, final RenderThemeFuture renderTheme,
            final DisplayModel displayModel, int tileSize ) {
        this.mapDatabase = mapDatabase;
        this.renderer = renderer;
        this.theme = renderTheme;
        this.model = displayModel;
        this.tileSize = tileSize;
    }

    public synchronized BufferedImage getImage( final int zoomLevel, final int xTile, final int yTile ) {
        try {
            Tile tile = new Tile(xTile, yTile, (byte) zoomLevel, tileSize);
            // displayModel.setFixedTileSize(tileSize);
            // Draw the tile
            float userScaleFactor = model.getUserScaleFactor();
            RendererJob mapGeneratorJob = new RendererJob(tile, mapDatabase, theme, model, userScaleFactor, false, false);
            AwtTileBitmap bmp = (AwtTileBitmap) renderer.executeJob(mapGeneratorJob);
            if (bmp != null) {
                BufferedImage bitmap = AwtGraphicFactory.getBitmap(bmp);
                return bitmap;
            }
        } catch (Exception e) {
            // e.printStackTrace();
            // will try again later
            System.err.println(
                    "Not rendering tile: " + zoomLevel + "/" + xTile + "/" + yTile + "  (" + e.getLocalizedMessage() + ")");
        }
        return null;
    }

}
