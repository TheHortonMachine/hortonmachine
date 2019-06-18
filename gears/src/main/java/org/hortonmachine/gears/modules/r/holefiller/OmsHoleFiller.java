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
package org.hortonmachine.gears.modules.r.holefiller;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.Variables.IDW;
import static org.hortonmachine.gears.libs.modules.Variables.TPS;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_LABEL;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_LICENSE;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_NAME;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_STATUS;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.interpolation2d.core.IDWInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.ISurfaceInterpolator;
import org.hortonmachine.gears.modules.r.interpolation2d.core.TPSInterpolator;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

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
import oms3.annotations.Unit;

@Description(OMSHOLEFILLER_DESCRIPTION)
@Documentation(OMSHOLEFILLER_DOCUMENTATION)
@Author(name = OMSHOLEFILLER_AUTHORNAMES, contact = OMSHOLEFILLER_AUTHORCONTACTS)
@Keywords(OMSHOLEFILLER_KEYWORDS)
@Label(OMSHOLEFILLER_LABEL)
@Name(OMSHOLEFILLER_NAME)
@Status(OMSHOLEFILLER_STATUS)
@License(OMSHOLEFILLER_LICENSE)
public class OmsHoleFiller extends HMModel {

    @Description(OMSHOLEFILLER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSHOLEFILLER_IN_ROI_DESCRIPTION)
    @In
    public SimpleFeatureCollection inROI;

    @Description(OMSHOLEFILLER_MODE_DESCRIPTION)
    @UI("combo:" + TPS + "," + IDW)
    @In
    public String pMode = TPS;

    @Description(OMSHOLEFILLER_P_BUFFER_DESCRIPTION)
    @Unit("m")
    @In
    public double pBuffer = 4.0;

    @Description(OMSHOLEFILLER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSHOLEFILLER_DESCRIPTION = "Module that fills raster holes using interpolation.";
    public static final String OMSHOLEFILLER_DOCUMENTATION = "";
    public static final String OMSHOLEFILLER_KEYWORDS = "Holefiller, Raster";
    public static final String OMSHOLEFILLER_LABEL = RASTERPROCESSING;
    public static final String OMSHOLEFILLER_NAME = "holefiller";
    public static final int OMSHOLEFILLER_STATUS = 40;
    public static final String OMSHOLEFILLER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSHOLEFILLER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSHOLEFILLER_AUTHORCONTACTS = "http://www.hydrologis.com";

    public static final String OMSHOLEFILLER_IN_RASTER_DESCRIPTION = "The raster to fill holes in.";
    public static final String OMSHOLEFILLER_MODE_DESCRIPTION = "The interpolation mode to use.";
    public static final String OMSHOLEFILLER_P_BUFFER_DESCRIPTION = "The buffer to use for interpolation.";
    public static final String OMSHOLEFILLER_IN_ROI_DESCRIPTION = "The regions vector map on which to checkc for nulls.";

    public static final String OMSHOLEFILLER_OUT_RASTER_DESCRIPTION = "The new raster.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        ISurfaceInterpolator interpolator;
        if (pMode.equals(IDW)) {
            interpolator = new IDWInterpolator(pBuffer);
        } else {
            interpolator = new TPSInterpolator(pBuffer);
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int rows = regionMap.getRows();
        int cols = regionMap.getCols();

        WritableRaster outWR = CoverageUtilities.renderedImage2WritableRaster(inRaster.getRenderedImage(), false);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);

        GridGeometry2D gridGeometry = inRaster.getGridGeometry();

        PreparedGeometry preparedRoi = null;
        if (inROI != null) {
            List<Geometry> roiList = FeatureUtilities.featureCollectionToGeometriesList(inROI, false, null);
            GeometryCollection gc = new GeometryCollection(roiList.toArray(GeometryUtilities.TYPE_GEOMETRY), gf);
            preparedRoi = PreparedGeometryFactory.prepare(gc);
        }
        pm.beginTask("Filling holes...", cols - 2);
        for( int r = 1; r < rows - 1; r++ ) {
            for( int c = 1; c < cols - 1; c++ ) {
                if (pm.isCanceled()) {
                    return;
                }

                double value = outIter.getSampleDouble(c, r, 0);
                if (isNovalue(value)) {
                    DirectPosition worldPosition = gridGeometry.gridToWorld(new GridCoordinates2D(c, r));
                    double[] coordinate = worldPosition.getCoordinate();
                    Coordinate pointCoordinate = new Coordinate(coordinate[0], coordinate[1]);
                    Point point = gf.createPoint(pointCoordinate);
                    if (preparedRoi == null || preparedRoi.intersects(point)) {

                        // TODO this could be done considering more points and more far away points.
                        // For now, this works.
                        List<Coordinate> surroundingValids = getValidSurroundingPoints(outIter, gridGeometry, c, r);
                        if (surroundingValids.size() > 3) {
                            double newValue = interpolator.getValue(surroundingValids.toArray(new Coordinate[0]),
                                    pointCoordinate);
                            outIter.setSample(c, r, 0, newValue);
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        outIter.done();

        outRaster = CoverageUtilities.buildCoverage("nulled", outWR, regionMap, inRaster.getCoordinateReferenceSystem());
    }

    private List<Coordinate> getValidSurroundingPoints( WritableRandomIter outIter, GridGeometry2D gridGeometry, int c, int r )
            throws TransformException {
        List<Coordinate> coords = new ArrayList<>();
        for( int dc = -1; dc <= 1; dc++ ) {
            for( int dr = -1; dr <= 1; dr++ ) {
                if (dc == 0 && dr == 0) {
                    continue;
                }
                double value = outIter.getSampleDouble(c + dc, r + dr, 0);
                if (!isNovalue(value)) {
                    DirectPosition worldPosition = gridGeometry.gridToWorld(new GridCoordinates2D(c + dc, r + dr));
                    double[] coordinate = worldPosition.getCoordinate();
                    Coordinate pointCoordinate = new Coordinate(coordinate[0], coordinate[1]);
                    pointCoordinate.z = value;
                    coords.add(pointCoordinate);
                }

            }
        }
        return coords;
    }

}
