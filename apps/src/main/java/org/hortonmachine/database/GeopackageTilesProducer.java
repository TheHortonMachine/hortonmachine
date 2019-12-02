package org.hortonmachine.database;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.dbs.geopackage.GeopackageDb;
import org.hortonmachine.dbs.utils.ITilesProducer;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.images.ImageGenerator;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GeopackageTilesProducer implements ITilesProducer {

    private IHMProgressMonitor pm;
    private int minZoom;
    private int maxZoom;
    private int tileSize;
    private CoordinateReferenceSystem mercatorCrs;
    private ImageGenerator imageGen;

    public GeopackageTilesProducer( IHMProgressMonitor pm, String filePath, boolean isRaster, int minZoom, int maxZoom,
            int tileSize ) throws Exception {
        this.pm = pm;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.tileSize = tileSize;

        mercatorCrs = CrsUtilities.getCrsFromEpsg("EPSG:" + GeopackageDb.MERCATOR_SRID, null);

        imageGen = new ImageGenerator(pm, mercatorCrs);
        if (isRaster) {
            imageGen.addCoveragePath(filePath);
        } else {
            imageGen.addFeaturePath(filePath, null);
        }
        imageGen.setLayers();
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
            BufferedImage dumpImage = imageGen.getImageWithCheck(new ReferencedEnvelope(tileBounds3857, mercatorCrs), tileSize,
                    tileSize, 0.0, null);
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
