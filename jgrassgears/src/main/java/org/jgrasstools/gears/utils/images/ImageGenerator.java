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
package org.jgrasstools.gears.utils.images;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.jgrasstools.gears.io.grasslegacy.map.color.ColorRule;
import org.jgrasstools.gears.io.grasslegacy.map.color.GrassColorTable;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.SldUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

/**
 * An utility class for simple image map generation. 
 *
 * <p>A sample usage could be the overlay of vector layers.
 * <pre>
 * ImageGenerator imgGen = new ImageGenerator();
 * imgGen.addFeaturePath(reticolo, null);
 * imgGen.setLayers();
 * imgGen.dumpPngImage(imagePath, bounds, 300, 300, 100);
 * imgGen.dispose();
 * </pre>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.3
 */
public class ImageGenerator {

    private List<String> featurePaths = new ArrayList<String>();
    private List<String> featureFilter = new ArrayList<String>();
    private List<String> coveragePaths = new ArrayList<String>();
    private AffineTransform worldToScreen;
    // private AffineTransform screenToWorld;

    private StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

    private MapContent content;
    private GTRenderer renderer;
    private CoordinateReferenceSystem crs;

    private IJGTProgressMonitor monitor = new DummyProgressMonitor();

    public ImageGenerator( IJGTProgressMonitor monitor ) {
        if (monitor != null)
            this.monitor = monitor;
    }

    /**
     * Add a new coverage file path.
     * 
     * <p>The order will be considered. First paths are drawn first.</p>
     * 
     * @param coveragePath the path to add.
     */
    public void addCoveragePath( String coveragePath ) {
        if (!coveragePaths.contains(coveragePath)) {
            coveragePaths.add(coveragePath);
        }
    }

    /**
     * Add a new feature file path.
     * 
     * <p>The order will be considered. First paths are drawn first.</p>
     * 
     * @param featurePath the path to add.
     */
    public void addFeaturePath( String featurePath, String filter ) {
        if (!featurePaths.contains(featurePath)) {
            featurePaths.add(featurePath);
            if (filter == null) {
                filter = "";
            }
            featureFilter.add(filter);
        }
    }

    /**
     * Set the layers that have to be drawn.
     * 
     * <p><b>This has to be called before the drawing process.</p>
     * @throws Exception 
     */
    public void setLayers() throws Exception {
        content = new MapContent();
        content.setTitle("dump");

        crs = null;

        // coverages first
        monitor.beginTask("Reading raster maps...", coveragePaths.size());
        for( String coveragePath : coveragePaths ) {
            File file = new File(coveragePath);
            GridCoverage2D raster = RasterReader.readRaster(coveragePath);
            if (crs == null)
                crs = raster.getCoordinateReferenceSystem();

            File styleFile = FileUtilities.substituteExtention(file, "sld");
            Style style;
            if (styleFile.exists()) {
                style = SldUtilities.getStyleFromFile(styleFile);
            } else {
                if (CoverageUtilities.isGrass(coveragePath)) {
                    style = getGrassStyle(coveragePath);
                } else {
                    RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
                    style = SLD.wrapSymbolizers(sym);
                }

            }

            GridCoverageLayer layer = new GridCoverageLayer(raster, style);
            content.addLayer(layer);

            monitor.worked(1);
        }
        monitor.done();

        monitor.beginTask("Reading vector maps...", featurePaths.size());
        for( int i = 0; i < featurePaths.size(); i++ ) {
            String featurePath = featurePaths.get(i);
            String filter = featureFilter.get(i);
            FileDataStore store = FileDataStoreFinder.getDataStore(new File(featurePath));
            SimpleFeatureSource featureSource = store.getFeatureSource();
            SimpleFeatureCollection featureCollection;
            if (filter.length() == 0) {
                featureCollection = featureSource.getFeatures();
            } else {
                featureCollection = featureSource.getFeatures(ECQL.toFilter(filter));
            }
            if (crs == null)
                crs = featureSource.getSchema().getCoordinateReferenceSystem();

            File styleFile = FileUtilities.substituteExtention(new File(featurePath), "sld");
            Style style;
            if (styleFile.exists()) {
                style = SldUtilities.getStyleFromFile(styleFile);
            } else {
                style = SLD.createSimpleStyle(featureSource.getSchema());
            }

            FeatureLayer layer = new FeatureLayer(featureCollection, style);
            content.addLayer(layer);

            monitor.worked(1);
        }
        monitor.done();

        renderer = new StreamingRenderer();
        renderer.setMapContent(content);
    }

