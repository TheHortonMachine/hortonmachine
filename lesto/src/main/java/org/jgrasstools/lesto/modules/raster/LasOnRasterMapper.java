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

import static java.lang.Math.round;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.Point;
import java.awt.image.WritableRaster;
import java.io.File;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.index.LasDataManager;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

@Description("Module that creates a raster by mapping maximum elevation las points.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("raster, lidar")
@Label(JGTConstants.LAS + "/raster")
@Name("lasonrastermapper")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class LasOnRasterMapper extends JGTModel {

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

    @Description("The output raster.")
    @Out
    public GridCoverage2D outRaster = null;

    @Execute
    public void process() throws Exception {
        checkNull(inIndexFile, inDtm);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDtm);
        double north = regionMap.getNorth();
        double south = regionMap.getSouth();
        double east = regionMap.getEast();
        double west = regionMap.getWest();
        if (pXres == null) {
            pXres = regionMap.getXres();
        }
        if (pYres == null) {
            pYres = regionMap.getYres();
        }

        CoordinateReferenceSystem crs = null;
        Polygon polygon = CoverageUtilities.getRegionPolygon(inDtm);
        crs = inDtm.getCoordinateReferenceSystem();

        final int newRows = (int) round((north - south) / pYres);
        int newCols = (int) round((east - west) / pXres);

        final GridGeometry2D newGridGeometry2D = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west,
                newCols, newRows, crs);
        RegionMap newRegionMap = CoverageUtilities.gridGeometry2RegionParamsMap(newGridGeometry2D);
        final WritableRaster newWR = CoverageUtilities.createDoubleWritableRaster(newCols, newRows, null, null,
                JGTConstants.doubleNovalue);

        try (LasDataManager lasData = new LasDataManager(new File(inIndexFile), null, 0.0, crs)) {
            lasData.open();
            pm.beginTask("Reading points on region...", IJGTProgressMonitor.UNKNOWN);
            List<LasRecord> lasPoints = lasData.getPointsInGeometry(polygon, false);
            pm.done();

            pm.beginTask("Setting raster points...", lasPoints.size());
            final Point gridPoint = new Point();
            for( LasRecord lasRecord : lasPoints ) {
                double z = lasRecord.z;
                Coordinate coordinate = new Coordinate(lasRecord.x, lasRecord.y, z);
                CoverageUtilities.colRowFromCoordinate(coordinate, newGridGeometry2D, gridPoint);

                double value = newWR.getSampleDouble(gridPoint.x, gridPoint.y, 0);
                if (JGTConstants.isNovalue(value) || value < z) {
                    newWR.setSample(gridPoint.x, gridPoint.y, 0, z);
                }
                pm.worked(1);
            }
            pm.done();
        }

        outRaster = CoverageUtilities.buildCoverage("outraster", newWR, newRegionMap, crs);

    }

}
