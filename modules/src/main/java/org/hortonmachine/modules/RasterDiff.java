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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_DO_NEGATIVES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_IN_RASTER1_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_IN_RASTER2_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_P_THRESHOLD_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERDIFF_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSRASTERDIFF_DESCRIPTION)
@Documentation(OMSRASTERDIFF_DOCUMENTATION)
@Author(name = OMSRASTERDIFF_AUTHORNAMES, contact = OMSRASTERDIFF_AUTHORCONTACTS)
@Keywords(OMSRASTERDIFF_KEYWORDS)
@Label(OMSRASTERDIFF_LABEL)
@Name(OMSRASTERDIFF_NAME)
@Status(OMSRASTERDIFF_STATUS)
@License(OMSRASTERDIFF_LICENSE)
public class RasterDiff extends HMModel {

    @Description(OMSRASTERDIFF_IN_RASTER1_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster1;

    @Description(OMSRASTERDIFF_IN_RASTER2_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster2;

    @Description(OMSRASTERDIFF_P_THRESHOLD_DESCRIPTION)
    @In
    public Double pThreshold;
    
    @Description(OMSRASTERDIFF_DO_NEGATIVES_DESCRIPTION)
    @In
    public boolean doNegatives = true;

    @Description(OMSRASTERDIFF_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsRasterDiff rasterdiff = new OmsRasterDiff();
        rasterdiff.inRaster1 = getRaster(inRaster1);
        rasterdiff.inRaster2 = getRaster(inRaster2);
        rasterdiff.pThreshold = pThreshold;
        rasterdiff.doNegatives = doNegatives;
        rasterdiff.pm = pm;
        rasterdiff.doProcess = doProcess;
        rasterdiff.doReset = doReset;
        rasterdiff.process();
        dumpRaster(rasterdiff.outRaster, outRaster);
    }

}
