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

import static java.lang.Math.round;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.Point;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.List;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Description("Creates a raster with the highest, or lowest, elevation of the lidar points in each cell: a surface model without interpolation.")
@Documentation(LasOnRasterMapper.DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("raster, lidar")
@Label(HMConstants.LESTO + "/raster")
@Name("lasonrastermapper")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class LasOnRasterMapper extends HMModel {

    @Description("The lidar data: a las file, the index file of a folder of las files created by LasIndexer or a las database.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas = null;

    @Description("The elevation model of the ground: it gives the area and the grid, and the lowest value of the cells.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm;

    @Description("The x resolution of the raster. If not supplied, the one of the elevation model is used.")
    @In
    public Double pXres;

    @Description("The y resolution of the raster. If not supplied, the one of the elevation model is used.")
    @In
    public Double pYres;

    @Description("If true, the highest point of each cell is mapped, if false the lowest.")
    @In
    public boolean doMax = true;

    @Description("The raster of the elevation of the points.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Description("The optional output of the elevation model on the grid of the raster, to compare them cell by cell.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDtm = null;

    public static final String DOCUMENTATION = """
            Each cell gets the elevation of its highest point, or lowest, but never less than the elevation model under the point: a point under the ground, as an error of the survey, takes the elevation of the ground. The cells without points stay empty: choose a resolution with a few points per cell, or fill the holes afterwards.

            With the highest points, the raster is a model of the surface, of the canopy and the roofs; minus the elevation model, a model of their height. The elevation model can be written also on the grid of the raster, resampled with the value of the nearest cell, for that difference.
            """;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, inDtm, outRaster);

        GridCoverage2D inDtmGC = getRaster(inDtm);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDtmGC);
        double north = regionMap.getNorth();
        double south = regionMap.getSouth();
        double east = regionMap.getEast();
        double west = regionMap.getWest();
        if (pXres == null || pYres == null) {
            pXres = regionMap.getXres();
            pYres = regionMap.getYres();
        }

        CoordinateReferenceSystem crs = null;
        Polygon polygon = CoverageUtilities.getRegionPolygon(inDtmGC);
        crs = inDtmGC.getCoordinateReferenceSystem();
        GridGeometry2D dtmGridGeometry = inDtmGC.getGridGeometry();

        final int newRows = (int) round((north - south) / pYres);
        int newCols = (int) round((east - west) / pXres);

        final GridGeometry2D newGridGeometry2D = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west, newCols,
                newRows, crs);
        RegionMap newRegionMap = CoverageUtilities.gridGeometry2RegionParamsMap(newGridGeometry2D);
        final WritableRaster newWR = CoverageUtilities.createWritableRaster(newCols, newRows, null, null,
                HMConstants.doubleNovalue);

        RandomIter dtmIter = CoverageUtilities.getRandomIterator(inDtmGC);

        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inLas), null, 0.0, crs)) {
            lasData.open();
            pm.beginTask("Reading points on region...", IHMProgressMonitor.UNKNOWN);
            List<LasRecord> lasPoints = lasData.getPointsInGeometry(polygon, false);
            pm.done();

            pm.beginTask("Setting raster points...", lasPoints.size());
            final Point gridPoint = new Point();
            final Point dtmPoint = new Point();
            for( LasRecord lasRecord : lasPoints ) {
                double dotZ = lasRecord.z;
                Coordinate coordinate = new Coordinate(lasRecord.x, lasRecord.y, dotZ);
                CoverageUtilities.colRowFromCoordinate(coordinate, newGridGeometry2D, gridPoint);

                double newRasterValue = newWR.getSampleDouble(gridPoint.x, gridPoint.y, 0);

                CoverageUtilities.colRowFromCoordinate(coordinate, dtmGridGeometry, dtmPoint);
                double dtmValue = dtmIter.getSampleDouble(dtmPoint.x, dtmPoint.y, 0);

                if (doMax) {
                    if (HMConstants.isNovalue(newRasterValue) || newRasterValue < dotZ) {
                        if (!HMConstants.isNovalue(dtmValue) && dtmValue > dotZ) {
                            dotZ = dtmValue;
                        }
                        newWR.setSample(gridPoint.x, gridPoint.y, 0, dotZ);
                    }
                } else {
                    if (HMConstants.isNovalue(newRasterValue) || newRasterValue > dotZ) {
                        if (!HMConstants.isNovalue(dtmValue) && dtmValue > dotZ) {
                            dotZ = dtmValue;
                        }
                        newWR.setSample(gridPoint.x, gridPoint.y, 0, dotZ);
                    }
                }
                pm.worked(1);
            }
            pm.done();
        }

        GridCoverage2D outRasterGC = CoverageUtilities.buildCoverage("outraster", newWR, newRegionMap, crs);
        dumpRaster(outRasterGC, outRaster);

        if (pXres != null && pYres != null && outDtm != null) {
            WritableRaster[] holder = new WritableRaster[1];
            GridCoverage2D outDtmGC = CoverageUtilities.createCoverageFromTemplate(outRasterGC, HMConstants.doubleNovalue,
                    holder);

            GridGeometry2D outGridGeometry = outDtmGC.getGridGeometry();

            WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(holder[0]);

            RegionMap outRegionMap = CoverageUtilities.getRegionParamsFromGridCoverage(outDtmGC);
            int cols = outRegionMap.getCols();
            int rows = outRegionMap.getRows();

            final Point p = new Point();
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    Coordinate coordinate = CoverageUtilities.coordinateFromColRow(c, r, outGridGeometry);
                    CoverageUtilities.colRowFromCoordinate(coordinate, dtmGridGeometry, p);
                    double dtmValue = dtmIter.getSampleDouble(p.x, p.y, 0);
                    outIter.setSample(c, r, 0, dtmValue);
                }
            }
            outIter.done();

            dumpRaster(outDtmGC, outDtm);
        }
        dtmIter.done();

    }

}
