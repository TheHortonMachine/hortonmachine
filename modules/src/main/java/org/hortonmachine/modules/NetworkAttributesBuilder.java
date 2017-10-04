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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_doHack_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_inTca_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_outHack_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETWORKATTRIBUTESBUILDER_outNet_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.network.networkattributes.OmsNetworkAttributesBuilder;

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

@Description(OMSNETWORKATTRIBUTESBUILDER_DESCRIPTION)
@Author(name = OMSNETWORKATTRIBUTESBUILDER_AUTHORNAMES, contact = OMSNETWORKATTRIBUTESBUILDER_AUTHORCONTACTS)
@Keywords(OMSNETWORKATTRIBUTESBUILDER_KEYWORDS)
@Label(OMSNETWORKATTRIBUTESBUILDER_LABEL)
@Name("_" + OMSNETWORKATTRIBUTESBUILDER_NAME)
@Status(OMSNETWORKATTRIBUTESBUILDER_STATUS)
@License(OMSNETWORKATTRIBUTESBUILDER_LICENSE)
public class NetworkAttributesBuilder extends HMModel {

    @Description(OMSNETWORKATTRIBUTESBUILDER_inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(OMSNETWORKATTRIBUTESBUILDER_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSNETWORKATTRIBUTESBUILDER_inTca_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTca = null;

    @Description(OMSNETWORKATTRIBUTESBUILDER_doHack_DESCRIPTION)
    @In
    public boolean doHack = false;

    @Description(OMSNETWORKATTRIBUTESBUILDER_outNet_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNet = null;

    @Description(OMSNETWORKATTRIBUTESBUILDER_outHack_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outHack = null;

    @Execute
    public void process() throws Exception {
        OmsNetworkAttributesBuilder networkattributesbuilder = new OmsNetworkAttributesBuilder();
        networkattributesbuilder.inNet = getRaster(inNet);
        networkattributesbuilder.inFlow = getRaster(inFlow);
        networkattributesbuilder.inTca = getRaster(inTca);
        networkattributesbuilder.doHack = doHack;
        networkattributesbuilder.pm = pm;
        networkattributesbuilder.doProcess = doProcess;
        networkattributesbuilder.doReset = doReset;
        networkattributesbuilder.process();
        dumpVector(networkattributesbuilder.outNet, outNet);
        dumpRaster(networkattributesbuilder.outHack, outHack);
    }
}