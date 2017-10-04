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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_IN_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_P_CODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_P_RES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_P_SEPARATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSXYZ2RASTER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.raster4xyz.OmsXyz2Raster;

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

@Description(OMSXYZ2RASTER_DESCRIPTION)
@Author(name = OMSXYZ2RASTER_AUTHORNAMES, contact = OMSXYZ2RASTER_AUTHORCONTACTS)
@Keywords(OMSXYZ2RASTER_KEYWORDS)
@Label(OMSXYZ2RASTER_LABEL)
@Name("_" + OMSXYZ2RASTER_NAME)
@Status(OMSXYZ2RASTER_STATUS)
@License(OMSXYZ2RASTER_LICENSE)
public class Xyz2Raster extends HMModel {

    @Description(OMSXYZ2RASTER_IN_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inFile;

    @Description(OMSXYZ2RASTER_P_RES_DESCRIPTION)
    @In
    public Double pRes;

    @Description(OMSXYZ2RASTER_P_CODE_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSXYZ2RASTER_P_SEPARATOR_DESCRIPTION)
    @In
    public String pSeparator;

    @Description(OMSXYZ2RASTER_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsXyz2Raster xyz2raster = new OmsXyz2Raster();
        xyz2raster.inFile = inFile;
        xyz2raster.pRes = pRes;
        xyz2raster.pCode = pCode;
        xyz2raster.pSeparator = pSeparator;
        xyz2raster.pm = pm;
        xyz2raster.doProcess = doProcess;
        xyz2raster.doReset = doReset;
        xyz2raster.process();
        dumpRaster(xyz2raster.outRaster, outRaster);
    }
}
