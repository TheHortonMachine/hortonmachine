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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_inProf_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_inTan_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_outTc3_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_outTc9_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_pProfthres_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTC_pTanthres_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hillslopeanalyses.tc.OmsTc;

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

@Description(OMSTC_DESCRIPTION)
@Author(name = OMSTC_AUTHORNAMES, contact = OMSTC_AUTHORCONTACTS)
@Keywords(OMSTC_KEYWORDS)
@Label(OMSTC_LABEL)
@Name("_" + OMSTC_NAME)
@Status(OMSTC_STATUS)
@License(OMSTC_LICENSE)
public class Tc extends HMModel {

    @Description(OMSTC_inProf_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inProf = null;

    @Description(OMSTC_inTan_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTan = null;

    @Description(OMSTC_pProfthres_DESCRIPTION)
    @In
    public double pProfthres = 0.0;

    @Description(OMSTC_pTanthres_DESCRIPTION)
    @In
    public double pTanthres = 0.0;

    @Description(OMSTC_outTc9_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTc9 = null;

    @Description(OMSTC_outTc3_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTc3 = null;

    @Execute
    public void process() throws Exception {
        OmsTc tc = new OmsTc();
        tc.inProf = getRaster(inProf);
        tc.inTan = getRaster(inTan);
        tc.pProfthres = pProfthres;
        tc.pTanthres = pTanthres;
        tc.pm = pm;
        tc.doProcess = doProcess;
        tc.doReset = doReset;
        tc.process();
        dumpRaster(tc.outTc9, outTc9);
        dumpRaster(tc.outTc3, outTc3);
    }
}
