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
package org.hortonmachine.gears.modules.r.scanline;

import static org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer.*;
import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.gridGeometry2RegionParamsMap;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.gridGeometryFromRegionValues;

import java.awt.image.WritableRaster;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.precision.SimpleGeometryPrecisionReducer;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSSCANLINERASTERIZER_DESCRIPTION)
@Documentation(OMSSCANLINERASTERIZER_DOCUMENTATION)
@Author(name = OMSSCANLINERASTERIZER_AUTHORNAMES, contact = OMSSCANLINERASTERIZER_AUTHORCONTACTS)
@Keywords(OMSSCANLINERASTERIZER_KEYWORDS)
@Label(OMSSCANLINERASTERIZER_LABEL)
@Name(OMSSCANLINERASTERIZER_NAME)
@Status(OMSSCANLINERASTERIZER_STATUS)
@License(OMSSCANLINERASTERIZER_LICENSE)
public class OmsScanLineRasterizer extends HMModel {

    @Description(OMSSCANLINERASTERIZER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OMSSCANLINERASTERIZER_P_VALUE_DESCRIPTION)
    @In
    public Double pValue = null;

    @Description(OMSSCANLINERASTERIZER_F_CAT_DESCRIPTION)
    @In
    public String fCat = null;

    @Description(OMSSCANLINERASTERIZER_P_NORTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSSCANLINERASTERIZER_P_SOUTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSSCANLINERASTERIZER_P_WEST_DESCRIPTION)
    @UI(HMConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSSCANLINERASTERIZER_P_EAST_DESCRIPTION)
    @UI(HMConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSSCANLINERASTERIZER_P_ROWS_DESCRIPTION)
    @UI(HMConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSSCANLINERASTERIZER_P_COLS_DESCRIPTION)
    @UI(HMConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSSCANLINERASTERIZER_P_USEPIP_DESCRIPTION)
    @In
    public Boolean pUsePointInPolygon = false;

