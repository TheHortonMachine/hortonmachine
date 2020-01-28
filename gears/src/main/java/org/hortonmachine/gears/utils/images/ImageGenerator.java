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
package org.hortonmachine.gears.utils.images;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.grassraster.GrassCoverageReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.map.WMSLayer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * An utility class for simple image map generation. 
 *
 * <p>A sample usage could be the overlay of vector layers.
 * <pre>
 * ImageGenerator imgGen = new ImageGenerator();
 * imgGen.addFeaturePath(reticolo, null);
 * imgGen.setLayers();
 * imgGen.dumpPngImage(imagePath, bounds, 300, 300, 100);
 * </pre>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.3
 */
public class ImageGenerator {


    private String wmsURL = null;
    private List<String> featurePaths = new ArrayList<String>();
    private List<String> featureFilter = new ArrayList<String>();
    private List<String> coveragePaths = new ArrayList<String>();
    private List<GridGeometry2D> coverageRegions = new ArrayList<GridGeometry2D>();
    // private AffineTransform worldToScreen;
    // private AffineTransform screenToWorld;

    private StyleFactory sf;

    private IHMProgressMonitor monitor = new DummyProgressMonitor();

    private List<Layer> layers = new ArrayList<Layer>();
    private List<Layer> synchronizedLayers = null;
    private MapContent content;

    private StreamingRenderer renderer;

    private File shapesFile;

    private CoordinateReferenceSystem forceCrs;

