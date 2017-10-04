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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_inCohesion_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_inQ_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_inRho_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_inSdepth_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_inSlope_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_inTca_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_inTgphi_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_inTrasmissivity_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_outQcrit_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_outShalstab_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_pCohesion_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_pQ_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_pRho_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_pRock_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_pSdepth_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_pTgphi_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSHALSTAB_pTrasmissivity_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.shalstab.OmsShalstab;

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

@Description(OMSSHALSTAB_DESCRIPTION)
@Author(name = OMSSHALSTAB_AUTHORNAMES, contact = OMSSHALSTAB_AUTHORCONTACTS)
@Keywords(OMSSHALSTAB_KEYWORDS)
@Label(OMSSHALSTAB_LABEL)
@Name("_" + OMSSHALSTAB_NAME)
@Status(OMSSHALSTAB_STATUS)
@License(OMSSHALSTAB_LICENSE)
public class Shalstab extends HMModel {

    @Description(OMSSHALSTAB_inSlope_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSlope = null;

    @Description(OMSSHALSTAB_inTca_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTca = null;

    @Description(OMSSHALSTAB_inTrasmissivity_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("m^2/day")
    @In
    public String inTrasmissivity = null;

    @Description(OMSSHALSTAB_pTrasmissivity_DESCRIPTION)
    @Unit("m^2/day")
    @In
    public double pTrasmissivity = -1.0;

    @Description(OMSSHALSTAB_inTgphi_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTgphi = null;

    @Description(OMSSHALSTAB_pTgphi_DESCRIPTION)
    @In
    public double pTgphi = -1.0;

    @Description(OMSSHALSTAB_inCohesion_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("Pa")
    @In
    public String inCohesion = null;

    @Description(OMSSHALSTAB_pCohesion_DESCRIPTION)
    @Unit("Pa")
    @In
    public double pCohesion = -1.0;

    @Description(OMSSHALSTAB_inSdepth_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("m")
    @In
    public String inSdepth = null;

    @Description(OMSSHALSTAB_pSdepth_DESCRIPTION)
    @Unit("m")
    @In
    public double pSdepth = -1.0;

    @Description(OMSSHALSTAB_inQ_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("mm/day")
    @In
    public String inQ = null;

    @Description(OMSSHALSTAB_pQ_DESCRIPTION)
    @Unit("mm/day")
    @In
    public double pQ = -1.0;

    @Description(OMSSHALSTAB_inRho_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRho = null;

    @Description(OMSSHALSTAB_pRho_DESCRIPTION)
    @In
    public double pRho = -1.0;

    @Description(OMSSHALSTAB_pRock_DESCRIPTION)
    @In
    public double pRock = -9999.0;

    @Description(OMSSHALSTAB_outQcrit_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outQcrit = null;

    @Description(OMSSHALSTAB_outShalstab_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outShalstab = null;

    @Execute
    public void process() throws Exception {
        OmsShalstab shalstab = new OmsShalstab();
        shalstab.inSlope = getRaster(inSlope);
        shalstab.inTca = getRaster(inTca);
        shalstab.inTrasmissivity = getRaster(inTrasmissivity);
        shalstab.pTrasmissivity = pTrasmissivity;
        shalstab.inTgphi = getRaster(inTgphi);
        shalstab.pTgphi = pTgphi;
        shalstab.inCohesion = getRaster(inCohesion);
        shalstab.pCohesion = pCohesion;
        shalstab.inSdepth = getRaster(inSdepth);
        shalstab.pSdepth = pSdepth;
        shalstab.inQ = getRaster(inQ);
        shalstab.pQ = pQ;
        shalstab.inRho = getRaster(inRho);
        shalstab.pRho = pRho;
        shalstab.pRock = pRock;
        shalstab.pm = pm;
        shalstab.doProcess = doProcess;
        shalstab.doReset = doReset;
        shalstab.process();
        dumpRaster(shalstab.outQcrit, outQcrit);
        dumpRaster(shalstab.outShalstab, outShalstab);
    }
}
