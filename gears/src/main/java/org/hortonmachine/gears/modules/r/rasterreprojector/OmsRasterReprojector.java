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

import org.eclipse.imagen.Interpolation;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessingException;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Envelope;

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

    public static final String OMSRASTERREPROJECTOR_DESCRIPTION = "Reprojects a raster into another coordinate reference system, optionally with a new cell size.";
    public static final String OMSRASTERREPROJECTOR_DOCUMENTATION = """
            Without cell size the cells are chosen by the resampling, which keeps about the number of cells of the source.

            With a cell size the output has cells of exactly that size, and its bounds are on multiples of it, as with the -tap option of gdalwarp: rasters reprojected with the same cell size share the same grid and line up cell by cell. The bounds cover the whole source, found from its border densified, since straight edges become curves in another projection; the cells outside the source are novalue.
            """;
    public static final String OMSRASTERREPROJECTOR_KEYWORDS = "Raster, Reprojection, CRS";
    public static final String OMSRASTERREPROJECTOR_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERREPROJECTOR_NAME = "rreproject";
    public static final int OMSRASTERREPROJECTOR_STATUS = 40;
    public static final String OMSRASTERREPROJECTOR_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERREPROJECTOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERREPROJECTOR_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERREPROJECTOR_IN_RASTER_DESCRIPTION = "The raster to reproject.";
    public static final String OMSRASTERREPROJECTOR_P_XRES_DESCRIPTION = "The optional cell size in x of the output.";
    public static final String OMSRASTERREPROJECTOR_P_YRES_DESCRIPTION = "The optional cell size in y of the output.";
    public static final String OMSRASTERREPROJECTOR_P_OUTCODE_DESCRIPTION = "The code of the target coordinate reference system, as EPSG:32632.";
    public static final String OMSRASTERREPROJECTOR_P_INTERPOLATION_DESCRIPTION = "The interpolation of the new cells: nearest neighbour keeps the original values, as needed for classes and codes; bilinear and bicubic smooth them.";
    public static final String OMSRASTERREPROJECTOR_OUT_RASTER_DESCRIPTION = "The reprojected raster.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs(pCode);

        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        if (pInterpolation.equals(BILINEAR)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        } else if (pInterpolation.equals(BICUBIC)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
        }
        CoordinateReferenceSystem sourceCrs = inRaster.getCoordinateReferenceSystem();
//        if (!CrsUtilities.isCrsValid(sourceCrs)) {
//            throw new ModelsIllegalargumentException("Source projection not supported.", this);
//        }

        GridGeometry2D gridGeometry = null;
        if (pXres != null && pYres != null) {
            gridGeometry = alignedGridGeometry(new Envelope(inRaster.getEnvelope2D()), sourceCrs, targetCrs, pXres, pYres);
            pm.message("Using supplied gridgeometry: " + gridGeometry);
        }
        pm.beginTask("Reprojecting...", IHMProgressMonitor.UNKNOWN);

        try {
            if (gridGeometry == null) {
                if (interpolation == null) {
                    outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs);
                } else {
                    outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs, null, interpolation);
                }
            } else {
                outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs, gridGeometry, interpolation);
            }
        } catch (CoverageProcessingException e) {
            
            
            CoverageProcessor processor = new CoverageProcessor();
            ParameterValueGroup params = processor.getOperation("Resample").getParameters();
            params.parameter("Source").setValue(inRaster);
//                params.parameter("Source").value = this.coverage
            params.parameter("CoordinateReferenceSystem").setValue(targetCrs);
            outRaster = (GridCoverage2D) processor.doOperation(params);
        }

        pm.done();

    }

    /**
     * The grid of the reprojected raster for a given cell size: it covers the whole source, whose border is densified
     * since its edges are curved in the target crs, with cells of exactly the given size and the bounds on multiples of
     * it.
     *
     * @param sourceEnvelope the envelope of the source raster.
     * @param sourceCrs the crs of the source.
     * @param targetCrs the target crs.
     * @param xRes the cell size in x.
     * @param yRes the cell size in y.
     * @return the grid geometry.
     * @throws Exception if the envelope can't be transformed.
     */
    static GridGeometry2D alignedGridGeometry( Envelope sourceEnvelope, CoordinateReferenceSystem sourceCrs,
            CoordinateReferenceSystem targetCrs, double xRes, double yRes ) throws Exception {
        HMCrsTransformer transformer = new HMCrsTransformer(sourceCrs, targetCrs);
        Envelope env = transformer.transformDensified(sourceEnvelope, DENSIFY_POINTS);
        // a tolerance, so that bounds already on the grid are not moved by a cell for rounding errors
        double tolerance = 1E-6;
        double w = Math.floor(env.getMinX() / xRes + tolerance) * xRes;
        double e = Math.ceil(env.getMaxX() / xRes - tolerance) * xRes;
        double s = Math.floor(env.getMinY() / yRes + tolerance) * yRes;
        double n = Math.ceil(env.getMaxY() / yRes - tolerance) * yRes;
        int cols = (int) Math.round((e - w) / xRes);
        int rows = (int) Math.round((n - s) / yRes);
        return CoverageUtilities.gridGeometryFromRegionValues(n, s, e, w, cols, rows, targetCrs);
    }

    /** The number of points per side of the source envelope used to find its bounds in the target crs. */
    private static final int DENSIFY_POINTS = 100;
}
