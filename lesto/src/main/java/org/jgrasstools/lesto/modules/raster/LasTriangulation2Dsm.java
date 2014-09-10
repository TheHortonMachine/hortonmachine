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
package org.jgrasstools.lesto.modules.raster;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.index.LasDataManager;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ThreadedRunnable;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

@Description("Module that creates a DSM from the triangulation of point clouds.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("triangulation, lidar, dsm")
@Label(JGTConstants.LAS + "/raster")
@Name("lastraingulation2dsm")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class LasTriangulation2Dsm extends JGTModel {

    @Description("Las files folder main index file path.")
    @In
    public String inIndexFile = null;

    @Description("A dtm raster to use for the area of interest.")
    @In
    public GridCoverage2D inDtm;

    @Description("New x resolution (if null, the dtm is used).")
    @In
    public Double pXres;

    @Description("New y resolution (if null, the dtm is used).")
    @In
    public Double pYres;

    @Description("Elevation threshold for triangles.")
    @In
    public double pElevThres = 0.5;

    @Description("The output raster.")
    @Out
    public GridCoverage2D outRaster = null;

    @Execute
    public void process() throws Exception {
        checkNull(inIndexFile, inDtm);

        CoordinateReferenceSystem crs = null;
        Polygon polygon = CoverageUtilities.getRegionPolygon(inDtm);
        crs = inDtm.getCoordinateReferenceSystem();

        List<Coordinate> lasCoordinates = new ArrayList<Coordinate>();
        try (LasDataManager lasData = new LasDataManager(new File(inIndexFile), null, 0.0, crs)) {
            lasData.open();
            pm.beginTask("Preparing triangulation...", -1);
            List<LasRecord> lasPoints = lasData.getPointsInGeometry(polygon, false);
            for( LasRecord lasRecord : lasPoints ) {
                lasCoordinates.add(new Coordinate(lasRecord.x, lasRecord.y, lasRecord.z));
            }
        }

        DelaunayTriangulationBuilder triangulationBuilder = new DelaunayTriangulationBuilder();
        triangulationBuilder.setSites(lasCoordinates);
        Geometry triangles = triangulationBuilder.getTriangles(gf);
        pm.done();

        int numTriangles = triangles.getNumGeometries();
        pm.beginTask("Extracting triangles based on threshold...", numTriangles);
        ArrayList<Geometry> trianglesList = new ArrayList<Geometry>();
        for( int i = 0; i < numTriangles; i++ ) {
            pm.worked(1);
            Geometry geometryN = triangles.getGeometryN(i);
            Coordinate[] coordinates = geometryN.getCoordinates();
            double diff1 = abs(coordinates[0].z - coordinates[1].z);
            if (diff1 > pElevThres) {
                continue;
            }
            double diff2 = abs(coordinates[0].z - coordinates[2].z);
            if (diff2 > pElevThres) {
                continue;
            }
            double diff3 = abs(coordinates[1].z - coordinates[2].z);
            if (diff3 > pElevThres) {
                continue;
            }
            trianglesList.add(geometryN);
        }
        pm.done();

        int newNumTriangles = trianglesList.size();
        int removedNum = numTriangles - newNumTriangles;
        pm.message("Original triangles: " + numTriangles);
        pm.message("New triangles: " + newNumTriangles);
        pm.message("Removed triangles: " + removedNum);

        pm.beginTask("Create triangles index...", newNumTriangles);
        final STRtree tree = new STRtree(trianglesList.size());
        for( Geometry triangle : trianglesList ) {
            Envelope env = triangle.getEnvelopeInternal();
            tree.insert(env, triangle);
            pm.worked(1);
        }
        pm.done();

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDtm);
        double north = regionMap.getNorth();
        double south = regionMap.getSouth();
        double east = regionMap.getEast();
        double west = regionMap.getWest();

        final int newRows = (int) round((north - south) / pYres);
        int newCols = (int) round((east - west) / pXres);

        final GridGeometry2D newGridGeometry2D = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west,
                newCols, newRows, crs);
        RegionMap newRegionMap = CoverageUtilities.gridGeometry2RegionParamsMap(newGridGeometry2D);
        final WritableRaster newWR = CoverageUtilities.createDoubleWritableRaster(newCols, newRows, null, null,
                JGTConstants.doubleNovalue);

        ThreadedRunnable< ? > runner = new ThreadedRunnable(getDefaultThreadsNum(), null);
        pm.beginTask("Setting raster points...", newCols);
        for( int c = 0; c < newCols; c++ ) {
            final int fCol = c;
            runner.executeRunnable(new Runnable(){
                public void run() {
                    try {
                        makeRow(tree, newRows, newGridGeometry2D, newWR, fCol);
                    } catch (TransformException e) {
                        e.printStackTrace();
                    }
                    pm.worked(1);
                }
            });
        }
        runner.waitAndClose();
        pm.done();

        outRaster = CoverageUtilities.buildCoverage("outraster", newWR, newRegionMap, crs);

    }

    private void makeRow( STRtree tree, int newRows, GridGeometry2D newGridGeometry2D, WritableRaster newWR, int c )
            throws TransformException {
        for( int r = 0; r < newRows; r++ ) {
            DirectPosition worldPosition = newGridGeometry2D.gridToWorld(new GridCoordinates2D(c, r));
            double[] wpCoords = worldPosition.getCoordinate();
            Coordinate coordinate = new Coordinate(wpCoords[0], wpCoords[1]);
            Point point = gf.createPoint(coordinate);
            Envelope e = new Envelope(coordinate);
            List interceptedTriangles = tree.query(e);
            if (interceptedTriangles.size() == 0) {
                continue;
            }
            double newElev = -9999.0;
            for( Object object : interceptedTriangles ) {
                if (object instanceof Geometry) {
                    Geometry triangle = (Geometry) object;
                    if (!triangle.intersects(point)) {
                        continue;
                    }
                    Coordinate[] triangleCoords = triangle.getCoordinates();
                    Coordinate c1 = new Coordinate(coordinate.x, coordinate.y, 1E4);
                    Coordinate c2 = new Coordinate(coordinate.x, coordinate.y, -1E4);
                    Coordinate intersection = GeometryUtilities.getLineWithPlaneIntersection(c1, c2, triangleCoords[0],
                            triangleCoords[1], triangleCoords[2]);
                    double maxZ = max(triangleCoords[0].z, triangleCoords[1].z);
                    maxZ = max(maxZ, triangleCoords[2].z);
                    if (intersection.z > maxZ) {
                        boolean equals = NumericsUtilities.dEq(intersection.z, maxZ, 0.0001);
                        if (!equals) {
                            System.out.println(triangle.toText());
                            System.out.println(gf.createPoint(intersection).toText());
                            throw new RuntimeException("Intersection can't be  > than the triangle.");
                        }
                    }
                    if (intersection.z > newElev) {
                        newElev = intersection.z;
                    }
                }
            }
            synchronized (newWR) {
                newWR.setSample(c, r, 0, newElev);
            }
        }
    }

    // public static void main( String[] args ) throws Exception {
    // int plot = 1223;
    //
    // double xRes = 0.5;
    // double yRes = 0.5;
    // double pElevThres = 100.0;
    //
    // String lasIndex = "/home/moovida/dati_unibz/Dati_LiDAR/LAS_Classificati/index.lasfolder";
    // String raster = "/media/FATBOTTOMED/dati_unibz/grassdb/unibz_utm32n/plots/cell/dtm_zone" +
    // plot;
    // // String outRaster = "/media/FATBOTTOMED/dati_unibz/grassdb/unibz_utm32n/plots/cell/dtm" +
    // // makeSafe(xRes) + "_3zone" + plot + ".tif";
    // String outRaster = "/media/FATBOTTOMED/dati_unibz/RILIEVI/plot_" + plot + "/dtm" +
    // makeSafe(xRes) + "_zone" + plot
    // + ".tif";
    //
    // EggClock timer = new EggClock("Time check: ", " min\n");
    // timer.start();
    //
    // LasTriangulation2Dsm l2r = new LasTriangulation2Dsm();
    // l2r.inIndexFile = lasIndex;
    // l2r.inDtm = getRaster(raster);
    // l2r.pElevThres = pElevThres;
    // l2r.pXres = xRes;
    // l2r.pYres = yRes;
    // l2r.process();
    //
    // dumpRaster(l2r.outRaster, outRaster);
    //
    // timer.printTimePassedInMinutes(System.err);
    // }

}
