package org.hortonmachine.database;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.hortonmachine.dbs.geopackage.GeopackageDb;
import org.hortonmachine.dbs.utils.ITilesProducer;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.images.ImageGenerator;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GeopackageTilesProducer implements ITilesProducer {

    private IHMProgressMonitor pm;
    private int minZoom;
    private int maxZoom;
    private int tileSize;
    private CoordinateReferenceSystem mercatorCrs;
    private MapContent mapContent;
    private StreamingRenderer renderer;
    private Rectangle imageBounds;
    private Color transparent = new Color(255, 255, 255, 0);

    public GeopackageTilesProducer( IHMProgressMonitor pm, GridCoverage2D raster3857, int minZoom, int maxZoom, int tileSize )
            throws Exception {
        this.pm = pm;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.tileSize = tileSize;

        mercatorCrs = CrsUtilities.getCrsFromEpsg("EPSG:" + GeopackageDb.MERCATOR_SRID, null);

//        ImageGenerator imageGen = new ImageGenerator(pm, mercatorCrs)

        imageBounds = new Rectangle(0, 0, tileSize, tileSize);
        mapContent = new MapContent();
        mapContent.setTitle("dump");
        mapContent.getViewport().setCoordinateReferenceSystem(mercatorCrs);

        RasterSymbolizer sym = SldUtilities.sf.getDefaultRasterSymbolizer();
        Style style = SLD.wrapSymbolizers(sym);
        GridCoverageLayer gcl = new GridCoverageLayer(raster3857, style, "tiles");
        mapContent.addLayer(gcl);

        renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);

    }

    @Override
    public int getMinZoom() {
        return minZoom;
    }

    @Override
    public int getMaxZoom() {
        return maxZoom;
    }

    @Override
    public boolean cancelled() {
        return pm.isCanceled();
    }

    @Override
    public int getTileSize() {
        return tileSize;
    }

    @Override
    public byte[] getTileData( Envelope tileBounds3857 ) {
        try {

            BufferedImage dumpImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
            ReferencedEnvelope renv = new ReferencedEnvelope(tileBounds3857, mercatorCrs);
            Graphics2D g2d = dumpImage.createGraphics();
            g2d.setColor(transparent);
            g2d.fillRect(0, 0, tileSize, tileSize);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            mapContent.getViewport().setBounds(renv);
            mapContent.getViewport().setScreenArea(imageBounds);
            renderer.paint(g2d, imageBounds, renv);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(dumpImage, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void startWorkingOnZoomLevel( int zoomLevel, int workCount ) {
        pm.beginTask("Working on zoom level " + zoomLevel, workCount);
    }

    @Override
    public void worked() {
        pm.worked(1);
    }

    @Override
    public void done() {
        pm.done();
    }

}
