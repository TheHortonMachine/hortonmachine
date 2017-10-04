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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_inRaster_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_inStream_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETDIFF_outDiff_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.network.netdiff.OmsNetDiff;

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

@Description(OMSNETDIFF_DESCRIPTION)
@Author(name = OMSNETDIFF_AUTHORNAMES, contact = OMSNETDIFF_AUTHORCONTACTS)
@Keywords(OMSNETDIFF_KEYWORDS)
@Label(OMSNETDIFF_LABEL)
@Name("_" + OMSNETDIFF_NAME)
@Status(OMSNETDIFF_STATUS)
@License(OMSNETDIFF_LICENSE)
public class NetDiff extends HMModel {

    @Description(OMSNETDIFF_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSNETDIFF_inStream_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inStream = null;

    @Description(OMSNETDIFF_inRaster_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster = null;

    @Description(OMSNETDIFF_outDiff_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDiff = null;

    @Execute
    public void process() throws Exception {
        OmsNetDiff netdiff = new OmsNetDiff();
        netdiff.inFlow = getRaster(inFlow);
        netdiff.inStream = getRaster(inStream);
        netdiff.inRaster = getRaster(inRaster);
        netdiff.pm = pm;
        netdiff.doProcess = doProcess;
        netdiff.doReset = doReset;
        netdiff.process();
        dumpRaster(netdiff.outDiff, outDiff);
    }
}
