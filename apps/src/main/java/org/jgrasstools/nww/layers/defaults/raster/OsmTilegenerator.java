package org.jgrasstools.nww.layers.defaults.raster;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.AwtTileBitmap;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

public class OsmTilegenerator {

    private static Field tileBitmapImageField;
    static {
        try {
            tileBitmapImageField = AwtTileBitmap.class.getSuperclass().getDeclaredField("bufferedImage");
            tileBitmapImageField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File mapFile;
    private DatabaseRenderer renderer;
    private XmlRenderTheme xmlRenderTheme;
    private DisplayModel displayModel;

    public OsmTilegenerator( final File mapFile, final DatabaseRenderer renderer, final XmlRenderTheme xmlRenderTheme,
            final DisplayModel displayModel ) {
        this.mapFile = mapFile;
        this.renderer = renderer;
        this.xmlRenderTheme = xmlRenderTheme;
        this.displayModel = displayModel;
    }

    public synchronized BufferedImage getImage( final int zoomLevel, final int xTile, final int yTile ) {
        try {
            Tile tile = new Tile(xTile, yTile, (byte) zoomLevel);
            RendererJob renderJob = new RendererJob(tile, mapFile, xmlRenderTheme, displayModel, 1.5f// displayModel.getScaleFactor()
                    , false);
            AwtTileBitmap tileBitmap = (AwtTileBitmap) renderer.executeJob(renderJob);
            BufferedImage tileImage = (BufferedImage) tileBitmapImageField.get(tileBitmap);
            return tileImage;
        } catch (Exception e) {
            // e.printStackTrace();
            // will try again later
            System.err.println(
                    "Not rendering tile: " + zoomLevel + "/" + xTile + "/" + yTile + "  (" + e.getLocalizedMessage() + ")");
        }
        return null;
    }

}
