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
package org.jgrasstools.gears.modules.r.rasterreprojector;

import javax.media.jai.Interpolation;

import oms3.annotations.Author;
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
@Description("Module for raster reprojection.")
@Documentation("RasterConverter.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Crs, Reprojection, Raster, RasterConverter, RasterReader")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.CERTIFIED)
@Name("rreproject")
@License("General Public License Version 3 (GPLv3)")
public class RasterReprojector extends JGTModel {

    @Description("The raster that has to be reprojected.")
    @In
    public GridCoverage2D inRaster;

    @Description("The north bound of the region to consider")
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description("The south bound of the region to consider")
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description("The west bound of the region to consider")
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description("The east bound of the region to consider")
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description("The rows of the region to consider")
    @UI(JGTConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description("The cols of the region to consider")
    @UI(JGTConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description("The code defining the target coordinate reference system, composed by authority and code number (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The interpolation type to use: nearest neightbour (0), bilinear (1), bicubic (2)")
    @In
    public int pInterpolation = 0;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The reprojected output raster.")
    @Out
    public GridCoverage2D outRaster = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        CoordinateReferenceSystem targetCrs = CRS.decode(pCode);

        Interpolation interpolationType = null;
        if (pInterpolation == 1) {
            interpolationType = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        } else if (pInterpolation == 2) {
            interpolationType = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
        } else if (pInterpolation == 3) {
            interpolationType = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
        } else {
            // default to nearest neighbour
            interpolationType = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        }

        GridGeometry2D gridGeometry = null;
        if (pNorth != null && pSouth != null && pWest != null && pEast != null && pRows != null && pCols != null) {
            gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(pNorth, pSouth, pEast, pWest, pCols, pRows, targetCrs);
            pm.message("Using supplied gridgeometry: " + gridGeometry);
        }
        pm.beginTask("Reprojecting...", IJGTProgressMonitor.UNKNOWN);
        if (gridGeometry == null) {
            outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs, null, interpolationType);
        } else {
            outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs, gridGeometry, interpolationType);
        }
        pm.done();

    }

}
