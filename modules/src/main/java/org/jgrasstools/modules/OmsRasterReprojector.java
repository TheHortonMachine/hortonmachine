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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_pCode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_pCols_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_pInterpolation_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_pRows_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_pSouth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_pWest_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.Variables.BICUBIC;
import static org.jgrasstools.gears.libs.modules.Variables.BILINEAR;
import static org.jgrasstools.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;

import javax.media.jai.Interpolation;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OMSRASTERREPROJECTOR_DESCRIPTION)
@Documentation(OMSRASTERREPROJECTOR_DOCUMENTATION)
@Author(name = OMSRASTERREPROJECTOR_AUTHORNAMES, contact = OMSRASTERREPROJECTOR_AUTHORCONTACTS)
@Keywords(OMSRASTERREPROJECTOR_KEYWORDS)
@Label(OMSRASTERREPROJECTOR_LABEL)
@Name("_" + OMSRASTERREPROJECTOR_NAME)
@Status(OMSRASTERREPROJECTOR_STATUS)
@License(OMSRASTERREPROJECTOR_LICENSE)
public class OmsRasterReprojector extends JGTModel {

    @Description(OMSRASTERREPROJECTOR_inRaster_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERREPROJECTOR_pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSRASTERREPROJECTOR_pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSRASTERREPROJECTOR_pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSRASTERREPROJECTOR_pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSRASTERREPROJECTOR_pRows_DESCRIPTION)
    @UI(JGTConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSRASTERREPROJECTOR_pCols_DESCRIPTION)
    @UI(JGTConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSRASTERREPROJECTOR_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSRASTERREPROJECTOR_pInterpolation_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSRASTERREPROJECTOR_outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public GridCoverage2D outRaster = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        CoordinateReferenceSystem targetCrs = CRS.decode(pCode);

        Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        if (pInterpolation.equals(BILINEAR)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        } else if (pInterpolation.equals(BICUBIC)) {
            interpolation = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
        }

        GridGeometry2D gridGeometry = null;
        if (pNorth != null && pSouth != null && pWest != null && pEast != null && pRows != null && pCols != null) {
            gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(pNorth, pSouth, pEast, pWest, pCols, pRows, targetCrs);
            pm.message("Using supplied gridgeometry: " + gridGeometry);
        }
        pm.beginTask("Reprojecting...", IJGTProgressMonitor.UNKNOWN);
        if (gridGeometry == null) {
            outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs, null, interpolation);
        } else {
            outRaster = (GridCoverage2D) Operations.DEFAULT.resample(inRaster, targetCrs, gridGeometry, interpolation);
        }
        pm.done();

    }

}
