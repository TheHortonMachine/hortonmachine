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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_inNet_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_outRescaled_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSRESCALEDDISTANCE_pRatio_DESCRIPTION;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.basin.rescaleddistance.OmsRescaledDistance;

@Description(OMSRESCALEDDISTANCE_DESCRIPTION)
@Author(name = OMSRESCALEDDISTANCE_AUTHORNAMES, contact = OMSRESCALEDDISTANCE_AUTHORCONTACTS)
@Keywords(OMSRESCALEDDISTANCE_KEYWORDS)
@Label(OMSRESCALEDDISTANCE_LABEL)
@Name("_" + OMSRESCALEDDISTANCE_NAME)
@Status(OMSRESCALEDDISTANCE_STATUS)
@License(OMSRESCALEDDISTANCE_LICENSE)
public class RescaledDistance extends JGTModel {

    @Description(OMSRESCALEDDISTANCE_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(OMSRESCALEDDISTANCE_inNet_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNet = null;

    @Description(OMSRESCALEDDISTANCE_inElev_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev = null;

    @Description(OMSRESCALEDDISTANCE_pRatio_DESCRIPTION)
    @In
    public double pRatio = 0;

    @Description(OMSRESCALEDDISTANCE_outRescaled_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
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
