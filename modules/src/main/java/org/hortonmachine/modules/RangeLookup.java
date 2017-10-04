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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_P_CLASSES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_P_RANGES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRANGELOOKUP_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup;

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

@Description(OMSRANGELOOKUP_DESCRIPTION)
@Author(name = OMSRANGELOOKUP_AUTHORNAMES, contact = OMSRANGELOOKUP_AUTHORCONTACTS)
@Keywords(OMSRANGELOOKUP_KEYWORDS)
@Label(OMSRANGELOOKUP_LABEL)
@Name("_" + OMSRANGELOOKUP_NAME)
@Status(OMSRANGELOOKUP_STATUS)
@License(OMSRANGELOOKUP_LICENSE)
public class RangeLookup extends HMModel {

    @Description(OMSRANGELOOKUP_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OMSRANGELOOKUP_P_RANGES_DESCRIPTION)
    @In
    public String pRanges;

    @Description(OMSRANGELOOKUP_P_CLASSES_DESCRIPTION)
    @In
    public String pClasses;

    @Description(OMSRANGELOOKUP_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        OmsRangeLookup rangelookup = new OmsRangeLookup();
        rangelookup.inRaster = getRaster(inRaster);
        rangelookup.pRanges = pRanges;
        rangelookup.pClasses = pClasses;
        rangelookup.pm = pm;
        rangelookup.doProcess = doProcess;
        rangelookup.doReset = doReset;
        rangelookup.process();
        dumpRaster(rangelookup.outRaster, outRaster);
    }
}