    @Description(OMSSCANLINERASTERIZER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSSCANLINERASTERIZER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    // PARAMS DESCR START
    public static final String OMSSCANLINERASTERIZER_DESCRIPTION = "Module for polygon vector to raster conversion.";
    public static final String OMSSCANLINERASTERIZER_DOCUMENTATION = "OmsScanLineRasterizer.html";
    public static final String OMSSCANLINERASTERIZER_KEYWORDS = "Raster, Vector, Rasterize";
    public static final String OMSSCANLINERASTERIZER_LABEL = RASTERPROCESSING;
    public static final String OMSSCANLINERASTERIZER_NAME = "rscanline";
    public static final int OMSSCANLINERASTERIZER_STATUS = 40;
    public static final String OMSSCANLINERASTERIZER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSSCANLINERASTERIZER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSSCANLINERASTERIZER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSSCANLINERASTERIZER_IN_VECTOR_DESCRIPTION = "The vector to rasterize.";
    public static final String OMSSCANLINERASTERIZER_P_VALUE_DESCRIPTION = "The value to use as raster value if no field is given.";
    public static final String OMSSCANLINERASTERIZER_F_CAT_DESCRIPTION = "The field to use to retrieve the category value for the raster.";
    public static final String OMSSCANLINERASTERIZER_P_NORTH_DESCRIPTION = "The north bound of the region to consider";
    public static final String OMSSCANLINERASTERIZER_P_SOUTH_DESCRIPTION = "The south bound of the region to consider";
    public static final String OMSSCANLINERASTERIZER_P_WEST_DESCRIPTION = "The west bound of the region to consider";
    public static final String OMSSCANLINERASTERIZER_P_EAST_DESCRIPTION = "The east bound of the region to consider";
    public static final String OMSSCANLINERASTERIZER_P_ROWS_DESCRIPTION = "The rows of the region to consider";
    public static final String OMSSCANLINERASTERIZER_P_COLS_DESCRIPTION = "The cols of the region to consider";
    public static final String OMSSCANLINERASTERIZER_P_MAX_THREADS_DESCRIPTION = "Max threads to use (default 4)";

    public static final String OMSSCANLINERASTERIZER_P_USEPIP_DESCRIPTION = "Use point in polygon (needs an input raster). In case scanline doesn't work.";
    public static final String OMSSCANLINERASTERIZER_IN_RASTER_DESCRIPTION = "An optional raster to take the values and region from.";
    public static final String OMSSCANLINERASTERIZER_OUT_RASTER_DESCRIPTION = "The output raster.";
    // PARAMS DESCR END

    private WritableRaster outWR;

    private int height;

    private int width;

    private GeometryFactory gf = GeometryUtilities.gf();

    private RegionMap paramsMap;

    private double xRes;

    private RandomIter inIter;

    @Execute
    public void process() throws Exception {
        checkNull(inVector);
        if (pValue == null && fCat == null) {
            throw new ModelsIllegalargumentException("One of pValue or the fCat have to be defined.", this, pm);
        }
        if (pNorth == null || pSouth == null || pWest == null || pEast == null || pRows == null || pCols == null) {
            if (inRaster == null) {
                throw new ModelsIllegalargumentException(
                        "It is necessary to supply all the information about the processing region. Did you set the boundaries and rows/cols?",
                        this, pm);
            }
        }

        if (inRaster != null) {
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
            pNorth = regionMap.getNorth();
            pSouth = regionMap.getSouth();
            pWest = regionMap.getWest();
            pEast = regionMap.getEast();
            pRows = regionMap.getRows();
            pCols = regionMap.getCols();

            inIter = CoverageUtilities.getRandomIterator(inRaster);
        }

        SimpleFeatureType schema = inVector.getSchema();
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        GridGeometry2D pGrid;
        if (inRaster != null) {
            pGrid = inRaster.getGridGeometry();
        } else {
            pGrid = gridGeometryFromRegionValues(pNorth, pSouth, pEast, pWest, pCols, pRows, crs);
        }
        if (outWR == null) {
            paramsMap = gridGeometry2RegionParamsMap(pGrid);
            height = paramsMap.getRows();
            width = paramsMap.getCols();
            xRes = paramsMap.getXres();

            outWR = CoverageUtilities.createWritableRaster(width, height, null, null, doubleNovalue);
        }

        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
        if (EGeometryType.isPoint(geometryDescriptor)) {
            throw new ModelsRuntimeException("Not implemented yet for points", this.getClass().getSimpleName());
        } else if (EGeometryType.isLine(geometryDescriptor)) {
            throw new ModelsRuntimeException("Not implemented yet for lines", this.getClass().getSimpleName());
        } else if (EGeometryType.isPolygon(geometryDescriptor)) {

            if (pUsePointInPolygon) {
                if (inRaster == null) {
                    throw new ModelsIllegalargumentException("The point in polygon mode needs an input raster to work on.", this);
                }
                pm.beginTask("Prepare input data...", IHMProgressMonitor.UNKNOWN);
                List<Geometry> allGeoms = FeatureUtilities.featureCollectionToGeometriesList(inVector, false, null);
                Geometry allGeomsUnion = CascadedPolygonUnion.union(allGeoms);
                PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(allGeomsUnion);
                pm.done();

                double value = pValue;
                pm.beginTask("Rasterizing...", height);
                WritableRandomIter wIter = CoverageUtilities.getWritableRandomIterator(outWR);
                for( int row = 0; row < height; row++ ) {
                    for( int col = 0; col < width; col++ ) {
                        Coordinate coord = CoverageUtilities.coordinateFromColRow(col, row, pGrid);
                        if (preparedGeometry.intersects(gf.createPoint(coord))) {
                            wIter.setSample(col, col, 0, value);
                        }
                    }
                    pm.worked(1);
                }
                pm.done();
                wIter.done();
            } else {
                rasterizepolygon(pGrid);
            }
        } else {
            throw new ModelsIllegalargumentException("Couldn't recognize the geometry type of the file.",
                    this.getClass().getSimpleName(), pm);
        }

        outRaster = CoverageUtilities.buildCoverage("rasterized", outWR, paramsMap,
                inVector.getSchema().getCoordinateReferenceSystem());

    }
    private void rasterizepolygon( final GridGeometry2D gridGeometry ) throws InvalidGridGeometryException, TransformException {

        int size = inVector.size();
        pm.beginTask("Rasterizing features...", size);
        FeatureIterator<SimpleFeature> featureIterator = inVector.features();

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(getDefaultThreadsNum());

        while( featureIterator.hasNext() ) {
            final SimpleFeature feature = featureIterator.next();

            // extract the value to put into the raster.
            double tmpValue = -1.0;
            if (pValue == null) {
                tmpValue = ((Number) feature.getAttribute(fCat)).doubleValue();
            } else {
                tmpValue = pValue;
            }
            final double value = tmpValue;
            final double delta = xRes / 4.0;

            Runnable runner = new Runnable(){
                public void run() {
                    try {
                        Geometry geometry = (Geometry) feature.getDefaultGeometry();
                        int numGeometries = geometry.getNumGeometries();
                        for( int i = 0; i < numGeometries; i++ ) {
                            final Geometry geometryN = geometry.getGeometryN(i);
                            // PreparedGeometry preparedGeometryN =
                            // PreparedGeometryFactory.prepare(geometryN);
                            for( int r = 0; r < height; r++ ) {
                                // do scan line to fill the polygon
                                double[] westPos = gridGeometry.gridToWorld(new GridCoordinates2D(0, r)).getCoordinate();
                                double[] eastPos = gridGeometry.gridToWorld(new GridCoordinates2D(width - 1, r)).getCoordinate();
                                Coordinate west = new Coordinate(westPos[0], westPos[1]);
                                Coordinate east = new Coordinate(eastPos[0], eastPos[1]);
                                LineString line = gf.createLineString(new Coordinate[]{west, east});
                                if (geometryN.intersects(line)) {
                                    Geometry internalLines = geometryN.intersection(line);
                                    int lineNums = internalLines.getNumGeometries();
                                    for( int l = 0; l < lineNums; l++ ) {
                                        Coordinate[] coords = internalLines.getGeometryN(l).getCoordinates();
                                        if (coords.length == 2) {
                                            for( int j = 0; j < coords.length; j = j + 2 ) {
                                                Coordinate startC = new Coordinate(coords[j].x + delta, coords[j].y);
                                                Coordinate endC = new Coordinate(coords[j + 1].x - delta, coords[j + 1].y);

                                                DirectPosition2D startDP;
                                                DirectPosition2D endDP;
                                                if (startC.x < endC.x) {
                                                    startDP = new DirectPosition2D(startC.x, startC.x);
                                                    endDP = new DirectPosition2D(endC.x, endC.x);
                                                } else {
                                                    startDP = new DirectPosition2D(endC.x, endC.x);
                                                    endDP = new DirectPosition2D(startC.x, startC.x);
                                                }
                                                GridCoordinates2D startGridCoord = gridGeometry.worldToGrid(startDP);
                                                GridCoordinates2D endGridCoord = gridGeometry.worldToGrid(endDP);

                                                /*
                                                 * the part in between has to be filled
                                                 */
                                                for( int k = startGridCoord.x; k <= endGridCoord.x; k++ ) {
                                                    if (inIter != null && fCat == null) {
                                                        double v = inIter.getSampleDouble(k, r, 0);
                                                        outWR.setSample(k, r, 0, v);
                                                    } else {
                                                        outWR.setSample(k, r, 0, value);
                                                    }
                                                }
                                            }
                                        } else {
                                            if (coords.length == 1) {
                                                pm.errorMessage(MessageFormat.format("Found a cusp in: {0}/{1}", coords[0].x,
                                                        coords[0].y));
                                            } else {
                                                throw new ModelsIOException(MessageFormat.format(
                                                        "Found intersection with more than 2 points in: {0}/{1}", coords[0].x,
                                                        coords[0].y), this);
                                            }
                                        }
                                    }

                                }
                            }
                        }

                        pm.worked(1);
                    } catch (Exception e) {
                        pm.errorMessage(e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                }
            };
            fixedThreadPool.execute(runner);
        }

        try {
            fixedThreadPool.shutdown();
            fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
            fixedThreadPool.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pm.done();
        featureIterator.close();
    }
}
