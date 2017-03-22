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
package org.jgrasstools.lesto.modules.vegetation;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

@Description(OmsPointCloudMaximaFinderStream.DESCR)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsPointCloudMaximaFinderStream.KEYWORDS)
@Label(OmsPointCloudMaximaFinderStream.LABEL)
@Name("_" + OmsPointCloudMaximaFinderStream.NAME + "_stream")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class OmsPointCloudMaximaFinderStream extends JGTModel {

    @Description(inLas_DESCR)
    @In
    public List<LasRecord> inLas = null;

    @In
    @Description("The requested are aof interest bounds.")
    public ReferencedEnvelope aoi;

    @Description("The base grid resolution.")
    @In
    public double pBaseGridResolution = 0.5;

    @Description(pMaxRadius_DESCR)
    @In
    public double pMaxRadius = -1.0;

    @Description(outTops_DESCR)
    @In
    public GridCoverage2D outCoverage = null;

    // @Description(outTops_DESCR)
    // @In
    // public SimpleFeatureCollection outTops = null;

    // VARS DOCS START
    public static final String outTops_DESCR = "The output local maxima.";
    public static final String pClass_DESCR = "The comma separated list of classes to filter (if empty, all are picked).";
    public static final String pThreshold_DESCR = "The elevation threshold to apply to the chm.";
    public static final String pElevDiffThres_DESCR = "Max permitted elevation difference around the maxima.";
    public static final String doDynamicRadius_DESCR = "Use an adaptive radius based on the height.";
    public static final String pMaxRadius_DESCR = "Radius for which a point can be local maxima.";
    public static final String inDsmDtmDiff_DESCR = "An optional dsm-dtm difference raster to use to check on the extracted tops.";
    public static final String inRoi_DESCR = "A set of polygons to use as region of interest.";
    public static final String inDtm_DESCR = "A dtm raster to use for the area of interest and to calculate the elevation threshold.";
    public static final String inLas_DESCR = "The input las.";
    public static final String NAME = "pointcloudmaximafinder";
    public static final String KEYWORDS = "Local maxima, las, lidar";
    public static final String DESCR = "Module that identifies local maxima in point clouds.";
    public static final String LABEL = JGTConstants.LESTO + "/vegetation";
    // VARS DOCS END

    @Execute
    public void process() throws Exception {
        checkNull(inLas, aoi);

        CoordinateReferenceSystem crs = aoi.getCoordinateReferenceSystem();

        double north = aoi.getMaxY();
        double south = aoi.getMinY();
        double east = aoi.getMaxX();
        double west = aoi.getMinX();

        int cols = (int) Math.round((east - west) / pBaseGridResolution);
        int rows = (int) Math.round((north - south) / pBaseGridResolution);
        GridGeometry2D gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west, cols, rows, crs);

        pm.beginTask("Distribute maximum values on grid...", inLas.size());
        double[][] maxValues = new double[cols][rows];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                maxValues[c][r] = JGTConstants.doubleNovalue;
            }
        }

        for( LasRecord dot : inLas ) {
            DirectPosition wPoint = new DirectPosition2D(dot.x, dot.y);
            if (!aoi.contains(wPoint)) {
                continue;
            }

            GridCoordinates2D gridCoord = gridGeometry.worldToGrid(wPoint);
            int x = gridCoord.x;
            int y = gridCoord.y;

            maxValues[x][y] = Math.max(dot.z, maxValues[x][y]);

            pm.worked(1);
        }
        pm.done();

        int rowPerRadius = (int) Math.ceil(pMaxRadius / pBaseGridResolution);

        pm.beginTask("Grow maxima regions...", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double centerValue = maxValues[c][r];
                if (JGTConstants.isNovalue(centerValue)) {
                    continue;
                }
                // moving window
                for( int wr = r - rowPerRadius; wr <= r + rowPerRadius; wr++ ) {
                    for( int wc = c - rowPerRadius; wc <= c + rowPerRadius; wc++ ) {
                        if (wr < 0 || wr >= rows || wr == r) {
                            continue;
                        }
                        if (wc < 0 || wc >= cols || wc == c) {
                            continue;
                        }
                        double wValue = maxValues[wc][wr];
                        if (JGTConstants.isNovalue(wValue)) {
                            continue;
                        }
                        maxValues[wc][wr] = Math.max(centerValue, wValue);
                        if (JGTConstants.isNovalue(maxValues[wc][wr])) {
                            System.out.println();
                        }
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(gridGeometry);
        outCoverage = CoverageUtilities.buildCoverage("tops", maxValues, regionMap, crs, false);

    }

    public static void main( String[] args ) throws Exception {
        String lasPath = "/media/hydrologis/LESTOPLUS/test_lidar_spatialite/las_aurina/uni_bz_44.las";
        String outPath = "/home/hydrologis/TMP/tops_2m.asc";
        OmsPointCloudMaximaFinderStream s = new OmsPointCloudMaximaFinderStream();

        List<LasRecord> dotList = new ArrayList<>();
        try (ALasReader reader = ALasReader.getReader(new File(lasPath), null)) {
            reader.open();
            while( reader.hasNextPoint() ) {
                LasRecord dot = (LasRecord) reader.getNextPoint();
                dotList.add(dot);
            }
            s.inLas = dotList;
            ReferencedEnvelope3D dataEnvelope = reader.getHeader().getDataEnvelope();
            s.aoi = new ReferencedEnvelope(dataEnvelope);
            s.pBaseGridResolution = 0.5;
            s.pMaxRadius = 2;
            s.process();

            OmsRasterWriter.writeRaster(outPath, s.outCoverage);
        }

    }

}
