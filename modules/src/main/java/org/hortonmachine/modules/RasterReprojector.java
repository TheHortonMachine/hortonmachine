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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.libs.modules.Variables.BICUBIC;
import static org.hortonmachine.gears.libs.modules.Variables.BILINEAR;
import static org.hortonmachine.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_LABEL;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_LICENSE;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_NAME;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_P_INTERPOLATION_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_P_OUTCODE_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_P_XRES_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_P_YRES_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector.OMSRASTERREPROJECTOR_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector;

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

@Description(OMSRASTERREPROJECTOR_DESCRIPTION)
@Author(name = OMSRASTERREPROJECTOR_AUTHORNAMES, contact = OMSRASTERREPROJECTOR_AUTHORCONTACTS)
@Keywords(OMSRASTERREPROJECTOR_KEYWORDS)
@Label(OMSRASTERREPROJECTOR_LABEL)
@Name("_" + OMSRASTERREPROJECTOR_NAME)
@Status(OMSRASTERREPROJECTOR_STATUS)
@License(OMSRASTERREPROJECTOR_LICENSE)
public class RasterReprojector extends HMModel {

    @Description(OMSRASTERREPROJECTOR_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

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
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        OmsRasterReprojector rasterreprojector = new OmsRasterReprojector();
        rasterreprojector.inRaster = getRaster(inRaster);
        rasterreprojector.pXres = pXres;
        rasterreprojector.pYres = pYres;
        rasterreprojector.pCode = pCode;
        rasterreprojector.pInterpolation = pInterpolation;
        rasterreprojector.pm = pm;
        rasterreprojector.doProcess = doProcess;
        rasterreprojector.doReset = doReset;
        rasterreprojector.process();
        dumpRaster(rasterreprojector.outRaster, outRaster);
    }
}
