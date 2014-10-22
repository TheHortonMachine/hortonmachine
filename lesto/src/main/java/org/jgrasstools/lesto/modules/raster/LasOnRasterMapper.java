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

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.jgrasstools.gears.io.las.ALasDataManager;
import org.jgrasstools.gears.io.las.core.LasRecord;
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
@Label(JGTConstants.LESTO + "/raster")
@Name("lasonrastermapper")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class LasOnRasterMapper extends JGTModel {

    @Description("Las file path.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inLas = null;

    @Description("A dtm raster to use for the area of interest and lower threshold.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDtm;

    @Description("New x resolution (if null, the dtm is used).")
    @In
    public Double pXres;

    @Description("New y resolution (if null, the dtm is used).")
    @In
    public Double pYres;

    @Description("If true, the maxima are mapped else the minima.")
    @In
    public boolean doMax = true;

    @Description("The output raster.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Description("The input dtm, resampled on the new resolution.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outDtm = null;

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

        final GridGeometry2D newGridGeometry2D = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west,
                newCols, newRows, crs);
        RegionMap newRegionMap = CoverageUtilities.gridGeometry2RegionParamsMap(newGridGeometry2D);
        final WritableRaster newWR = CoverageUtilities.createDoubleWritableRaster(newCols, newRows, null, null,
                JGTConstants.doubleNovalue);

        RandomIter dtmIter = CoverageUtilities.getRandomIterator(inDtmGC);

        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inLas), null, 0.0, crs)) {
            lasData.open();
            pm.beginTask("Reading points on region...", IJGTProgressMonitor.UNKNOWN);
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
                    if (JGTConstants.isNovalue(newRasterValue) || newRasterValue < dotZ) {
                        if (!JGTConstants.isNovalue(dtmValue) && dtmValue > dotZ) {
                            dotZ = dtmValue;
                        }
                        newWR.setSample(gridPoint.x, gridPoint.y, 0, dotZ);
                    }
                } else {
                    if (JGTConstants.isNovalue(newRasterValue) || newRasterValue > dotZ) {
                        if (!JGTConstants.isNovalue(dtmValue) && dtmValue > dotZ) {
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
            GridCoverage2D outDtmGC = CoverageUtilities.createCoverageFromTemplate(outRasterGC, JGTConstants.doubleNovalue,
                    holder);

            GridGeometry2D outGridGeometry = outDtmGC.getGridGeometry();

            WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(holder[0]);

            RegionMap outRegionMap = CoverageUtilities.getRegionParamsFromGridCoverage(outDtmGC);
            int cols = outRegionMap.getCols();
            int rows = outRegionMap.getRows();

            final Point p = new Point();
            for( int c = 0; c < cols; c++ ) {
                for( int r = 0; r < rows; r++ ) {
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