    public ImageGenerator( IHMProgressMonitor monitor, CoordinateReferenceSystem forceCrs ) {
        this.forceCrs = forceCrs;
        if (monitor != null)
            this.monitor = monitor;
        sf = CommonFactoryFinder.getStyleFactory(null);
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
     * Add a coverage read region (this has to have same index as addCoveragePath.
     * 
     * @param coverageRegion the region to read.
     */
    public void addCoverageRegion( GridGeometry2D coverageRegion ) {
        if (!coverageRegions.contains(coverageRegion)) {
            coverageRegions.add(coverageRegion);
        }
    }

    /**
     * Set a WMS service to handle as raster layer.
     * 
     * @param wmsURL the WMS url and layer name in the format: http://wmsurl#layername
     */
    public void setWMS( String wmsURL ) {
        this.wmsURL = wmsURL;
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
     * Method to add a file that contains a list of shapes/texts to add.
     * 
     * <p><b>This is applied on top of the final image. Positions are in pixel.</b> 
     * </p>
     * </p>
     * <p>
     * Supported are:</br>
     * <ul>
     *  <li>text;x;y;mytext;colorrgba;size</li>
     * <li>box;x;y;w;h;strokewidth;fillrgba;strokergba</li>
     * <li>roundedbox;x;y;w;h;round;strokewidth;fillrgba;strokergba</li>
     *  <li>...</li>
     * </ul>
     * </p>
     * 
     */
    public void addShapesPath( String shapesPath ) {
        this.shapesFile = new File(shapesPath);
    }

    // private void setTransforms( final ReferencedEnvelope envelope, int width, int height ) {
    //
    // double envWidth = envelope.getWidth();
    // double envHeight = envelope.getHeight();
    // double envValue = envWidth;
    // if (envHeight > envWidth) {
    // envValue = envHeight;
    // }
    //
    // double xscale = width / envValue;
    // double yscale = height / envValue;
    //
    // double median0 = envelope.getMedian(0);
    // double xoff = median0 * xscale - width / 2.0;
    // double median1 = envelope.getMedian(1);
    // double yoff = median1 * yscale + height / 2.0;
    //
    // worldToScreen = new AffineTransform(xscale, 0, 0, -yscale, -xoff, yoff);
    // }

    /**
     * Set the layers that have to be drawn.
     * 
     * <p><b>This has to be called before the drawing process.</p>
     * @return the max envelope of the data.
     * @throws Exception 
     */
    public ReferencedEnvelope setLayers() throws Exception {
        ReferencedEnvelope maxExtent = null;

        // wms first
        if (wmsURL != null) {
            String[] split = wmsURL.split("#");
            WebMapServer server = new WebMapServer(new URL(split[0]));
            org.geotools.ows.wms.Layer wmsLayer = getWMSLayer(server, split[1]);
            WMSLayer layer = new WMSLayer(server, wmsLayer);
            layers.add(layer);

            ReferencedEnvelope originalEnvelope = layer.getBounds();
            if (originalEnvelope != null) {
                if (maxExtent == null) {
                    maxExtent = new ReferencedEnvelope(originalEnvelope.getCoordinateReferenceSystem());
                }
                expandToIncludeEnvelope(maxExtent, originalEnvelope);
            }
        }

        // coverages
        monitor.beginTask("Reading raster maps...", coveragePaths.size());
        for( int r = 0; r < coveragePaths.size(); r++ ) {
            String coveragePath = coveragePaths.get(r);
            GridGeometry2D region = null;
            if (coverageRegions != null && coverageRegions.size() == coveragePaths.size()) {
                region = coverageRegions.get(r);
            }

            File file = new File(coveragePath);
            GridCoverage2D raster = null;
            AbstractGridCoverage2DReader reader = null;
            try {

                try {
                    // first try a format that gives back a reader
                    AbstractGridFormat format = GridFormatFinder.findFormat(file);
                    reader = format.getReader(file);
                    if (reader instanceof GrassCoverageReader) {
                        reader = null;
                    }
                } catch (Exception e1) {
                    // ignore and try others
                }
                if (reader == null) {

                    if (region == null) {
                        raster = OmsRasterReader.readRaster(coveragePath);
                    } else {
                        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(region);
                        double n = regionMap.getNorth();
                        double s = regionMap.getSouth();
                        double w = regionMap.getWest();
                        double e = regionMap.getEast();
                        double xres = regionMap.getXres();
                        double yres = regionMap.getYres();
                        OmsRasterReader rreader = new OmsRasterReader();
                        rreader.file = coveragePath;
                        rreader.pNorth = n;
                        rreader.pSouth = s;
                        rreader.pWest = w;
                        rreader.pEast = e;
                        rreader.pXres = xres;
                        rreader.pYres = yres;
                        rreader.process();
                        raster = rreader.outRaster;
                    }

                }
                // if (crs == null) {
                // crs = raster.getCoordinateReferenceSystem();
                // }
            } catch (Exception e) {
                monitor.errorMessage(e.getLocalizedMessage());
                monitor.errorMessage("Trying to find other coverage source...");
                // try with available readers
                try {
                    AbstractGridFormat format = GridFormatFinder.findFormat(file);
                    reader = format.getReader(file);
                    // if (crs == null) {
                    // crs = reader.getCrs();
                    // }
                } catch (Exception ex) {
                    throw ex;
                }
            }
            File styleFile = FileUtilities.substituteExtention(file, "sld");
            Style style;
            if (styleFile.exists()) {
                style = SldUtilities.getStyleFromFile(styleFile);
            } else {
                style = SldUtilities.getStyleFromRasterFile(styleFile);
            }

            if (raster != null) {
                GridCoverageLayer layer = new GridCoverageLayer(raster, style);
                layers.add(layer);

                org.opengis.geometry.Envelope envelope = raster.getEnvelope();
                if (maxExtent == null) {
                    maxExtent = new ReferencedEnvelope(envelope.getCoordinateReferenceSystem());
                }
                expandToIncludeEnvelope(maxExtent, envelope);
            }
            if (reader != null) {
                GridReaderLayer layer = new GridReaderLayer(reader, style);
                // SimpleFeatureSource featureSource = layer.getFeatureSource();
                layers.add(layer);

                org.opengis.geometry.Envelope envelope = reader.getOriginalEnvelope();
                if (maxExtent == null) {
                    maxExtent = new ReferencedEnvelope(envelope.getCoordinateReferenceSystem());
                }
                expandToIncludeEnvelope(maxExtent, envelope);
            }
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
            // if (crs == null) {
            // crs = featureSource.getSchema().getCoordinateReferenceSystem();
            // }

            File styleFile = FileUtilities.substituteExtention(new File(featurePath), "sld");
            Style style;
            if (styleFile.exists()) {
                style = SldUtilities.getStyleFromFile(styleFile);
            } else {
                style = SLD.createSimpleStyle(featureSource.getSchema());
            }

            FeatureLayer layer = new FeatureLayer(featureCollection, style);
            layers.add(layer);

            if (maxExtent == null) {
                maxExtent = new ReferencedEnvelope(featureCollection.getSchema().getCoordinateReferenceSystem());
            }
            expandToIncludeEnvelope(maxExtent, featureCollection.getBounds());

            monitor.worked(1);
        }

        synchronizedLayers = Collections.synchronizedList(layers);

        monitor.done();
        return maxExtent;
    }

    private void expandToIncludeEnvelope( ReferencedEnvelope maxExtent, org.opengis.geometry.Envelope envelope ) {
        ReferencedEnvelope tmpExtent = new ReferencedEnvelope(envelope.getCoordinateReferenceSystem());
        DirectPosition ll = envelope.getLowerCorner();
        double[] coordinate = ll.getCoordinate();
        tmpExtent.expandToInclude(new Coordinate(coordinate[0], coordinate[1]));
        DirectPosition ur = envelope.getUpperCorner();
        coordinate = ur.getCoordinate();
        tmpExtent.expandToInclude(new Coordinate(coordinate[0], coordinate[1]));

        try {
            ReferencedEnvelope transformed = tmpExtent.transform(maxExtent.getCoordinateReferenceSystem(), true);
            maxExtent.expandToInclude(transformed);
        } catch (TransformException | FactoryException e) {
            e.printStackTrace();
        }
    }

    private org.geotools.ows.wms.Layer getWMSLayer( WebMapServer server, String layerName ) {
        for( org.geotools.ows.wms.Layer layer : server.getCapabilities().getLayerList() ) {
            if (layerName.equals(layer.getName())) {
                return layer;
            }
        }
        throw new IllegalArgumentException("Could not find layer " + layerName);
    }

    private synchronized void checkMapContent() {
        if (content == null) {
            content = new MapContent();
            content.setTitle("dump");

            for( Layer layer : layers ) {
                content.addLayer(layer);
            }

            renderer = new StreamingRenderer();
            renderer.setMapContent(content);
        }
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
    public BufferedImage drawImage( ReferencedEnvelope ref, int imageWidth, int imageHeight, double buffer ) {
        checkMapContent();

        if (buffer > 0.0)
            ref.expandBy(buffer, buffer);

        Rectangle2D refRect = new Rectangle2D.Double(ref.getMinX(), ref.getMinY(), ref.getWidth(), ref.getHeight());
        Rectangle2D imageRect = new Rectangle2D.Double(0, 0, imageWidth, imageHeight);

        GeometryUtilities.scaleToRatio(imageRect, refRect, false);

        ReferencedEnvelope newRef = new ReferencedEnvelope(refRect, ref.getCoordinateReferenceSystem());

        Rectangle imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
        BufferedImage dumpImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = dumpImage.createGraphics();
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        synchronized (renderer) {
            renderer.paint(g2d, imageBounds, newRef);
        }

        return dumpImage;
    }

    public void drawImage( Graphics2D g2d, ReferencedEnvelope ref, int imageWidth, int imageHeight, double buffer ) {
        checkMapContent();

        if (buffer > 0.0)
            ref.expandBy(buffer, buffer);

        Rectangle2D refRect = new Rectangle2D.Double(ref.getMinX(), ref.getMinY(), ref.getWidth(), ref.getHeight());
        Rectangle2D imageRect = new Rectangle2D.Double(0, 0, imageWidth, imageHeight);

        GeometryUtilities.scaleToRatio(imageRect, refRect, false);

        ReferencedEnvelope newRef = new ReferencedEnvelope(refRect, ref.getCoordinateReferenceSystem());

        Rectangle imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
        Color white = Color.white;
        g2d.setColor(new Color(white.getRed(), white.getGreen(), white.getBlue(), 0));
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        synchronized (renderer) {
            content.getViewport().setBounds(newRef);
            renderer.paint(g2d, imageBounds, newRef);
        }
    }

    /**
     * Draw the map on an image creating a new MapContent.
     * 
     * @param bounds the area of interest.
     * @param imageWidth the width of the image to produce.
     * @param imageHeight the height of the image to produce.
     * @param buffer the buffer to add around the map bounds in map units. 
     * @return the image.
     */
    public BufferedImage drawImageWithNewMapContent( ReferencedEnvelope ref, int imageWidth, int imageHeight, double buffer ) {
        MapContent content = new MapContent();
        content.setTitle("dump");

        if (forceCrs != null) {
            content.getViewport().setCoordinateReferenceSystem(forceCrs);
            content.getViewport().setBounds(ref);
        }

        synchronized (synchronizedLayers) {
            for( Layer layer : synchronizedLayers ) {
                content.addLayer(layer);
            }
        }

        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(content);

        if (buffer > 0.0) {
            ref = new ReferencedEnvelope(ref);
            ref.expandBy(buffer, buffer);
        }

        double envW = ref.getWidth();
        double envH = ref.getHeight();

        if (envW < envH) {
            double newEnvW = envH * (double) imageWidth / (double) imageHeight;
            double delta = newEnvW - envW;
            ref.expandBy(delta / 2, 0);
        } else {
            double newEnvH = envW * (double) imageHeight / (double) imageWidth;
            double delta = newEnvH - envH;
            ref.expandBy(0, delta / 2.0);
        }

        Rectangle imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
        BufferedImage dumpImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dumpImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        renderer.paint(g2d, imageBounds, ref);

        return dumpImage;
    }

    public void dispose() {
        if (content != null)
            content.dispose();
    }

    /**
     * Writes an image of maps drawn to a png file. 
     * 
     * @param imagePath the path to which to write the image.
     * @param bounds the area of interest.
     * @param imageWidth the width of the image to produce.
     * @param imageHeight the height of the image to produce.
     * @param buffer the buffer to add around the map bounds in map units.
     * @param rgbCheck an rgb tripled. If not <code>null</code> and the image generated is 
     *                  composed only of that color, then the tile is not generated.
     *                  This can be useful to avoid generation of empty tiles. 
     * @throws IOException
     */
    public void dumpPngImage( String imagePath, ReferencedEnvelope bounds, int imageWidth, int imageHeight, double buffer,
            int[] rgbCheck ) throws IOException {
        BufferedImage dumpImage = drawImageWithNewMapContent(bounds, imageWidth, imageHeight, buffer);
        boolean dumpIt = true;
        if (rgbCheck != null)
            dumpIt = !isAllOfCheckColor(rgbCheck, dumpImage);
        if (dumpIt)
            ImageIO.write(dumpImage, "png", new File(imagePath)); //$NON-NLS-1$
    }

    /**
     * Writes an image of maps drawn to a jpg file. 
     * 
     * @param imagePath the path to which to write the image.
     * @param bounds the area of interest.
     * @param imageWidth the width of the image to produce.
     * @param imageHeight the height of the image to produce.
     * @param buffer the buffer to add around the map bounds in map units. 
     * @param rgbCheck an rgb tripled. If not <code>null</code> and the image generated is 
     *                  composed only of that color, then the tile is not generated.
     *                  This can be useful to avoid generation of empty tiles.
     * @throws IOException
     */
    public void dumpJpgImage( String imagePath, ReferencedEnvelope bounds, int imageWidth, int imageHeight, double buffer,
            int[] rgbCheck ) throws IOException {
        BufferedImage dumpImage = drawImageWithNewMapContent(bounds, imageWidth, imageHeight, buffer);
        boolean dumpIt = true;
        if (rgbCheck != null)
            dumpIt = !isAllOfCheckColor(rgbCheck, dumpImage);
        if (dumpIt)
            ImageIO.write(dumpImage, "jpg", new File(imagePath));
    }

    /**
     * Draw the map on an image. 
     * 
     * @param bounds the area of interest.
     * @param imageWidth the width of the image to produce.
     * @param imageHeight the height of the image to produce.
     * @param buffer the buffer to add around the map bounds in map units. 
     * @param rgbCheck an rgb tripled. If not <code>null</code> and the image generated is 
     *                  composed only of that color, then the tile is not generated.
     *                  This can be useful to avoid generation of empty tiles.
     * @throws IOException
     */
    public BufferedImage getImageWithCheck( ReferencedEnvelope bounds, int imageWidth, int imageHeight, double buffer,
            int[] rgbCheck ) throws IOException {
        BufferedImage dumpImage = drawImageWithNewMapContent(bounds, imageWidth, imageHeight, buffer);
        boolean dumpIt = true;
        if (rgbCheck != null)
            dumpIt = !isAllOfCheckColor(rgbCheck, dumpImage);
        if (dumpIt) {
            return dumpImage;
        } else {
            return null;
        }
    }

    private boolean isAllOfCheckColor( int[] rgbCheck, BufferedImage dumpImage ) {
        WritableRaster raster = dumpImage.getRaster();
        for( int i = 0; i < raster.getWidth(); i++ ) {
            for( int j = 0; j < raster.getHeight(); j++ ) {
                int[] value = raster.getPixel(i, j, (int[]) null);
                if (value[0] != rgbCheck[0] || value[1] != rgbCheck[1] || value[2] != rgbCheck[2]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Create an image for a given paper size and scale.
     * 
     * @param imagePath the path to which to write the image.
     * @param bounds the area of interest. In this case only the center is considered. The bounds
     *              are recalculated based in paper size and scale.
     * @param scale the scale wanted for the map.
     * @param paperFormat the paper format to use.
     * @param dpi the wanted dpi. If <code>null</code>, 72dpi is used as default.
     * @param legend an optional legend {@link BufferedImage image}.
     * @param legendX the X position of the legend in the final image.
     * @param legendY the Y position of the legend in the final image.
     * @param scalePrefix if not <code>null</code>, this string will be added before the scale definition.
     *                      If <code>null</code>, no scale definition will be added.
     * @param scaleSize a size for the scale.
     * @param scaleX the X position of the scale in the final image.
     * @param scaleY the X position of the scale in the final image.
     * @throws Exception 
     * @since 0.7.6
     */
    public void dumpPngImageForScaleAndPaper( String imagePath, ReferencedEnvelope bounds, double scale, EPaperFormat paperFormat,
            Double dpi, BufferedImage legend, int legendX, int legendY, String scalePrefix, float scaleSize, int scaleX,
            int scaleY ) throws Exception {
        if (dpi == null) {
            dpi = 72.0;
        }

        // we use the bounds top find the center
        Coordinate centre = bounds.centre();

        double boundsXExtension = paperFormat.width() / 1000.0 * scale;
        double boundsYExtension = paperFormat.height() / 1000.0 * scale;

        Coordinate ll = new Coordinate(centre.x - boundsXExtension / 2.0, centre.y - boundsYExtension / 2.0);
        Coordinate ur = new Coordinate(centre.x + boundsXExtension / 2.0, centre.y + boundsYExtension / 2.0);
        Envelope tmpEnv = new Envelope(ll, ur);
        bounds = new ReferencedEnvelope(tmpEnv, bounds.getCoordinateReferenceSystem());

        int imageWidth = (int) (paperFormat.width() / 25.4 * dpi);
        int imageHeight = (int) (paperFormat.height() / 25.4 * dpi);

        BufferedImage dumpImage = drawImage(bounds, imageWidth, imageHeight, 0);
        Graphics2D graphics = (Graphics2D) dumpImage.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (shapesFile != null && shapesFile.exists()) {
            applyShapes(graphics);
        }
        if (legend != null) {
            graphics.drawImage(legend, null, legendX, legendY);
        }

        if (scalePrefix != null) {
            Font scaleFont = graphics.getFont().deriveFont(scaleSize);
            graphics.setFont(scaleFont);

            FontMetrics fontMetrics = graphics.getFontMetrics(scaleFont);
            String scaleString = scalePrefix + "1:" + (int) scale;
            Rectangle2D stringBounds = fontMetrics.getStringBounds(scaleString, graphics);

            double width = stringBounds.getWidth();
            double height = stringBounds.getHeight();
            graphics.setColor(Color.white);
            double border = 5;
            graphics.fillRect((int) scaleX, (int) (scaleY - height + 2 * border), (int) (width + 3 * border),
                    (int) (height + 2 * border));

            graphics.setColor(Color.black);
            graphics.drawString(scaleString, (int) scaleX + 5, (int) scaleY);

        }
        ImageIO.write(dumpImage, "png", new File(imagePath));

    }

    /**
     * Create an image for a given paper size and scale.
     * 
     * @param imagePath the path to which to write the image.
     * @param bounds the area of interest. In this case only the center is considered. The bounds
     *              are recalculated based in paper size and scale.
     * @param scale the scale wanted for the map.
     * @param paperFormat the paper format to use.
     * @param dpi the wanted dpi. If <code>null</code>, 72dpi is used as default.
     * @param legend an optional legend {@link BufferedImage image}.
     * @param legendX the X position of the legend in the final image.
     * @param legendY the Y position of the legend in the final image.
     * @param scalePrefix if not <code>null</code>, this string will be added before the scale definition.
     *                      If <code>null</code>, no scale definition will be added.
     * @param scaleSize a size for the scale.
     * @param scaleX the X position of the scale in the final image.
     * @param scaleY the X position of the scale in the final image.
     * @throws Exception 
     * @since 0.7.6
     */
    public void dump2Graphics2D( Graphics2D graphics2d, ReferencedEnvelope bounds, double scale, EPaperFormat paperFormat,
            Double dpi, BufferedImage legend, int legendX, int legendY, String scalePrefix, float scaleSize, int scaleX,
            int scaleY ) throws Exception {
        if (dpi == null) {
            dpi = 72.0;
        }

        // we use the bounds top find the center
        Coordinate centre = bounds.centre();

        double boundsXExtension = paperFormat.width() / 1000.0 * scale;
        double boundsYExtension = paperFormat.height() / 1000.0 * scale;

        Coordinate ll = new Coordinate(centre.x - boundsXExtension / 2.0, centre.y - boundsYExtension / 2.0);
        Coordinate ur = new Coordinate(centre.x + boundsXExtension / 2.0, centre.y + boundsYExtension / 2.0);
        Envelope tmpEnv = new Envelope(ll, ur);
        // tmpEnv.expandBy(1000);
        bounds = new ReferencedEnvelope(tmpEnv, bounds.getCoordinateReferenceSystem());

        int imageWidth = (int) (paperFormat.width() / 25.4 * dpi);
        int imageHeight = (int) (paperFormat.height() / 25.4 * dpi);

        drawImage(graphics2d, bounds, imageWidth, imageHeight, 0);
        graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (shapesFile != null && shapesFile.exists()) {
            applyShapes(graphics2d);
        }
        if (legend != null) {
            graphics2d.drawImage(legend, null, legendX, legendY);
        }

        if (scalePrefix != null) {
            Font scaleFont = graphics2d.getFont().deriveFont(scaleSize);
            graphics2d.setFont(scaleFont);

            FontMetrics fontMetrics = graphics2d.getFontMetrics(scaleFont);
            String scaleString = scalePrefix + "1:" + (int) scale;
            Rectangle2D stringBounds = fontMetrics.getStringBounds(scaleString, graphics2d);

            double width = stringBounds.getWidth();
            double height = stringBounds.getHeight();
            graphics2d.setColor(Color.white);
            double border = 5;
            graphics2d.fillRect((int) scaleX, (int) (scaleY - height + 2 * border), (int) (width + 3 * border),
                    (int) (height + 2 * border));

            graphics2d.setColor(Color.black);
            graphics2d.drawString(scaleString, (int) scaleX + 5, (int) scaleY);

        }
    }

    private void applyShapes( Graphics2D graphics ) throws Exception {
        Stream<String> lines = Files.lines(Paths.get(shapesFile.toURI())).distinct()//
                .filter(l -> l.trim().length() != 0);
        lines.forEach(l -> {
            if (l.startsWith("text")) {
                // text;x;y;mytext;colorrgba;size
                String[] split = l.split(";");
                int x = Integer.parseInt(split[1]);
                int y = Integer.parseInt(split[2]);
                String msg = split[3];
                Color color = ColorUtilities.colorFromRbgString(split[4]);
                int size = Integer.parseInt(split[5]);

                graphics.setColor(color);
                graphics.setFont(new Font("Arial", Font.PLAIN, size));
                graphics.drawString(msg, x, y);
            } else if (l.startsWith("box")) {
                // box;x;y;w;h;strokewidth;fillrgba;strokergba
                String[] split = l.split(";");
                int x = Integer.parseInt(split[1]);
                int y = Integer.parseInt(split[2]);
                int w = Integer.parseInt(split[3]);
                int h = Integer.parseInt(split[4]);
                int strokeWidth = Integer.parseInt(split[5]);
                Color colorFill = ColorUtilities.colorFromRbgString(split[6]);
                Color colorStroke = ColorUtilities.colorFromRbgString(split[7]);

                graphics.setColor(colorFill);
                graphics.fillRect(x, y, w, h);

                BasicStroke stroke = new BasicStroke(strokeWidth);
                graphics.setStroke(stroke);
                graphics.setColor(colorStroke);
                graphics.drawRect(x, y, w, h);
            } else if (l.startsWith("roundedbox")) {
                // roundedbox;x;y;w;h;round;strokewidth;fillrgba;strokergba
                String[] split = l.split(";");
                int x = Integer.parseInt(split[1]);
                int y = Integer.parseInt(split[2]);
                int w = Integer.parseInt(split[3]);
                int h = Integer.parseInt(split[4]);
                int round = Integer.parseInt(split[5]);
                int strokeWidth = Integer.parseInt(split[6]);
                Color colorFill = ColorUtilities.colorFromRbgString(split[7]);
                Color colorStroke = ColorUtilities.colorFromRbgString(split[8]);

                graphics.setColor(colorFill);
                graphics.fillRoundRect(x, y, w, h, round, round);

                BasicStroke stroke = new BasicStroke(strokeWidth);
                graphics.setStroke(stroke);
                graphics.setColor(colorStroke);
                graphics.drawRoundRect(x, y, w, h, round, round);
            }
        });

    }
}
