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

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;

import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasRecordElevationComparator;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ThreadedRunnable;
import org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.lesto.modules.raster.adaptivetinfilter.TinHandler;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.algorithm.locate.SimplePointInAreaLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.STRtree;

@Description("Tool for DEM generation from laser scanner data using adaptive tin models .")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("tin, filter, lidar")
@Label(HMConstants.LESTO + "/raster")
@Name("adaptivetinfilter")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
@Bibliography("Dem generation from laser scanner data using adaptive tin models - 01/2000 - P.Axelsson")
@SuppressWarnings("nls")
public class AdaptiveTinFilter extends HMModel {
    @Description("The las folder index file")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas;

    @Description("Input raster to use as output grid template.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTemplate;

    @Description("Number of iterations permitted.")
    @In
    public int pIterations = 20;

    @Description("Support grid resolution in meters (will be recalculated/corrected on grid template).")
    @In
    public double pSecRes = 50.0;

    @Description("Minimum distance threshold.")
    @In
    public double pDistThres = 0.5;

    @Description("Final cleanup distance.")
    @In
    public double pFinalCleanupDist = 1.0;

    @Description("Minimum angle threshold.")
    @In
    public double pAngleThres = 10.0;

    @Description("Tin triangle edge limit (if null, ignored).")
    @In
    public Double pEdgeThres = null;

    @Description("If true the vector files of tin and nonground are dumped to shapefile.")
    @In
    public boolean doTin = false;

    @Description("The start seed triangles.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outSeeds;

    @Description("Final output tin.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTin;

    @Description("Output non ground points.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNonGround;

    @Description("Output tiles.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTiles;

    @Description("The interpolated output raster.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDem;

    private GridCoverage2D inTemplateGC;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, inTemplate, outDem);

        inTemplateGC = getRaster(inTemplate);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inTemplateGC);
        double newCols = Math.ceil(regionMap.getWidth() / pSecRes);
        double newRows = Math.ceil(regionMap.getHeight() / pSecRes);

        // create the support grid used to find the seeds
        OmsGridsGenerator gridGenerator = new OmsGridsGenerator();
        gridGenerator.inRaster = inTemplateGC;
        gridGenerator.pCols = (int) newCols;
        gridGenerator.pRows = (int) newRows;
        gridGenerator.process();
        SimpleFeatureCollection outTilesFC = gridGenerator.outMap;
        if (outTiles != null) {
            dumpVector(outTilesFC, outTiles);
        }
        List<Geometry> secGridGeoms = FeatureUtilities.featureCollectionToGeometriesList(outTilesFC, true, null);

        // find seeds, i.e. lowest points in the sec grids
        List<Coordinate> seedsList = getSeeds(secGridGeoms);

        int defaultThreadsNum = getDefaultThreadsNum();
        // defaultThreadsNum = 1;
        final TinHandler tinHandler = new TinHandler(pm, inTemplateGC.getCoordinateReferenceSystem(), pAngleThres, pDistThres,
                pEdgeThres, defaultThreadsNum);
        tinHandler.setStartCoordinates(seedsList);

        // dump the first seed based tin
        if (outSeeds != null) {
            SimpleFeatureCollection outSeedsFC = tinHandler.toFeatureCollection();
            dumpVector(outSeedsFC, outSeeds);
        }

        try (ALasDataManager lasHandler = ALasDataManager.getDataManager(new File(inLas), null, 0,
                inTemplateGC.getCoordinateReferenceSystem())) {
            lasHandler.open();

            tinHandler.filterOnAllData(lasHandler);

            int iteration = 1;

            boolean firstRound = true;
            do {
                pm.message("Iteration N." + iteration);

                int tinBefore = tinHandler.getCurrentGroundPointsNum();
                if (firstRound) {
                    // use all data, we are at round 1
                    tinHandler.filterOnAllData(lasHandler);
                    firstRound = false;
                } else {
                    tinHandler.filterOnLeftOverData();
                }
                int tinAfter = tinHandler.getCurrentGroundPointsNum();

                int addedPoints = tinAfter - tinBefore;
                pm.message("Points added to the next iteration: " + addedPoints);

                tinHandler.resetTin();

                if (addedPoints == 0) {
                    break;
                }
                iteration++;
            } while( iteration <= pIterations );
        }

        tinHandler.getTriangles();

        /*
         * as a final cleanup do a filter on distance from triangles to remove
         * non picked ground points
         */
        tinHandler.finalCleanup(pFinalCleanupDist);

        final double[] minMaxElev = tinHandler.getMinMaxElev();
        pm.message("Tin triangles min and max elevation:" + Arrays.toString(minMaxElev));

        if (doTin) {
            if (outTin != null) {
                SimpleFeatureCollection outTinFC = tinHandler.toFeatureCollection();
                dumpVector(outTinFC, outTin);
            }

            if (outNonGround != null) {
                SimpleFeatureCollection outNonGroundFC = tinHandler.toFeatureCollectionOthers();
                dumpVector(outNonGroundFC, outNonGround);
            }

        }
        double[] minMaxElev0 = tinHandler.getMinMaxElev();
        doRaster(tinHandler, regionMap, minMaxElev0, 0);

    }

    private void doRaster( TinHandler tinHandler, RegionMap regionMap, final double[] minMaxElev, int iteration )
            throws Exception {
        final WritableRaster[] rasterHandler = new WritableRaster[1];
        GridCoverage2D outDemGC = CoverageUtilities.createCoverageFromTemplate(inTemplateGC, doubleNovalue, rasterHandler);

        final STRtree tinTree = tinHandler.generateTinIndex(null);
        final GridGeometry2D gridGeometry = inTemplateGC.getGridGeometry();
        // pm.beginTask("Generating dem...", regionMap.getCols() *
        // regionMap.getRows());
        ThreadedRunnable tRun = new ThreadedRunnable(getDefaultThreadsNum(), null);
        for( int r = 0; r < regionMap.getRows(); r++ ) {
            for( int c = 0; c < regionMap.getCols(); c++ ) {
                final int col = c;
                final int row = r;
                tRun.executeRunnable(new Runnable(){
                    public void run() {
                        try {
                            DirectPosition directPosition = gridGeometry.gridToWorld(new GridCoordinates2D(col, row));
                            double[] coord = directPosition.getCoordinate();
                            Coordinate coordinate = new Coordinate(coord[0], coord[1]);
                            Envelope e = new Envelope(coordinate);
                            e.expandBy(TinHandler.POINTENVELOPE_EXPAND);
                            List nearTinGeoms = tinTree.query(e);
                            if (nearTinGeoms.size() == 0) {
                                // pm.worked(1);
                                return;
                            }
                            Geometry tinGeom = null;
                            for( int i = 0; i < nearTinGeoms.size(); i++ ) {
                                tinGeom = (Geometry) nearTinGeoms.get(i);
                                SimplePointInAreaLocator pointLoc = new SimplePointInAreaLocator(tinGeom);
                                // check if the point is inside the projection
                                // of the triangle
                                if (pointLoc.locate(coordinate) == Location.INTERIOR) {
                                    break;
                                }
                            }
                            if (tinGeom == null) {
                                pm.errorMessage("Didn't find a matching triangle...");
                                return;
                            }

                            Coordinate[] tinCoords = tinGeom.getCoordinates();

                            Coordinate c1 = new Coordinate(coordinate.x, coordinate.y, 1E6);
                            Coordinate c2 = new Coordinate(coordinate.x, coordinate.y, -1E6);

                            Coordinate intersection = GeometryUtilities.getLineWithPlaneIntersection(c1, c2, tinCoords[0],
                                    tinCoords[1], tinCoords[2]);
                            // set elevation

                            if (intersection != null) {
                                double z = intersection.z;
                                if (z >= minMaxElev[0] && z <= minMaxElev[1]) {
                                    synchronized (rasterHandler) {
                                        rasterHandler[0].setSample(col, row, 0, z);
                                    }
                                }
                            }
                            // pm.worked(1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        }
        tRun.waitAndClose();
        pm.done();

        dumpRaster(outDemGC, outDem);
    }

    private SimpleFeatureCollection featureCollectionFromNonGroundCoordinates( CoordinateReferenceSystem crs,
            List<Coordinate> nonGroundCoordinateList ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("nongroundpoints");
        b.setCRS(crs);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        b.add("the_geom", Point.class);
        b.add("elev", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for( Coordinate c : nonGroundCoordinateList ) {
            Point g = gf.createPoint(c);
            Object[] values = new Object[]{g, c.z};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            newCollection.add(feature);
        }
        return newCollection;
    }

    private List<Coordinate> getSeeds( List<Geometry> secGridGeoms ) throws Exception {
        final List<Coordinate> seedsList = new ArrayList<Coordinate>();
        try (ALasDataManager lasHandler = ALasDataManager.getDataManager(new File(inLas), null, 0,
                inTemplateGC.getCoordinateReferenceSystem())) {
            lasHandler.open();
            int defaultThreadsNum = getDefaultThreadsNum();
            pm.beginTask("Extracting seed points on " + secGridGeoms.size() + " tiles... (cores = " + defaultThreadsNum + ")",
                    secGridGeoms.size());
            ThreadedRunnable tRun = new ThreadedRunnable(defaultThreadsNum, null);
            for( final Geometry secGridGeom : secGridGeoms ) {
                tRun.executeRunnable(new Runnable(){
                    public void run() {
                        try {
                            final List<LasRecord> pointsInGeom = lasHandler.getPointsInGeometry(secGridGeom, true);
                            if (pointsInGeom.size() != 0) {
                                Collections.sort(pointsInGeom, new LasRecordElevationComparator());
                                LasRecord seedPoint = pointsInGeom.get(0);
                                seedsList.add(new Coordinate(seedPoint.x, seedPoint.y, seedPoint.z));
                            } else {
                                pm.errorMessage("No points in: " + secGridGeom);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        pm.worked(1);
                    }
                });
            }
            tRun.waitAndClose();
            pm.done();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return seedsList;
    }

    // public static void main( String[] args ) throws Exception {
    // EggClock egg = new EggClock("time: ", " min");
    // egg.startAndPrint(System.err);
    //
    // String outFolder = "/home/moovida/2014_01_rilievo_helica/axxel/";
    //
    // String outputTiles = outFolder + "zambana_tiles.shp";
    // String outputSeeds = outFolder + "zambana_seeds.shp";
    // String outputTin = outFolder + "zambana_tin.shp";
    // String outputChm = outFolder + "zambana_chm.shp";
    // String outDemPath = outFolder + "zambana_dtm.asc";
    //
    // String las = "/home/moovida/2014_01_rilievo_helica/used/index.lasfolder";
    // String template = "/home/moovida/2014_01_rilievo_helica/zambana_tiles.asc";
    //
    // double secRes = 100.0;
    //
    // AdaptiveTinFilter tin = new AdaptiveTinFilter();
    // tin.inLas = las;
    // tin.inTemplate = getRaster(template);
    // tin.pDistThres = 0.5;
    // tin.pAngleThres = 10;
    // tin.pEdgeThres = null;
    // tin.pSecRes = secRes;
    // tin.pIterations = 30;
    // tin.doTin = true;
    // tin.process();
    //
    // dumpRaster(tin.outDem, outDemPath);
    // dumpVector(tin.outSeeds, outputSeeds);
    // dumpVector(tin.outTin, outputTin);
    // dumpVector(tin.outNonGround, outputChm);
    // dumpVector(tin.outTiles, outputTiles);
    //
    // egg.printTimePassedInMinutes(System.err);
    // }
}