    private Style getGrassStyle( String path ) throws Exception {
        List<String> valuesList = new ArrayList<String>();
        List<Color> colorsList = new ArrayList<Color>();

        StyleBuilder sB = new StyleBuilder(sf);
        RasterSymbolizer rasterSym = sf.createRasterSymbolizer();

        File grassFile = new File(path);
        String mapName = grassFile.getName();
        String mapsetPath = grassFile.getParentFile().getParent();

        GrassColorTable ctable = new GrassColorTable(mapsetPath, mapName, null);
        Enumeration<ColorRule> rules = ctable.getColorRules();

        while( rules.hasMoreElements() ) {
            ColorRule element = (ColorRule) rules.nextElement();

            float fromValue = element.getLowCategoryValue();
            float toValue = element.getLowCategoryValue() + element.getCategoryRange();
            byte[] lowcatcol = element.getColor(fromValue);
            byte[] highcatcol = element.getColor(toValue);
            Color fromColor = new Color((int) (lowcatcol[0] & 0xff), (int) (lowcatcol[1] & 0xff), (int) (lowcatcol[2] & 0xff));
            Color toColor = new Color((int) (highcatcol[0] & 0xff), (int) (highcatcol[1] & 0xff), (int) (highcatcol[2] & 0xff));

            String from = String.valueOf(fromValue);
            if (!valuesList.contains(from)) {
                valuesList.add(from);
                colorsList.add(fromColor);
            }

            String to = String.valueOf(toValue);
            if (!valuesList.contains(to)) {
                valuesList.add(to);
                colorsList.add(toColor);
            }
        }

        ColorMap colorMap = sf.createColorMap();

        if (valuesList.size() > 1) {
            for( int i = 0; i < valuesList.size(); i++ ) {
                String fromValueStr = valuesList.get(i);
                // String toValueStr = valuesList.get(i + 1);
                Color fromColor = colorsList.get(i);
                // Color toColor = colorsList.get(i + 1);
                // double[] values = {Double.parseDouble(fromValueStr),
                // Double.parseDouble(toValueStr)};
                // double opacity = 1.0;

                Expression fromColorExpr = sB.colorExpression(new java.awt.Color(fromColor.getRed(), fromColor.getGreen(),
                        fromColor.getBlue(), 255));
                // Expression toColorExpr = sB.colorExpression(new java.awt.Color(toColor.getRed(),
                // toColor.getGreen(), toColor
                // .getBlue(), 255));
                Expression fromExpr = sB.literalExpression(Double.parseDouble(fromValueStr));
                // Expression toExpr = sB.literalExpression(values[1]);
                // Expression opacityExpr = sB.literalExpression(opacity);

                ColorMapEntry entry = sf.createColorMapEntry();
                entry.setQuantity(fromExpr);
                entry.setColor(fromColorExpr);
                // entry.setOpacity(opacityExpr);
                colorMap.addColorMapEntry(entry);

                // entry = sf.createColorMapEntry();
                // entry.setQuantity(toExpr);
                // entry.setOpacity(opacityExpr);
                // entry.setColor(toColorExpr);
                // colorMap.addColorMapEntry(entry);
            }
        } else if (valuesList.size() == 1) {
            String fromValueStr = valuesList.get(0);
            Color fromColor = colorsList.get(0);
            // double opacity = 1.0;

            Expression fromColorExpr = sB.colorExpression(new java.awt.Color(fromColor.getRed(), fromColor.getGreen(), fromColor
                    .getBlue(), 255));
            Expression fromExpr = sB.literalExpression(Double.parseDouble(fromValueStr));
            // Expression opacityExpr = sB.literalExpression(opacity);

            ColorMapEntry entry = sf.createColorMapEntry();
            entry.setQuantity(fromExpr);
            entry.setColor(fromColorExpr);
            // entry.setOpacity(opacityExpr);
            colorMap.addColorMapEntry(entry);
            colorMap.addColorMapEntry(entry);
        } else {
            throw new IllegalArgumentException();
        }

        rasterSym.setColorMap(colorMap);

        /*
         * set global transparency for the map
         */
        int alpha = ctable.getAlpha();
        rasterSym.setOpacity(sB.literalExpression(alpha / 255.0));

        Style newStyle = SLD.wrapSymbolizers(rasterSym);

        return newStyle;
    }

    /**
     * Draw the map on an image.
     * 
     * @param bounds the area of interest.
     * @param imageWidth the width of the image to produce.
     * @param imageHeight the height of the image to produce.
     * @param buffer the buffer to add around the map bounds in map units. 
     * @return the image.
     */
    public BufferedImage drawImage( Envelope bounds, int imageWidth, int imageHeight, double buffer ) {
        if (renderer == null) {
            throw new IllegalArgumentException("MapContent is not available. Did you call setLayers first?");
        }

        // now we have point and polygon
        // create bounds
        bounds.expandBy(buffer, buffer);
        ReferencedEnvelope ref = new ReferencedEnvelope(bounds, crs);

        content.getViewport().setBounds(ref);
        setTransforms(ref, imageWidth, imageHeight);

        BufferedImage dumpImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dumpImage.createGraphics();
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle imgRec = new Rectangle(imageWidth, imageHeight);
        renderer.paint(g2d, imgRec, ref, worldToScreen);

        return dumpImage;
    }

    /**
     * Writes an image of maps drawn to file. 
     * 
     * @param imagePath the path to which to write the image.
     * @param bounds the area of interest.
     * @param imageWidth the width of the image to produce.
     * @param imageHeight the height of the image to produce.
     * @param buffer the buffer to add around the map bounds in map units. 
     * @throws IOException
     */
    public void dumpPngImage( String imagePath, Envelope bounds, int imageWidth, int imageHeight, double buffer )
            throws IOException {
        BufferedImage dumpImage = drawImage(bounds, imageWidth, imageHeight, buffer);
        ImageIO.write(dumpImage, "png", new File(imagePath));
    }

    public void dispose() {
        content.dispose();
    }

    private void setTransforms( final ReferencedEnvelope envelope, int width, int height ) {

        double envWidth = envelope.getWidth();
        double xscale = width / envWidth;
        double envHeight = envelope.getHeight();
        double yscale = height / envHeight;

        double median0 = envelope.getMedian(0);
        double xoff = median0 * xscale - width / 2.0;
        double median1 = envelope.getMedian(1);
        double yoff = median1 * yscale + height / 2.0;

        worldToScreen = new AffineTransform(xscale, 0, 0, -yscale, -xoff, yoff);
    }

}
