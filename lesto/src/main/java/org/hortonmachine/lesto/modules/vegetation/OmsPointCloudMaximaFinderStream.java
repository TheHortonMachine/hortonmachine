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
package org.hortonmachine.lesto.modules.vegetation;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.grassraster.JGrassConstants;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
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
public class OmsPointCloudMaximaFinderStream extends HMModel {

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
    public static final String LABEL = HMConstants.LESTO + "/vegetation";
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

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);

        pm.beginTask("Distribute maximum values on grid...", inLas.size());
        for( LasRecord dot : inLas ) {
            DirectPosition wPoint = new DirectPosition2D(dot.x, dot.y);
            if (!aoi.contains(wPoint)) {
                continue;
            }

            GridCoordinates2D gridCoord = gridGeometry.worldToGrid(wPoint);
            int x = gridCoord.x;
            int y = gridCoord.y;

            double value = outWR.getSampleDouble(x, y, 0);
            if (HMConstants.isNovalue(value)) {
                value = dot.z;
            } else {
                value = Math.max(dot.z, value);
            }

            outWR.setSample(x, y, 0, value);

            pm.worked(1);
        }
        pm.done();

        int rowPerRadius = (int) Math.ceil(pMaxRadius / pBaseGridResolution);

        pm.beginTask("Grow maxima regions...", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double centerValue = outWR.getSampleDouble(c, r, 0);
                if (HMConstants.isNovalue(centerValue)) {
                    continue;
                }
                // moving window
                for( int wc = c - rowPerRadius; wc <= c + rowPerRadius; wc++ ) {
                    for( int wr = r - rowPerRadius; wr <= r + rowPerRadius; wr++ ) {
                        if (wr == r && wc == c) {
                            continue;
                        }
                        if (wr < 0 || wr >= rows) {
                            continue;
                        }
                        if (wc < 0 || wc >= cols) {
                            continue;
                        }
                        double wValue = outWR.getSampleDouble(wc, wr, 0);
                        if (HMConstants.isNovalue(wValue)) {
                            continue;
                        }

                        double newValue = Math.max(centerValue, wValue);
                        outWR.setSample(wc, wr, 0, newValue);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(gridGeometry);
        outCoverage = CoverageUtilities.buildCoverage("tops", outWR, regionMap, crs);

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
