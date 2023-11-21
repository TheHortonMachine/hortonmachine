package org.hortonmachine.gears;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;

import org.geotools.geometry.jts.WKBReader;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.PolygonHelper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.InStream;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.overlay.snap.SnapIfNeededOverlayOp;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.util.UniqueCoordinateArrayFilter;

/**
 * Test {@link TestPolygonHelper}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestPolygonHelper extends HMTestCase {

    String polygonWkt = "POLYGON ((0.000065 0.000055, 0.001455 0.00005, 0.0019 0, " +
            "0.00223 0.0005, 0.00223 0.0005, 0.001855 0.0002, " + // doubles
            "0.002265 0.00056, 0.00238 0.000975, 0.001665 0.001085, 0.0022 0.0008, 0.00166 0.00107, 0.00154 0.001125, 0.0022 0.0016, 0.001255 "
            +
            "0.002225, 0.00067 0.002065, 0.00026 0.001185, " +
            "0.0002 0.0019, 0.0002 0.0019, " + // doubles
            "0.000245 0.00185, 0.00014 0.00184, 0.00008 0.00083, 0.000065 0.000055))";

    public File getFile(String relPath) throws Exception {
        URL url = this.getClass().getClassLoader().getResource(relPath);
        File file = new File(url.toURI());
        return file;
    }

    public void testRemoveDuplicates() throws Exception {
        Geometry origGeom = new WKTReader().read(polygonWkt);

        PolygonHelper helper = new PolygonHelper(origGeom);

        helper.removeDuplicatePoints();
        Geometry processedGeom = helper.getGeometry();
        // result is removal of two double points + one point deu to selfintersection,
        // which is also cleaned in the process
        assertEquals(origGeom.getCoordinates().length - 3, processedGeom.getCoordinates().length);
    }

    public void testReducePrecision() throws Exception {
        Geometry origGeom = new WKTReader().read(polygonWkt);

        PolygonHelper helper = new PolygonHelper(origGeom);
        helper.reducePrecision(100000);
        Geometry processedGeom = helper.getGeometry();

        String expected = "POLYGON ((0.00006 0.00006, 0.00146 0.00005, 0.0019 0, 0.00223 0.0005, 0.00223 0.0005, 0.00186 0.0002, 0.00227 0.00056, 0.00238 0.00098, 0.00167 0.00109, 0.0022 0.0008, 0.00166 0.00107, 0.00154 0.00112, 0.0022 0.0016, 0.00126 0.00223, 0.00067 0.00207, 0.00026 0.00119, 0.0002 0.0019, 0.0002 0.0019, 0.00025 0.00185, 0.00014 0.00184, 0.00008 0.00083, 0.00006 0.00006))";
        assertTrue(new WKTReader().read(expected).equalsExact(processedGeom));

        helper = new PolygonHelper(origGeom);
        helper.reducePrecision(1000);
        processedGeom = helper.getGeometry();

        expected = "POLYGON ((0 0, 0.001 0, 0.002 0, 0.002 0.001, 0.002 0.001, 0.002 0, 0.002 0.001, 0.002 0.001, 0.002 0.001, 0.002 0.001, 0.002 0.001, 0.002 0.001, 0.002 0.002, 0.001 0.002, 0.001 0.002, 0 0.001, 0 0.002, 0 0.002, 0 0.002, 0 0.002, 0 0.001, 0 0))";
        assertTrue(new WKTReader().read(expected).equalsExact(processedGeom));
    }

    public void testScale() throws Exception {
        Geometry origGeom = new WKTReader().read(polygonWkt);

        PolygonHelper helper = new PolygonHelper(origGeom);

        helper.applyScaleFactor(100000);
        helper.reducePrecision(100);
        Geometry processedGeom = helper.getGeometry();
        String expected = "POLYGON ((6.5 5.5, 145.5 5, 190 0, 223 50, 223 50, 185.5 20, 226.5 56, 238 97.5, 166.5 108.5, 220 80, 166 107, 154 112.5, 220 160, 125.5 222.5, 67 206.5, 26 118.5, 20 190, 20 190, 24.5 185, 14 184, 8 83, 6.5 5.5))";
        assertTrue(new WKTReader().read(expected).equalsExact(processedGeom));

        helper = new PolygonHelper(processedGeom);
        helper.applyScaleFactor(1 / 100000.0);
        processedGeom = helper.getGeometry();
        assertTrue(origGeom.equalsExact(processedGeom, 0.000001));
    }

    /**
     * Small test case in which complex examples from large files (that do not go into repo) 
     * can be tested. Do not remove.
     * @throws Exception
     */ 
    // public void testComplexIntersection01() throws Exception {
    //     String referenceGeomWkt = "POLYGON ((-3.220815036000474 42.42936681526999, -3.220815036000474 43.4990377164616, -1.959365763999526 43.4990377164616, -1.959365763999526 42.42936681526999, -3.220815036000474 42.42936681526999))";
    //     Geometry referenceGeom = new WKTReader().read(referenceGeomWkt);
    //     PolygonHelper geomAHelper = new PolygonHelper(referenceGeom);
    //     // geomAHelper.applyScaleFactor(scaleFactor);
    //     // geomAHelper.reducePrecision(100);
    //     referenceGeom = geomAHelper.getGeometry();

    //     File intersectionGeometriesFile = getFile("error_geoms.csv");
    //     List<String> lines = FileUtilities.readFileToLinesList(intersectionGeometriesFile.getAbsolutePath());

    //     StringBuilder sb = new StringBuilder("id;sub;wkt;\n");
    //     for (int j = 1; j < lines.size(); j++) {
    //         // System.out.println("processing: " + j);

    //         String wkt = lines.get(j);
    //         // remove trailing ;
    //         wkt = wkt.substring(0, wkt.length() - 1);
    //         Geometry geom = new WKTReader().read(wkt);

    //         // byte[] bytes =
    //         // FileUtilities.readFileToBytes(geom01File.getAbsolutePath());
    //         // Geometry geom01 = new WKBReader().read(bytes);

    //         // loop over geometries and extract only the largest one
    //         // Geometry largestGeom = null;
    //         // double largestArea = 0;
    //         for (int i = 0; i < geom.getNumGeometries(); i++) {
    //             Geometry geometryN = geom.getGeometryN(i);
            
    //             int scaleFactor = 100000;
    //             PolygonHelper helper = new PolygonHelper(geometryN);
    //             helper.removeSelfIntersections();
    //             // helper.applyScaleFactor(scaleFactor);
    //             // helper.removeSelfIntersections();
    //             // helper.reducePrecision(100);
    //             Geometry geometry = helper.getGeometry();
    
    //             if (referenceGeom.intersects(geometry)) {
    //                 Geometry intersection = referenceGeom.intersection(geometry);
    //                 PolygonHelper helper3 = new PolygonHelper(intersection);
    //                 // helper3.applyScaleFactor(1/scaleFactor);
    //                 // System.out.println("intersection: " + intersection.toText());
    //                 sb.append(j).append(";").append(i).append(";").append(intersection.toText() + ";\n");
    //             }
            
            
    //         }


    //     }
    //     FileUtilities.writeFile(sb.toString(),
    //             new File("/home/hydrologis/TMP/JTE_TOPOLOGYERRORS/debug-shapes/error_geoms_intersections.csv"));
    // }

    public void testGridSnapping() throws Exception{
        String wkt = "POLYGON ((0.1 0, 0.1 1, 0.9 0.9, 0.9 0.1, 0.1 0))";
        
        Geometry origGeom = new WKTReader().read(wkt);

        MultiPoint grid = PolygonHelper.createDefaultGrid(origGeom, 1, 0.0, 0.0);

        PolygonHelper helper = new PolygonHelper(origGeom);
        helper.snapToGrid(grid, 0.2);
        Geometry processedGeom = helper.getGeometry();
        
        assertTrue(new WKTReader().read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))").equalsExact(processedGeom, 0.000001));
        
        
        helper = new PolygonHelper(origGeom);
        helper.snapToGrid(grid, 0.101);
        processedGeom = helper.getGeometry();
        
        assertTrue(new WKTReader().read("POLYGON ((0 0, 0 1, 0.9 0.9, 0.9 0.1, 0 0))").equalsExact(processedGeom, 0.000001));
    }
    
    public void testSmallHoles() throws Exception{
        String wkt = "POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0), (0.4 0.59, 0.4 0.5, 0.400001 0.59, 0.4 0.59), (0.1 0.2, 0.1 0.1, 0.2 0.1, 0.2 0.2, 0.1 0.2))";
        
        Geometry origGeom = new WKTReader().read(wkt);

        // remove all with area threshold
        PolygonHelper helper = new PolygonHelper(origGeom);
        helper.removeSmallInternalRings(0.011);
        Geometry processedGeom = helper.getGeometry();
        assertTrue(new WKTReader().read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))").equalsExact(processedGeom, 0.000001));
        
        // remove only smaller one
        helper = new PolygonHelper(origGeom);
        helper.removeSmallInternalRings(0.009);
        processedGeom = helper.getGeometry();
        assertTrue(new WKTReader().read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0), (0.1 0.2, 0.1 0.1, 0.2 0.1, 0.2 0.2, 0.1 0.2))").equalsExact(processedGeom, 0.000001));
        
        // remove only smaller one by length threshold
        helper = new PolygonHelper(origGeom);
        helper.removeSmallRings(0.2, true);
        processedGeom = helper.getGeometry();
        assertTrue(new WKTReader().read("POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0), (0.1 0.2, 0.1 0.1, 0.2 0.1, 0.2 0.2, 0.1 0.2))").equalsExact(processedGeom, 0.000001));   
    }
    
    public void testSimplification() throws Exception{
        String wkt = "POLYGON ((0 1, 0.1 1.04, 0.1 1, 1 1, 0.8 0.5, 1 0, 0.7 0, 0.4 0, 0 0, 0 1))";
        
        Geometry origGeom = new WKTReader().read(wkt);
        
        // remove only points along lines
        PolygonHelper helper = new PolygonHelper(origGeom);
        helper.simplifyGeometry(0);
        Geometry processedGeom = helper.getGeometry();
        assertTrue(new WKTReader().read("POLYGON ((0 1, 0.1 1.04, 0.1 1, 1 1, 0.8 0.5, 1 0, 0 0, 0 1))").equalsExact(processedGeom, 0.000001));

        // remove also minor than 0.05
        helper = new PolygonHelper(origGeom);
        helper.simplifyGeometry(0.05);
        processedGeom = helper.getGeometry();
        assertTrue(new WKTReader().read("POLYGON ((0 1, 1 1, 0.8 0.5, 1 0, 0 0, 0 1))").equalsExact(processedGeom, 0.000001));
        
        // remove also last larger deviation
        helper = new PolygonHelper(origGeom);
        helper.simplifyGeometry(0.2);
        processedGeom = helper.getGeometry();
        assertTrue(new WKTReader().read("POLYGON ((0 1, 1 1, 1 0, 0 0, 0 1))").equalsExact(processedGeom, 0.000001));
        
    }
}
