package org.hortonmachine.database;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.utils.ITilesProducer;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.images.ImageGenerator;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GeopackageTilesProducer implements ITilesProducer {

    private IHMProgressMonitor pm;
    private int minZoom;
    private int maxZoom;
    private int tileSize;
    private CoordinateReferenceSystem mercatorCrs;
    private ImageGenerator imageGen;
    private PreparedGeometry limitsGeom3857;

    public GeopackageTilesProducer( IHMProgressMonitor pm, String filePath, boolean isRaster, int minZoom, int maxZoom,
            int tileSize, PreparedGeometry limitsGeom3857 ) throws Exception {
        this.pm = pm;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.tileSize = tileSize;
        this.limitsGeom3857 = limitsGeom3857;

        mercatorCrs = CrsUtilities.getCrsFromEpsg("EPSG:" + GeopackageCommonDb.MERCATOR_SRID, null);

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
            if (limitsGeom3857 != null) {
                Polygon envPoly = GeometryUtilities.createPolygonFromEnvelope(tileBounds3857);
                if (!limitsGeom3857.intersects(envPoly)) {
                    return null;
                }
            }
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

    @Override
    public Envelope areaConstraint() {
        if (limitsGeom3857 != null) {
            return limitsGeom3857.getGeometry().getEnvelopeInternal();
        }
        return null;
    }

}
