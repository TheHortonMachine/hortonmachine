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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_inTca_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_outTriggers_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_pGradthres_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISTRIGGERCNR_pTcathres_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.debristriggers.OmsDebrisTriggerCnr;

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
import oms3.annotations.Unit;

@Description(OMSDEBRISTRIGGERCNR_DESCRIPTION)
@Author(name = OMSDEBRISTRIGGERCNR_AUTHORNAMES, contact = OMSDEBRISTRIGGERCNR_AUTHORCONTACTS)
@Keywords(OMSDEBRISTRIGGERCNR_KEYWORDS)
@Label(OMSDEBRISTRIGGERCNR_LABEL)
@Name("_" + OMSDEBRISTRIGGERCNR_NAME)
@Status(OMSDEBRISTRIGGERCNR_STATUS)
@License(OMSDEBRISTRIGGERCNR_LICENSE)
public class DebrisTriggerCnr extends HMModel {

    @Description(OMSDEBRISTRIGGERCNR_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSDEBRISTRIGGERCNR_inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(OMSDEBRISTRIGGERCNR_inTca_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTca = null;

    @Description(OMSDEBRISTRIGGERCNR_pTcathres_DESCRIPTION)
    @Unit("km2")
    @In
    public double pTcathres = 10;

    @Description(OMSDEBRISTRIGGERCNR_pGradthres_DESCRIPTION)
    @Unit("degree")
    @In
    public double pGradthres = 38;

    @Description(OMSDEBRISTRIGGERCNR_outTriggers_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTriggers = null;

    @Execute
    public void process() throws Exception {
        OmsDebrisTriggerCnr debristriggercnr = new OmsDebrisTriggerCnr();
        debristriggercnr.inElev = getRaster(inElev);
        debristriggercnr.inNet = getRaster(inNet);
        debristriggercnr.inTca = getRaster(inTca);
        debristriggercnr.pTcathres = pTcathres;
        debristriggercnr.pGradthres = pGradthres;
        debristriggercnr.pm = pm;
        debristriggercnr.doProcess = doProcess;
        debristriggercnr.doReset = doReset;
        debristriggercnr.process();
        dumpRaster(debristriggercnr.outTriggers, outTriggers);
    }

}
