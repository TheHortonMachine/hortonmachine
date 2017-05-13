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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.SldUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;

/**
 * Tiles generator for imagemosaic files.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImageMosaicTilesGenerator implements ITilesGenerator {

    private static final int TILESIZE = 256;
    private File cacheFolder;
    private String title;
    private String url;

    private int tileSize = TILESIZE;
    private GTRenderer renderer;

    public ImageMosaicTilesGenerator( String title, File imsf, Integer tileSize ) throws Exception {
        this.title = title;
        if (tileSize != null && tileSize >= TILESIZE) {
            this.tileSize = tileSize;
        }
        AbstractGridFormat format = GridFormatFinder.findFormat(imsf);
        AbstractGridCoverage2DReader coverageTilesReader = format.getReader(imsf);

        MapContent mapContent = new MapContent();
        try {
            RasterSymbolizer sym = SldUtilities.sf.getDefaultRasterSymbolizer();
            Style style = SLD.wrapSymbolizers(sym);
            GridReaderLayer layer = new GridReaderLayer(coverageTilesReader, style);
            mapContent.addLayer(layer);
            mapContent.getViewport().setCoordinateReferenceSystem(CrsUtilities.WGS84);
        } catch (Exception e) {
            e.printStackTrace();
        }
        renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);

        String fileName = FileUtilities.getNameWithoutExtention(imsf);
        if (title == null) {
            title = fileName;
        }
        File folder = imsf.getParentFile();
        cacheFolder = new File(folder.getParentFile(), folder.getName() + "-tiles");
        cacheFolder.mkdir();

        url = "gettile?z={z}&x={x}&y={y}&id=" + title;

    }

    @Override
    public String getName() {
        return title;
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

            Rectangle imageBounds = new Rectangle(0, 0, tileSize, tileSize);
            BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D gr = image.createGraphics();
            gr.setPaint(Color.WHITE);
            gr.fill(imageBounds);
            gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double north = tile2lat(yTile, zoom);
            double south = tile2lat(yTile + 1, zoom);
            double west = tile2lon(xtile, zoom);
            double east = tile2lon(xtile + 1, zoom);

            synchronized (renderer) {
                ReferencedEnvelope mapArea = new ReferencedEnvelope(west, east, south, north, CrsUtilities.WGS84);
                renderer.paint(gr, imageBounds, mapArea);
            }
            ImageIO.write(image, "png", imgFile);
            bImg = image;
        } else {
            bImg = ImageIO.read(imgFile);
        }
        ImageIO.write(bImg, "png", outputStream);
    }

    @Override
    public String getUrl() {
        return url;
    }

    private static double tile2lon( int x, int z ) {
        return x / Math.pow(2.0, z) * 360.0 - 180.0;
    }

    private static double tile2lat( int y, int z ) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

}
