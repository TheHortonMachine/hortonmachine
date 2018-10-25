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
package org.hortonmachine.lesto.modules.raster;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

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
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ThreadedRunnable;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

@Description("Module that creates a DSM from the triangulation of point clouds.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("triangulation, lidar, dsm")
@Label(HMConstants.LESTO + "/raster")
@Name("lastraingulation2dsm")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class LasTriangulation2Dsm extends HMModel {

    @Description("Las file path.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas = null;

    @Description("A dtm raster to use for the area of interest.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm;

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
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, inDtm, outRaster);

        GridCoverage2D inDtmGC = getRaster(inDtm);
        Polygon polygon = CoverageUtilities.getRegionPolygon(inDtmGC);
        CoordinateReferenceSystem crs = inDtmGC.getCoordinateReferenceSystem();

        List<Coordinate> lasCoordinates = new ArrayList<Coordinate>();
        pm.beginTask("Preparing triangulation...", -1);
        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inLas), null, 0.0, crs)) {
            lasData.open();
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

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDtmGC);
        double north = regionMap.getNorth();
        double south = regionMap.getSouth();
        double east = regionMap.getEast();
        double west = regionMap.getWest();

        if (pXres == null || pYres == null) {
            pXres = regionMap.getXres();
            pYres = regionMap.getYres();
        }
        final int newRows = (int) round((north - south) / pYres);
        int newCols = (int) round((east - west) / pXres);

        final GridGeometry2D newGridGeometry2D = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west,
                newCols, newRows, crs);
        RegionMap newRegionMap = CoverageUtilities.gridGeometry2RegionParamsMap(newGridGeometry2D);
        final WritableRaster newWR = CoverageUtilities.createWritableRaster(newCols, newRows, null, null,
                HMConstants.doubleNovalue);

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

        GridCoverage2D outRasterGC = CoverageUtilities.buildCoverage("outraster", newWR, newRegionMap, crs);
        dumpRaster(outRasterGC, outRaster);
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
                            pm.errorMessage(triangle.toText());
                            pm.errorMessage(gf.createPoint(intersection).toText());
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

}
