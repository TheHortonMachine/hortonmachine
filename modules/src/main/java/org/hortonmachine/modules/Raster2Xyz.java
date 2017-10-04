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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_DO_REMOVE_NV_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_IN_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTER2XYZ_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.raster2xyz.OmsRaster2Xyz;

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

@Description(OMSRASTER2XYZ_DESCRIPTION)
@Author(name = OMSRASTER2XYZ_AUTHORNAMES, contact = OMSRASTER2XYZ_AUTHORCONTACTS)
@Keywords(OMSRASTER2XYZ_KEYWORDS)
@Label(OMSRASTER2XYZ_LABEL)
@Name("_" + OMSRASTER2XYZ_NAME)
@Status(OMSRASTER2XYZ_STATUS)
@License(OMSRASTER2XYZ_LICENSE)
public class Raster2Xyz extends HMModel {

    @Description(OMSRASTER2XYZ_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OMSRASTER2XYZ_IN_FILE_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String inFile;

    @Description(OMSRASTER2XYZ_DO_REMOVE_NV_DESCRIPTION)
    @In
    public boolean doRemovenv = true;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        OmsRaster2Xyz raster2xyz = new OmsRaster2Xyz();
        raster2xyz.inRaster = getRaster(inRaster);
        raster2xyz.inFile = inFile;
        raster2xyz.doRemovenv = doRemovenv;
        raster2xyz.pm = pm;
        raster2xyz.doProcess = doProcess;
        raster2xyz.doReset = doReset;
        raster2xyz.process();
    }
}
