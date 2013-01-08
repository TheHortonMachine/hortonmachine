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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.gridGeometry2RegionParamsMap;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.gridGeometryFromRegionValues;
import static org.jgrasstools.gears.utils.geometry.GeometryUtilities.getGeometryType;

import java.awt.image.WritableRaster;
import java.text.MessageFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities.GEOMETRYTYPE;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

@Description("Module for polygon vector to raster conversion.")
@Documentation("OmsScanLineRasterizer.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Raster, Vector, Rasterize")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.CERTIFIED)
@Name("_rscanline")
@License("General Public License Version 3 (GPLv3)")
@SuppressWarnings("nls")
public class OmsScanLineRasterizer extends JGTModel {

    @Description("The vector to rasterize.")
    @In
    public SimpleFeatureCollection inVector = null;

    @Description("The value to use as raster value if no field is given.")
    @In
    public Double pValue = null;

    @Description("The field to use to retrieve the category value for the raster.")
    @In
    public String fCat = null;

    @Description("The north bound of the region to consider")
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description("The south bound of the region to consider")
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description("The west bound of the region to consider")
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description("The east bound of the region to consider")
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description("The rows of the region to consider")
    @UI(JGTConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description("The cols of the region to consider")
    @UI(JGTConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description("Max threads to use (default 4)")
    @In
    public Integer pMaxThreads = 4;

    @Description("The output raster.")
    @Out
    public GridCoverage2D outRaster;

    private WritableRaster outWR;

    private int height;

    private int width;

    private GeometryFactory gf = GeometryUtilities.gf();

    private RegionMap paramsMap;

    private double xRes;

    @Execute
    public void process() throws Exception {
        checkNull(inVector);
        if (pValue == null && fCat == null) {
            throw new ModelsIllegalargumentException("One of pValue or the fCat have to be defined.", this);
        }
        if (pNorth == null || pSouth == null || pWest == null || pEast == null || pRows == null || pCols == null) {
            throw new ModelsIllegalargumentException(
                    "It is necessary to supply all the information about the processing region. Did you set the boundaries and rows/cols?",
                    this);
        }

        SimpleFeatureType schema = inVector.getSchema();
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        GridGeometry2D pGrid = gridGeometryFromRegionValues(pNorth, pSouth, pEast, pWest, pCols, pRows, crs);
        if (outWR == null) {
            paramsMap = gridGeometry2RegionParamsMap(pGrid);
            height = paramsMap.getRows();
            width = paramsMap.getCols();
            xRes = paramsMap.getXres();

            outWR = CoverageUtilities.createDoubleWritableRaster(width, height, null, null, doubleNovalue);
        }

        GeometryType type = schema.getGeometryDescriptor().getType();
        if (getGeometryType(type) == GEOMETRYTYPE.POINT || getGeometryType(type) == GEOMETRYTYPE.MULTIPOINT) {
            throw new ModelsRuntimeException("Not implemented yet for points", this.getClass().getSimpleName());
        } else if (getGeometryType(type) == GEOMETRYTYPE.LINE || getGeometryType(type) == GEOMETRYTYPE.MULTILINE) {
            throw new ModelsRuntimeException("Not implemented yet for lines", this.getClass().getSimpleName());
        } else if (getGeometryType(type) == GEOMETRYTYPE.POLYGON || getGeometryType(type) == GEOMETRYTYPE.MULTIPOLYGON) {
            rasterizepolygon(pGrid);
        } else {
            throw new ModelsIllegalargumentException("Couldn't recognize the geometry type of the file.", this.getClass()
                    .getSimpleName());
        }

        outRaster = CoverageUtilities.buildCoverage("rasterized", outWR, paramsMap, inVector.getSchema()
                .getCoordinateReferenceSystem());

    }
    private void rasterizepolygon( final GridGeometry2D gridGeometry ) throws InvalidGridGeometryException, TransformException {

        int size = inVector.size();
        pm.beginTask("Rasterizing features...", size);
        FeatureIterator<SimpleFeature> featureIterator = inVector.features();

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(pMaxThreads);

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
                                                    outWR.setSample(k, r, 0, value);
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
