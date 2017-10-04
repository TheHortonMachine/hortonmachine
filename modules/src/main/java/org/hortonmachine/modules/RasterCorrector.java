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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_P_CORRECTIONS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERCORRECTOR_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector;

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

@Description(OMSRASTERCORRECTOR_DESCRIPTION)
@Author(name = OMSRASTERCORRECTOR_AUTHORNAMES, contact = OMSRASTERCORRECTOR_AUTHORCONTACTS)
@Keywords(OMSRASTERCORRECTOR_KEYWORDS)
@Label(OMSRASTERCORRECTOR_LABEL)
@Name("_" + OMSRASTERCORRECTOR_NAME)
@Status(OMSRASTERCORRECTOR_STATUS)
@License(OMSRASTERCORRECTOR_LICENSE)
public class RasterCorrector extends HMModel {

    @Description(OMSRASTERCORRECTOR_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OMSRASTERCORRECTOR_P_CORRECTIONS_DESCRIPTION)
    @UI(HMConstants.EASTINGNORTHING_UI_HINT)
    @In
    public String pCorrections;

    @Description(OMSRASTERCORRECTOR_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsRasterCorrector rastercorrector = new OmsRasterCorrector();
        rastercorrector.inRaster = getRaster(inRaster);
        rastercorrector.pCorrections = pCorrections;
        rastercorrector.pm = pm;
        rastercorrector.doProcess = doProcess;
        rastercorrector.doReset = doReset;
        rastercorrector.process();
        dumpRaster(rastercorrector.outRaster, outRaster);
    }
}
