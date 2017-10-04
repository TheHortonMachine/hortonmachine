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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_outDepo_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_outMcs_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pDcoeff_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pEasting_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pMcoeff_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pMontecarlo_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pNorthing_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pVolume_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.debrisflow.OmsDebrisFlow;

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

@Description(OMSDEBRISFLOW_DESCRIPTION)
@Author(name = OMSDEBRISFLOW_AUTHORNAMES, contact = OMSDEBRISFLOW_AUTHORCONTACTS)
@Keywords(OMSDEBRISFLOW_KEYWORDS)
@Label(OMSDEBRISFLOW_LABEL)
@Name("_" + OMSDEBRISFLOW_NAME)
@Status(OMSDEBRISFLOW_STATUS)
@License(OMSDEBRISFLOW_LICENSE)
public class DebrisFlow extends HMModel {
    @Description(OMSDEBRISFLOW_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSDEBRISFLOW_pVolume_DESCRIPTION)
    @Unit("m2")
    @In
    public double pVolume = 4000;

    @Description(OMSDEBRISFLOW_pMcoeff_DESCRIPTION)
    @Unit("-")
    @In
    public double pMcoeff = 52;

    @Description(OMSDEBRISFLOW_pDcoeff_DESCRIPTION)
    @Unit("-")
    @In
    public double pDcoeff = 0.06;

    @Description(OMSDEBRISFLOW_pEasting_DESCRIPTION)
    @Unit("m")
    @In
    public double pEasting = 143;

    @Description(OMSDEBRISFLOW_pNorthing_DESCRIPTION)
    @Unit("m")
    @In
    public double pNorthing = 604;

    @Description(OMSDEBRISFLOW_pMontecarlo_DESCRIPTION)
    @In
    public int pMontecarlo = 50;

    @Description(OMSDEBRISFLOW_outMcs_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMcs = null;

    @Description(OMSDEBRISFLOW_outDepo_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDepo = null;

    @Execute
    public void process() throws Exception {
        OmsDebrisFlow f = new OmsDebrisFlow();
        f.pm = pm;
        f.inElev = getRaster(inElev);
        f.pVolume = pVolume;
        f.pMcoeff = pMcoeff;
        f.pDcoeff = pDcoeff;
        f.pEasting = pEasting;
        f.pNorthing = pNorthing;
        f.pMontecarlo = pMontecarlo;
        f.process();
        dumpRaster(f.outMcs, outMcs);
        dumpRaster(f.outDepo, outDepo);
    }

}
