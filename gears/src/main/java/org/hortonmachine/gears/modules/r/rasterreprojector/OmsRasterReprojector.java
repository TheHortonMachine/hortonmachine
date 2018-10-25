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
package org.hortonmachine.gears.modules.r.rasterreprojector;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.BICUBIC;
import static org.hortonmachine.gears.libs.modules.Variables.BILINEAR;
import static org.hortonmachine.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_LABEL;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_LICENSE;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_NAME;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_STATUS;

import javax.media.jai.Interpolation;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

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

@Description(OMSRASTERREPROJECTOR_DESCRIPTION)
@Documentation(OMSRASTERREPROJECTOR_DOCUMENTATION)
@Author(name = OMSRASTERREPROJECTOR_AUTHORNAMES, contact = OMSRASTERREPROJECTOR_AUTHORCONTACTS)
@Keywords(OMSRASTERREPROJECTOR_KEYWORDS)
@Label(OMSRASTERREPROJECTOR_LABEL)
@Name(OMSRASTERREPROJECTOR_NAME)
@Status(OMSRASTERREPROJECTOR_STATUS)
@License(OMSRASTERREPROJECTOR_LICENSE)
public class OmsRasterReprojector extends HMModel {

    @Description(OMSRASTERREPROJECTOR_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERREPROJECTOR_P_XRES_DESCRIPTION)
    @UI(HMConstants.PROCESS_COLS_UI_HINT)
    @In
    public Double pXres = null;

    @Description(OMSRASTERREPROJECTOR_P_YRES_DESCRIPTION)
    @UI(HMConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Double pYres = null;

    @Description(OMSRASTERREPROJECTOR_P_OUTCODE_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSRASTERREPROJECTOR_P_INTERPOLATION_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSRASTERREPROJECTOR_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    public static final String OMSRASTERREPROJECTOR_DESCRIPTION = "Module for raster reprojection.";
    public static final String OMSRASTERREPROJECTOR_DOCUMENTATION = "";
    public static final String OMSRASTERREPROJECTOR_KEYWORDS = "Crs, Reprojection, Raster, OmsRasterConverter, OmsRasterReader";
    public static final String OMSRASTERREPROJECTOR_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERREPROJECTOR_NAME = "rreproject";
    public static final int OMSRASTERREPROJECTOR_STATUS = 40;
    public static final String OMSRASTERREPROJECTOR_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERREPROJECTOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERREPROJECTOR_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERREPROJECTOR_IN_RASTER_DESCRIPTION = "The raster that has to be reprojected.";
    public static final String OMSRASTERREPROJECTOR_P_XRES_DESCRIPTION = "The optional X resolution to set for the output.";
    public static final String OMSRASTERREPROJECTOR_P_YRES_DESCRIPTION = "The optional Y resolution to set for the output.";
    public static final String OMSRASTERREPROJECTOR_P_OUTCODE_DESCRIPTION = "The projection code for the target coordinate reference system (ex. EPSG:32632).";
    public static final String OMSRASTERREPROJECTOR_P_INTERPOLATION_DESCRIPTION = "The interpolation type to use";
    public static final String OMSRASTERREPROJECTOR_OUT_RASTER_DESCRIPTION = "The reprojected output raster.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        CoordinateReferenceSystem targetCrs = CrsUtilities.getCrsFromEpsg(pCode, null);

        Interpolation interpolation = null;
        if (pInterpolation.equals(BILINEAR)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        } else if (pInterpolation.equals(BICUBIC)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
        }
        CoordinateReferenceSystem sourceCrs = inRaster.getCoordinateReferenceSystem();
        if (!CrsUtilities.isCrsValid(sourceCrs)) {
            return;
        }

        GridGeometry2D gridGeometry = null;
        if (pXres != null && pYres != null) {
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
            double n = regionMap.getNorth();
            double s = regionMap.getSouth();
            double e = regionMap.getEast();
            double w = regionMap.getWest();
            Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(new Envelope(w, e, s, n));


            MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs);
            Geometry targetGeometry = JTS.transform(polygon, transform);
            Envelope env = targetGeometry.getEnvelopeInternal();
            n = env.getMaxY();
            s = env.getMinY();
            w = env.getMinX();
            e = env.getMaxX();

            int newCols = (int) Math.round((e - w) / pXres);
            int newRows = (int) Math.round((n - s) / pYres);

            gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(n, s, e, w, newCols, newRows, targetCrs);
            pm.message("Using supplied gridgeometry: " + gridGeometry);
        }
        pm.beginTask("Reprojecting...", IHMProgressMonitor.UNKNOWN);

        if (gridGeometry == null) {
            if (interpolation == null) {
                outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs);
            } else {
                outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs, null, interpolation);
            }
        } else {
            outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs, gridGeometry, interpolation);
        }
        pm.done();

    }
}
