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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_inNet_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_outH2cd_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSH2CD_pMode_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.hillslopeanalyses.h2cd.OmsH2cd;

@Description(OMSH2CD_DESCRIPTION)
@Author(name = OMSH2CD_AUTHORNAMES, contact = OMSH2CD_AUTHORCONTACTS)
@Keywords(OMSH2CD_KEYWORDS)
@Label(OMSH2CD_LABEL)
@Name("_" + OMSH2CD_NAME)
@Status(OMSH2CD_STATUS)
@License(OMSH2CD_LICENSE)
public class H2cd extends JGTModel {

    @Description(OMSH2CD_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(OMSH2CD_inNet_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNet = null;

    @Description(OMSH2CD_inElev_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev = null;

    @Description(OMSH2CD_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSH2CD_outH2cd_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outH2cd = null;

    @Execute
    public void process() throws Exception {
        OmsH2cd h2cd = new OmsH2cd();
        h2cd.inFlow = getRaster(inFlow);
        h2cd.inNet = getRaster(inNet);
        h2cd.inElev = getRaster(inElev);
        h2cd.pMode = pMode;
        h2cd.pm = pm;
        h2cd.doProcess = doProcess;
        h2cd.doReset = doReset;
        h2cd.process();
        dumpRaster(h2cd.outH2cd, outH2cd);
    }
}
