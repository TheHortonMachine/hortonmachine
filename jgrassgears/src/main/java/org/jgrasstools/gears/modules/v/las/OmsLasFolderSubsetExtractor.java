///*
// * This file is part of JGrasstools (http://www.jgrasstools.org)
// * (C) HydroloGIS - www.hydrologis.com 
// * 
// * JGrasstools is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.jgrasstools.gears.modules.v.las;
//
//import static java.lang.Math.max;
//import static java.lang.Math.min;
//import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
//import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
//import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
//import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.media.jai.iterator.RandomIter;
//
//import oms3.annotations.Author;
//import oms3.annotations.Description;
//import oms3.annotations.Execute;
//import oms3.annotations.In;
//import oms3.annotations.Keywords;
//import oms3.annotations.Label;
//import oms3.annotations.License;
//import oms3.annotations.Name;
//import oms3.annotations.Status;
//
//import org.geotools.coverage.grid.GridCoordinates2D;
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.coverage.grid.GridGeometry2D;
//import org.geotools.geometry.DirectPosition2D;
//import org.jgrasstools.gears.io.las.core.ALasWriter;
//import org.jgrasstools.gears.io.las.core.LasRecord;
//import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
//import org.jgrasstools.gears.io.las.index.LasDataManager;
//import org.jgrasstools.gears.io.las.index.LasIndexer;
//import org.jgrasstools.gears.libs.modules.JGTConstants;
//import org.jgrasstools.gears.libs.modules.JGTModel;
//import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//
//import com.vividsolutions.jts.geom.Polygon;
//
//@Description("Module that can create subsets of an indexed las folder.")
//@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
//@Keywords("las, lidar")
//@Label(JGTConstants.LAS)
//@Name("lasfoldersubsetextractor")
//@Status(OMSHYDRO_DRAFT)
//@License(OMSHYDRO_LICENSE)
//@SuppressWarnings("nls")
//public class OmsLasFolderSubsetExtractor extends JGTModel {
//
//    @Description("Las files folder main index file path.")
//    @In
//    public String inIndexFile = null;
//
//    @Description("An optional raster to use for extraction.")
//    @In
//    public GridCoverage2D inRaster;
//
//    @Description("If true, difference is also applied.")
//    @In
//    public boolean doDifference = true;
//
//    @Description("Elevation threshold.")
//    @In
//    public double pThreshold = 0.0;
//
//    @Description("The output las file.")
//    @In
//    public String outLasFile = null;
//
//    @Execute
//    public void process() throws Exception {
//        checkNull(inIndexFile, inRaster);
//
//        Polygon polygon = CoverageUtilities.getRegionPolygon(inRaster);
//        CoordinateReferenceSystem crs = inRaster.getCoordinateReferenceSystem();
//
//        GridGeometry2D gridGeometry = inRaster.getGridGeometry();
//        RandomIter rasterIter = CoverageUtilities.getRandomIterator(inRaster);
//
//        pm.beginTask("Reading data...", -1);
//        LasDataManager lasData = new LasDataManager(new File(inIndexFile), null, 0.0, crs);
//        List<LasRecord> pointsInGeometry;
//        try {
//            lasData.open();
//            pointsInGeometry = lasData.getPointsInGeometry(polygon, false);
//        } finally {
//            lasData.close();
//        }
//        pm.done();
//
//        List<LasRecord> newPoints = new ArrayList<LasRecord>();
//        pm.beginTask("Extracting data...", pointsInGeometry.size());
//        double xMin = Double.POSITIVE_INFINITY;
//        double yMin = Double.POSITIVE_INFINITY;
//        double zMin = Double.POSITIVE_INFINITY;
//        double xMax = Double.NEGATIVE_INFINITY;
//        double yMax = Double.NEGATIVE_INFINITY;
//        double zMax = Double.NEGATIVE_INFINITY;
//        for( LasRecord lasRecord : pointsInGeometry ) {
//
//            GridCoordinates2D grid = gridGeometry.worldToGrid(new DirectPosition2D(lasRecord.x, lasRecord.y));
//
//            double value = rasterIter.getSampleDouble(grid.x, grid.y, 0);
//            if (JGTConstants.isNovalue(value)) {
//                pm.worked(1);
//                continue;
//            }
//
//            if (lasRecord.z > value) {
//                // keep it
//                if (doDifference) {
//                    lasRecord.z = lasRecord.z - value;
//                }
//                if (lasRecord.z > pThreshold) {
//                    newPoints.add(lasRecord);
//
//                    xMin = min(xMin, lasRecord.x);
//                    yMin = min(yMin, lasRecord.y);
//                    zMin = min(zMin, lasRecord.z);
//                    xMax = max(xMax, lasRecord.x);
//                    yMax = max(yMax, lasRecord.y);
//                    zMax = max(zMax, lasRecord.z);
//                }
//            }
//            pm.worked(1);
//        }
//        pm.done();
//
//        pm.beginTask("Writing las file...", -1);
//        File outFile = new File(outLasFile);
//        ALasWriter w = new LasWriter(outFile, crs);
//        w.open();
//        w.setBounds(xMin, xMax, yMin, yMax, zMin, zMax);
//        for( LasRecord lasRecord : newPoints ) {
//            w.addPoint(lasRecord);
//        }
//        w.close();
//        pm.done();
//
//        // index it
//        pm.beginTask("Writing las indexes...", -1);
//        LasIndexer indexer = new LasIndexer();
//        indexer.inFolder = outFile.getParentFile().getAbsolutePath();
//        indexer.pCode = "EPSG:32632";
//        indexer.process();
//        pm.done();
//
//    }
//
////    public static void main( String[] args ) throws Exception {
////        int plot = 485;
////
////        String lasIndex = "/home/moovida/dati_unibz/Dati_LiDAR/LAS_Classificati/index.lasfolder";
////        String raster = "/media/FATBOTTOMED/dati_unibz/RILIEVI/plot_" + plot + "/dtm0_5_zone" + plot + "_opened.tif";
////        String outLas = "/media/FATBOTTOMED/dati_unibz/RILIEVI/plot_" + plot + "/pointcloud_on_openeddiff_" + plot + ".las";
////
////        EggClock timer = new EggClock("Time check: ", " min\n");
////        timer.start();
////
////        LasOverSurfaceExtractor vectorializer = new LasOverSurfaceExtractor();
////        vectorializer.inIndexFile = lasIndex;
////        vectorializer.inRaster = getRaster(raster);
////        vectorializer.doDifference = true;
////        vectorializer.pThreshold = 1.3;
////        vectorializer.outLasFile = outLas;
////        vectorializer.process();
////
////        timer.printTimePassedInMinutes(System.err);
////    }
//
//    private static String makeSafe( double num ) {
//        return String.valueOf(num).replace('.', '_');
//    }
//
//}
