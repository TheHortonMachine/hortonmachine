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
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

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

    public MapsforgeTilesGenerator( String title, File mapsforgeFile, Integer tileSize, boolean isVisible, boolean isDefault )
            throws Exception {
        this.title = title;
        this.isVisible = isVisible;
        this.isDefault = isDefault;
        if (tileSize == null || tileSize < 256) {
            tileSize = TILESIZE;
        }
        GraphicFactory graphicFactory = AwtGraphicFactory.INSTANCE;
        MapDatabase mapDatabase = new MapDatabase();
        DatabaseRenderer dbRenderer = null;
        XmlRenderTheme xmlRenderTheme = null;
        DisplayModel displayModel = null;
        if (mapsforgeFile.exists()) {
            String fileName = FileUtilities.getNameWithoutExtention(mapsforgeFile);
            if (title == null) {
                title = fileName;
            }
            cacheFolder = new File(mapsforgeFile.getParentFile(), fileName + "-tiles");
            cacheFolder.mkdir();

            // open map file
            mapDatabase.openFile(mapsforgeFile);
            dbRenderer = new DatabaseRenderer(mapDatabase, graphicFactory);

            String mapName = FileUtilities.getNameWithoutExtention(mapsforgeFile);
            File xmlStyleFile = new File(mapsforgeFile.getParentFile(), mapName + ".xml");
            if (xmlStyleFile.exists()) {
                try {
                    xmlRenderTheme = new ExternalRenderTheme(xmlStyleFile);
                } catch (Exception e) {
                    xmlRenderTheme = InternalRenderTheme.OSMARENDER;
                }
            } else {
                xmlRenderTheme = InternalRenderTheme.OSMARENDER;
            }
            displayModel = new DisplayModel();
            displayModel.setUserScaleFactor(tileSize / 256f);
        } else {
            throw new FileNotFoundException("Could not find: " + mapsforgeFile);
        }

        osmTilegenerator = new OsmTilegenerator(mapsforgeFile, dbRenderer, xmlRenderTheme, displayModel);

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
