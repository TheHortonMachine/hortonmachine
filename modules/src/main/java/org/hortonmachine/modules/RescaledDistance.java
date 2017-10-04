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

import static org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance;

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

@Description(OMSRESCALEDDISTANCE_DESCRIPTION)
@Author(name = OMSRESCALEDDISTANCE_AUTHORNAMES, contact = OMSRESCALEDDISTANCE_AUTHORCONTACTS)
@Keywords(OMSRESCALEDDISTANCE_KEYWORDS)
@Label(OMSRESCALEDDISTANCE_LABEL)
@Name("_" + OMSRESCALEDDISTANCE_NAME)
@Status(OMSRESCALEDDISTANCE_STATUS)
@License(OMSRESCALEDDISTANCE_LICENSE)
public class RescaledDistance extends HMModel {

    @Description(OMSRESCALEDDISTANCE_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSRESCALEDDISTANCE_inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(OMSRESCALEDDISTANCE_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSRESCALEDDISTANCE_pRatio_DESCRIPTION)
    @In
    public double pRatio = 0;

    @Description(OMSRESCALEDDISTANCE_outRescaled_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRescaled = null;

    @Execute
    public void process() throws Exception {
        OmsRescaledDistance rescaleddistance = new OmsRescaledDistance();
        rescaleddistance.inFlow = getRaster(inFlow);
        rescaleddistance.inNet = getRaster(inNet);
        rescaleddistance.inElev = getRaster(inElev);
        rescaleddistance.pRatio = pRatio;
        rescaleddistance.pm = pm;
        rescaleddistance.doProcess = doProcess;
        rescaleddistance.doReset = doReset;
        rescaleddistance.process();
        dumpRaster(rescaleddistance.outRescaled, outRescaled);
    }
}
