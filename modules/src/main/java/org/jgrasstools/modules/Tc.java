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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_inProf_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_inTan_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_outTc3_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_outTc9_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_pProfthres_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTC_pTanthres_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.hillslopeanalyses.tc.OmsTc;

@Description(OMSTC_DESCRIPTION)
@Author(name = OMSTC_AUTHORNAMES, contact = OMSTC_AUTHORCONTACTS)
@Keywords(OMSTC_KEYWORDS)
@Label(OMSTC_LABEL)
@Name("_" + OMSTC_NAME)
@Status(OMSTC_STATUS)
@License(OMSTC_LICENSE)
public class Tc extends JGTModel {

    @Description(OMSTC_inProf_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inProf = null;

    @Description(OMSTC_inTan_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inTan = null;

    @Description(OMSTC_pProfthres_DESCRIPTION)
    @In
    public double pProfthres = 0.0;

    @Description(OMSTC_pTanthres_DESCRIPTION)
    @In
    public double pTanthres = 0.0;

    @Description(OMSTC_outTc9_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outTc9 = null;

    @Description(OMSTC_outTc3_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
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
