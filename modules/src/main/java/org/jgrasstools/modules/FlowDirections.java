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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_outFlow_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.geomorphology.flow.OmsFlowDirections;

@Description(OMSFLOWDIRECTIONS_DESCRIPTION)
@Author(name = OMSFLOWDIRECTIONS_AUTHORNAMES, contact = OMSFLOWDIRECTIONS_AUTHORCONTACTS)
@Keywords(OMSFLOWDIRECTIONS_KEYWORDS)
@Label(OMSFLOWDIRECTIONS_LABEL)
@Name("_" + OMSFLOWDIRECTIONS_NAME)
@Status(OMSFLOWDIRECTIONS_STATUS)
@License(OMSFLOWDIRECTIONS_LICENSE)
public class FlowDirections extends JGTModel {
    @Description(OMSFLOWDIRECTIONS_inPit_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inPit = null;

    @Description(OMSFLOWDIRECTIONS_outFlow_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outFlow = null;

    @Execute
    public void process() throws Exception {
        OmsFlowDirections omsflowdirections = new OmsFlowDirections();
        omsflowdirections.inPit = getRaster(inPit);
        omsflowdirections.pm = pm;
        omsflowdirections.doProcess = doProcess;
        omsflowdirections.doReset = doReset;
        omsflowdirections.process();
        dumpRaster(omsflowdirections.outFlow, outFlow);

    }

}
