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
package org.jgrasstools.gears.modules.r.bobthebuilder;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.utils.math.NumericsUtilities.isBetween;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.modules.r.interpolation2d.core.IDWInterpolator;
import org.jgrasstools.gears.modules.r.scanline.ScanLineRasterizer;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.coverage.ProfilePoint;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.geometry.DirectPosition;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

@Description("Builds rasterized artifacts on a raster.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Build, Raster")
@Name("bobbuilder")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class BobTheBuilder extends JGTModel {

    @Description("The input raster.")
    @In
    public GridCoverage2D inRaster = null;

    @Description("The vector map containing the polygonal area to modify.")
    @In
    public SimpleFeatureCollection inArea = null;

    @Description("The vector map containing the points that provide the new elevations.")
    @In
    public SimpleFeatureCollection inElevations = null;

    @Description("The maximum radius to use for interpolation.")
    @In
    public double pMaxbuffer = -1;

    @Description("The field of the elevations map that contain the elevation of the point.")
    @In
    public String fElevation = null;

    @Description("Switch that defines if the module should erode in places the actual raster is higher (default is false).")
    @In
    public boolean doErode = false;

    @Description("Switch that defines if the module should use only points contained in the polygon for the interpolation (default is false. i.e. use all).")
    @In
    public boolean doUseOnlyInternal = false;

    @Description("Switch that defines if the module should add the border of the polygon as elevation point to aid connection between new and old (default is false).")
    @In
    public boolean doPolygonborder = false;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The modified raster map.")
    @Out
    public GridCoverage2D outRaster = null;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, inArea, inElevations, fElevation);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double west = regionMap.getWest();
        double east = regionMap.getEast();
        double south = regionMap.getSouth();
        double north = regionMap.getNorth();

        ReferencedEnvelope vectorBounds = inArea.getBounds();
        if (!isBetween(vectorBounds.getMaxX(), west, east) || !isBetween(vectorBounds.getMinX(), west, east)
                || !isBetween(vectorBounds.getMaxY(), south, north) || !isBetween(vectorBounds.getMinY(), south, north)) {
            throw new ModelsIllegalargumentException("The vector map has to be within the raster map boundaries.", this);
        }

        List<FeatureMate> polygonMates = FeatureUtilities.featureCollectionToMatesList(inArea);
        String polygonMessage = "This operation can be applied only to a single polygon.";
        if (polygonMates.size() != 1) {
            throw new ModelsIllegalargumentException(polygonMessage, this);
        }
        FeatureMate polygonMate = polygonMates.get(0);
        Geometry polygon = polygonMate.getGeometry();
        if (polygon instanceof MultiPolygon) {
            polygon = polygon.getGeometryN(0);
        }
        if (!(polygon instanceof Polygon)) {
            throw new ModelsIllegalargumentException(polygonMessage, this);
        }

        List<FeatureMate> pointsMates = FeatureUtilities.featureCollectionToMatesList(inElevations);
        if (doUseOnlyInternal) {
            PreparedGeometry preparedPolygon = PreparedGeometryFactory.prepare(polygon);
            List<FeatureMate> tmpPointsMates = new ArrayList<FeatureMate>();
            for( FeatureMate pointMate : pointsMates ) {
                Geometry geometry = pointMate.getGeometry();
                if (preparedPolygon.covers(geometry)) {
                    tmpPointsMates.add(pointMate);
                }
            }
            pointsMates = tmpPointsMates;
        }
        if (pointsMates.size() < 4) {
            throw new ModelsIllegalargumentException(
                    "You need at least 4 elevation points (the more, the better) to gain a decent interpolation.", this);
        }

        List<Coordinate> controlPointsList = new ArrayList<Coordinate>();
        if (doPolygonborder) {
            pm.beginTask("Extract polygon border...", IJGTProgressMonitor.UNKNOWN);
            Coordinate[] polygonCoordinates = polygon.getCoordinates();
            List<ProfilePoint> profile = CoverageUtilities.doProfile(inRaster, polygonCoordinates);
            for( ProfilePoint profilePoint : profile ) {
                Coordinate position = profilePoint.getPosition();
                double elevation = profilePoint.getElevation();
                Coordinate coord = new Coordinate(position.x, position.y, elevation);
                controlPointsList.add(coord);
            }
            pm.done();
        }

        for( FeatureMate pointsMate : pointsMates ) {
            Coordinate coordinate = pointsMate.getGeometry().getCoordinate();
            double elev = pointsMate.getAttribute(fElevation, Double.class);
            Coordinate coord = new Coordinate(coordinate.x, coordinate.y, elev);
            controlPointsList.add(coord);
        }

        Coordinate[] controlPoints = controlPointsList.toArray(new Coordinate[0]);

        GridGeometry2D gridGeometry = inRaster.getGridGeometry();
        RandomIter elevIter = CoverageUtilities.getRandomIterator(inRaster);

        WritableRaster outputWR = CoverageUtilities
                .createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter outputIter = RandomIterFactory.createWritable(outputWR, null);

        SimpleFeatureCollection newCollection = FeatureCollections.newCollection();
        newCollection.add(polygonMate.getFeature());
        ScanLineRasterizer slRasterizer = new ScanLineRasterizer();
        slRasterizer.pm = pm;
        slRasterizer.inVector = newCollection;
        slRasterizer.pCols = cols;
        slRasterizer.pRows = rows;
        slRasterizer.pNorth = north;
        slRasterizer.pSouth = south;
        slRasterizer.pEast = east;
        slRasterizer.pWest = west;
        slRasterizer.pValue = 1.0;
        slRasterizer.process();
        GridCoverage2D outRasterized = slRasterizer.outRaster;

        if (pMaxbuffer < 0)
            pMaxbuffer = Math.max(vectorBounds.getWidth(), vectorBounds.getHeight());

        IDWInterpolator interpolator = new IDWInterpolator(pMaxbuffer);
        final GridCoordinates2D gridCoord = new GridCoordinates2D();
        RandomIter rasterizedIter = CoverageUtilities.getRandomIterator(outRasterized);
        pm.beginTask("Interpolating...", cols);
        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
                double probValue = rasterizedIter.getSampleDouble(c, r, 0);
                if (isNovalue(probValue)) {
                    continue;
                }
                gridCoord.setLocation(c, r);
                DirectPosition world = gridGeometry.gridToWorld(gridCoord);
                double[] coordinate = world.getCoordinate();
                double interpolated = interpolator.getValue(controlPoints, new Coordinate(coordinate[0], coordinate[1]));
                outputIter.setSample(c, r, 0, interpolated);
            }
            pm.worked(1);
        }
        pm.done();

        pm.beginTask("Merging with original raster...", cols);
        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
                double interpolatedValue = outputIter.getSampleDouble(c, r, 0);
                double rasterValue = elevIter.getSampleDouble(c, r, 0);
                if (isNovalue(interpolatedValue)) {
                    if (!isNovalue(rasterValue))
                        outputIter.setSample(c, r, 0, rasterValue);
                } else {
                    if (doErode) {
                        // any value generated is ok
                        outputIter.setSample(c, r, 0, interpolatedValue);
                    } else {
                        // only values higher than the raster are ok
                        if (!isNovalue(rasterValue)) {
                            if (rasterValue < interpolatedValue) {
                                outputIter.setSample(c, r, 0, interpolatedValue);
                            } else {
                                outputIter.setSample(c, r, 0, rasterValue);
                            }
                        } else {
                            outputIter.setSample(c, r, 0, interpolatedValue);
                        }

                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        outRaster = CoverageUtilities.buildCoverage("raster", outputWR, regionMap, inRaster.getCoordinateReferenceSystem());

    }
}
