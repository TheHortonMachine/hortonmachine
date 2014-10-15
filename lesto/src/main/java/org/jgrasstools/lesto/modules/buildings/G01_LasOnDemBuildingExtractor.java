package org.jgrasstools.lesto.modules.buildings;
import static java.lang.Math.abs;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;

import java.awt.image.WritableRaster;
import java.io.File;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ThreadedRunnable;
import org.jgrasstools.gears.libs.modules.Variables;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.modules.r.morpher.OmsMorpher;
import org.jgrasstools.gears.modules.v.vectorize.OmsVectorizer;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

public class G01_LasOnDemBuildingExtractor extends JGTModel {
    String pattern = "oltre";

    public G01_LasOnDemBuildingExtractor() throws Exception {

        final double demthres = 1.8;
        final double minBuildingArea = 25;
        final double rasterResolution = 0.5;
        final String BASE = "/media/lacntfs/geologico/";

        String overviewShpPath = BASE + "las/" + pattern + "_1400/overview_" + pattern + "_1400.shp";
        final String inDem = BASE + "/geologico2013_grassdb/dtm/cell/dtm_" + pattern + "1400";

        SimpleFeatureCollection overviewFC = getVector(overviewShpPath);
        final List<SimpleFeature> overviewList = FeatureUtilities.featureCollectionToList(overviewFC);
        int count = 0;
        ThreadedRunnable< ? > runner = new ThreadedRunnable(2, pm);
        for( final SimpleFeature overview : overviewList ) {
            final int fcount = count;
            count++;
            runner.executeRunnable(new Runnable(){
                public void run() {
                    try {
                        System.out.println("*************************************************");
                        System.out.println("PROCESSING: " + fcount + " of " + overviewList.size());
                        System.out.println("*************************************************");
                        doWork(overview, BASE, inDem, demthres, minBuildingArea, rasterResolution);
                        System.out.println("*************************************************");
                        System.out.println("DONE PROCESSING: " + fcount + " of " + overviewList.size());
                        System.out.println("*************************************************");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        runner.waitAndClose();
    }

    private void doWork( SimpleFeature overview, String BASE, String inDem, double demthres, double minBuildingArea,
            double rasterResolution ) throws Exception {
        // Geometry polygon = (Geometry) overview.getDefaultGeometry();
        String lasName = overview.getAttribute("name").toString();

        String inLas = BASE + "las/" + pattern + "_1400/" + lasName;

        String name = lasName.replaceFirst(".las", "");
        String outBuildingsShp = BASE + "buildings/results_" + pattern + "1400/" + name + "_buildings_" + pattern
                + "1400_min1.8m.shp";
        File outBuildingsShpFile = new File(outBuildingsShp);
        if (outBuildingsShpFile.exists()) {
            return;
        }

        ALasReader r = ALasReader.getReader(new File(inLas), null);
        r.open();

        ILasHeader header = r.getHeader();
        ReferencedEnvelope3D e = header.getDataEnvelope();
        Envelope2D envelope2d = new Envelope2D(e.getCoordinateReferenceSystem(), e.getMinX(), e.getMinY(), e.getWidth(),
                e.getHeight());

        OmsRasterReader reader = new OmsRasterReader();
        reader.file = inDem;
        reader.pNorth = e.getMaxY();
        reader.pSouth = e.getMinY();
        reader.pWest = e.getMinX();
        reader.pEast = e.getMaxX();
        reader.pXres = rasterResolution;
        reader.pYres = rasterResolution;
        reader.process();
        GridCoverage2D raster = reader.outRaster;
        // GridCoverage2D raster = OmsRasterReader.readRaster(inDem);

        WritableRaster[] buildingsHolder = new WritableRaster[1];
        GridCoverage2D newBuildingsRaster = CoverageUtilities.createSubCoverageFromTemplate(raster, envelope2d, 1.0,
                buildingsHolder);
        GridGeometry2D gridGeometry = newBuildingsRaster.getGridGeometry();

        java.awt.Point p = new java.awt.Point();
        IJGTProgressMonitor pm = new LogProgressMonitor();
        pm.beginTask("check", (int) header.getRecordsCount());
        while( r.hasNextPoint() ) {
            LasRecord dot = r.getNextPoint();
            Coordinate c = new Coordinate(dot.x, dot.y);
            if (!envelope2d.contains(dot.x, dot.y)) {
                continue;
            }

            double dtmValue = CoverageUtilities.getValue(raster, c);
            double height = abs(dot.z - dtmValue);
            CoverageUtilities.colRowFromCoordinate(c, gridGeometry, p);
            if (height < demthres) {
                buildingsHolder[0].setSample(p.x, p.y, 0, doubleNovalue);
            }
            // if (height > treeLow && height < treeHigh) {
            // if (dot.classification == NOT_CLASS) {
            // dot.classification = VEG_CLASS;
            // }
            // }
            pm.worked(1);
        }
        pm.done();
        r.close();

        OmsMorpher morpher = new OmsMorpher();
        morpher.pm = pm;
        morpher.inMap = newBuildingsRaster;
        morpher.pMode = Variables.OPEN;
        morpher.process();
        newBuildingsRaster = morpher.outMap;

        OmsVectorizer buildingsVectorizer = new OmsVectorizer();
        buildingsVectorizer.pm = pm;
        buildingsVectorizer.inRaster = newBuildingsRaster;
        buildingsVectorizer.doRemoveHoles = true;
        // TODO check
        double minAreaInCells = minBuildingArea / rasterResolution / rasterResolution;
        buildingsVectorizer.pThres = minAreaInCells;
        buildingsVectorizer.fDefault = "rast";
        buildingsVectorizer.process();

        dumpVector(buildingsVectorizer.outVector, outBuildingsShp);
    }

    public static void main( String[] args ) throws Exception {
        new G01_LasOnDemBuildingExtractor();

    }
}
