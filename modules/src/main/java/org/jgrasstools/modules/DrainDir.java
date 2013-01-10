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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_doLad_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_inFlownet_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_outFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_outTca_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDRAINDIR_pLambda_DESCRIPTION;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
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
import org.jgrasstools.hortonmachine.modules.geomorphology.draindir.OmsDrainDir;

@Description(OMSDRAINDIR_DESCRIPTION)
@Documentation(OMSDRAINDIR_DOCUMENTATION)
@Author(name = OMSDRAINDIR_AUTHORNAMES, contact = OMSDRAINDIR_AUTHORCONTACTS)
@Keywords(OMSDRAINDIR_KEYWORDS)
@Label(OMSDRAINDIR_LABEL)
@Name("_" + OMSDRAINDIR_NAME)
@Status(OMSDRAINDIR_STATUS)
@License(OMSDRAINDIR_LICENSE)
public class DrainDir extends JGTModel {

    @Description(OMSDRAINDIR_inPit_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inPit = null;

    @Description(OMSDRAINDIR_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(OMSDRAINDIR_inFlownet_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlownet = null;

    @Description(OMSDRAINDIR_pLambda_DESCRIPTION)
    @In
    public double pLambda = 1.0;

    @Description(OMSDRAINDIR_doLad_DESCRIPTION)
    @In
    public boolean doLad = true;

    @Description(OMSDRAINDIR_outFlow_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outFlow = null;

    @Description(OMSDRAINDIR_outTca_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outTca = null;

    @Execute
    public void process() throws Exception {
        OmsDrainDir draindir = new OmsDrainDir();
        draindir.inPit = getRaster(inPit);
        draindir.inFlow = getRaster(inFlow);
        draindir.inFlownet = getRaster(inFlownet);
        draindir.pLambda = pLambda;
        draindir.doLad = doLad;
        draindir.pm = pm;
        draindir.doProcess = doProcess;
        draindir.doReset = doReset;
        draindir.process();
        dumpRaster(draindir.outFlow, outFlow);
        dumpRaster(draindir.outTca, outTca);
    }

}
