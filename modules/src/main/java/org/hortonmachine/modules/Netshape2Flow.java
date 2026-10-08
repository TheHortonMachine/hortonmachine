/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_DOCUMENTATION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_fActive_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_fId_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_outFlownet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_outNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_outProblems_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.network.netshape2flow.OmsNetshape2Flow;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSNETSHAPE2FLOW_DESCRIPTION)
@Documentation(OMSNETSHAPE2FLOW_DOCUMENTATION)
@Author(name = OMSNETSHAPE2FLOW_AUTHORNAMES, contact = OMSNETSHAPE2FLOW_AUTHORCONTACTS)
@Keywords(OMSNETSHAPE2FLOW_KEYWORDS)
@Label(OMSNETSHAPE2FLOW_LABEL)
@Name("_" + OMSNETSHAPE2FLOW_NAME)
@Status(OMSNETSHAPE2FLOW_STATUS)
@License(OMSNETSHAPE2FLOW_LICENSE)
public class Netshape2Flow extends HMModel {

    @Description(OMSNETSHAPE2FLOW_inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNet = null;

    @Description("A raster whose grid, extent and resolution, is used for the output maps, for example the elevation model.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inGrid = null;

    @Description(OMSNETSHAPE2FLOW_fActive_DESCRIPTION)
    @In
    public String fActive = null;

    @Description(OMSNETSHAPE2FLOW_fId_DESCRIPTION)
    @In
    public String fId = null;

    @Description(OMSNETSHAPE2FLOW_outFlownet_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFlownet = null;

    @Description(OMSNETSHAPE2FLOW_outNet_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNet = null;

    @Description(OMSNETSHAPE2FLOW_outProblems_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outProblems = null;

    @Execute
    public void process() throws Exception {
        OmsNetshape2Flow netshape2flow = new OmsNetshape2Flow();
        netshape2flow.inNet = getVector(inNet);
        netshape2flow.inGrid = getRaster(inGrid).getGridGeometry();
        netshape2flow.fActive = fActive;
        netshape2flow.fId = fId;
        netshape2flow.pm = pm;
        netshape2flow.doProcess = doProcess;
        netshape2flow.doReset = doReset;
        netshape2flow.process();
        dumpRaster(netshape2flow.outFlownet, outFlownet);
        dumpRaster(netshape2flow.outNet, outNet);
        if (outProblems != null) {
            dumpVector(netshape2flow.outProblems, outProblems);
        }
    }
}
