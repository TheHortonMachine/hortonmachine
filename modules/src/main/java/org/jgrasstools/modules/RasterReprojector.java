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
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_IN_RASTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_OUT_RASTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_P_CODE_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_P_COLS_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_P_EAST_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_P_INTERPOLATION_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_P_NORTH_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_P_ROWS_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_P_SOUTH_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERREPROJECTOR_P_WEST_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.Variables.BICUBIC;
import static org.jgrasstools.gears.libs.modules.Variables.BILINEAR;
import static org.jgrasstools.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.rasterreprojector.OmsRasterReprojector;

@Description(OMSRASTERREPROJECTOR_DESCRIPTION)
@Author(name = OMSRASTERREPROJECTOR_AUTHORNAMES, contact = OMSRASTERREPROJECTOR_AUTHORCONTACTS)
@Keywords(OMSRASTERREPROJECTOR_KEYWORDS)
@Label(OMSRASTERREPROJECTOR_LABEL)
@Name("_" + OMSRASTERREPROJECTOR_NAME)
@Status(OMSRASTERREPROJECTOR_STATUS)
@License(OMSRASTERREPROJECTOR_LICENSE)
public class RasterReprojector extends JGTModel {

    @Description(OMSRASTERREPROJECTOR_IN_RASTER_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster;

    @Description(OMSRASTERREPROJECTOR_P_NORTH_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSRASTERREPROJECTOR_P_SOUTH_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSRASTERREPROJECTOR_P_WEST_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSRASTERREPROJECTOR_P_EAST_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSRASTERREPROJECTOR_P_ROWS_DESCRIPTION)
    @UI(JGTConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSRASTERREPROJECTOR_P_COLS_DESCRIPTION)
    @UI(JGTConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSRASTERREPROJECTOR_P_CODE_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSRASTERREPROJECTOR_P_INTERPOLATION_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSRASTERREPROJECTOR_OUT_RASTER_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        OmsRasterReprojector rasterreprojector = new OmsRasterReprojector();
        rasterreprojector.inRaster = getRaster(inRaster);
        rasterreprojector.pNorth = pNorth;
        rasterreprojector.pSouth = pSouth;
        rasterreprojector.pWest = pWest;
        rasterreprojector.pEast = pEast;
        rasterreprojector.pRows = pRows;
        rasterreprojector.pCols = pCols;
        rasterreprojector.pCode = pCode;
        rasterreprojector.pInterpolation = pInterpolation;
        rasterreprojector.pm = pm;
        rasterreprojector.doProcess = doProcess;
        rasterreprojector.doReset = doReset;
        rasterreprojector.process();
        dumpRaster(rasterreprojector.outRaster, outRaster);
    }
}
