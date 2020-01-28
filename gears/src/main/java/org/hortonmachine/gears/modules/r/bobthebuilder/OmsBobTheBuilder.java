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
package org.hortonmachine.gears.modules.r.bobthebuilder;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DO_ERODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DO_POLYGON_BORDER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_DO_USE_ONLY_INTERNAL_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_F_ELEVATION_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_IN_AREA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_IN_ELEVATIONS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_P_MAX_BUFFER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSBOBTHEBUILDER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.isBetween;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.modules.r.interpolation2d.core.IDWInterpolator;
import org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.coverage.ProfilePoint;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opengis.geometry.DirectPosition;

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

@Description(OMSBOBTHEBUILDER_DESCRIPTION)
@Documentation(OMSBOBTHEBUILDER_DOCUMENTATION)
@Author(name = OMSBOBTHEBUILDER_AUTHORNAMES, contact = OMSBOBTHEBUILDER_AUTHORCONTACTS)
@Keywords(OMSBOBTHEBUILDER_KEYWORDS)
@Label(OMSBOBTHEBUILDER_LABEL)
@Name(OMSBOBTHEBUILDER_NAME)
@Status(OMSBOBTHEBUILDER_STATUS)
@License(OMSBOBTHEBUILDER_LICENSE)
public class OmsBobTheBuilder extends HMModel {

    @Description(OMSBOBTHEBUILDER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster = null;

    @Description(OMSBOBTHEBUILDER_IN_AREA_DESCRIPTION)
    @In
    public SimpleFeatureCollection inArea = null;

    @Description(OMSBOBTHEBUILDER_IN_ELEVATIONS_DESCRIPTION)
    @In
    public SimpleFeatureCollection inElevations = null;

    @Description(OMSBOBTHEBUILDER_P_MAX_BUFFER_DESCRIPTION)
    @In
    public double pMaxbuffer = -1;

    @Description(OMSBOBTHEBUILDER_F_ELEVATION_DESCRIPTION)
    @In
    public String fElevation = null;

    @Description(OMSBOBTHEBUILDER_DO_ERODE_DESCRIPTION)
    @In
    public boolean doErode = false;

    @Description(OMSBOBTHEBUILDER_DO_USE_ONLY_INTERNAL_DESCRIPTION)
    @In
    public boolean doUseOnlyInternal = false;

    @Description(OMSBOBTHEBUILDER_DO_POLYGON_BORDER_DESCRIPTION)
    @In
    public boolean doPolygonborder = false;

    @Description(OMSBOBTHEBUILDER_OUT_RASTER_DESCRIPTION)
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
            throw new ModelsIllegalargumentException("The vector map has to be within the raster map boundaries.", this, pm);
        }

        List<FeatureMate> polygonMates = FeatureUtilities.featureCollectionToMatesList(inArea);
        String polygonMessage = "This operation can be applied only to a single polygon.";
        if (polygonMates.size() != 1) {
            throw new ModelsIllegalargumentException(polygonMessage, this, pm);
        }
        FeatureMate polygonMate = polygonMates.get(0);
        Geometry polygon = polygonMate.getGeometry();
        if (polygon instanceof MultiPolygon) {
            polygon = polygon.getGeometryN(0);
        }
        if (!(polygon instanceof Polygon)) {
            throw new ModelsIllegalargumentException(polygonMessage, this, pm);
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
                    "You need at least 4 elevation points (the more, the better) to gain a decent interpolation.", this, pm);
        }

        List<Coordinate> controlPointsList = new ArrayList<Coordinate>();
        if (doPolygonborder) {
            pm.beginTask("Extract polygon border...", IHMProgressMonitor.UNKNOWN);
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

        WritableRaster outputWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        WritableRandomIter outputIter = RandomIterFactory.createWritable(outputWR, null);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        newCollection.add(polygonMate.getFeature());
        OmsScanLineRasterizer slRasterizer = new OmsScanLineRasterizer();
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
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
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
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
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
