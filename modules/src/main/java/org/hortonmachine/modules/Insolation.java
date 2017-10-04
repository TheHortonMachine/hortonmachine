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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_outIns_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_tEndDate_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSINSOLATION_tStartDate_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.insolation.OmsInsolation;

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

@Description(OMSINSOLATION_DESCRIPTION)
@Author(name = OMSINSOLATION_AUTHORNAMES, contact = OMSINSOLATION_AUTHORCONTACTS)
@Keywords(OMSINSOLATION_KEYWORDS)
@Label(OMSINSOLATION_LABEL)
@Name("_" + OMSINSOLATION_NAME)
@Status(OMSINSOLATION_STATUS)
@License(OMSINSOLATION_LICENSE)
public class Insolation extends HMModel {

    @Description(OMSINSOLATION_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSINSOLATION_tStartDate_DESCRIPTION)
    @In
    public String tStartDate = null;

    @Description(OMSINSOLATION_tEndDate_DESCRIPTION)
    @In
    public String tEndDate = null;

    @Description(OMSINSOLATION_outIns_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outIns;

    @Execute
    public void process() throws Exception {
        OmsInsolation insolation = new OmsInsolation();
        insolation.inElev = getRaster(inElev);
        insolation.tStartDate = tStartDate;
        insolation.tEndDate = tEndDate;
        insolation.pm = pm;
        insolation.doProcess = doProcess;
        insolation.doReset = doReset;
        insolation.process();
        dumpRaster(insolation.outIns, outIns);
    }
}
