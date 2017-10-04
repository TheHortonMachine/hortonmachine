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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_inStrahler_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_outArea_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_outBisfurcation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSTRAHLERRATIOS_outLength_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.network.strahler.OmsStrahlerRatios;

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

@Description(OMSSTRAHLERRATIOS_DESCRIPTION)
@Author(name = OMSSTRAHLERRATIOS_AUTHORNAMES, contact = OMSSTRAHLERRATIOS_AUTHORCONTACTS)
@Keywords(OMSSTRAHLERRATIOS_KEYWORDS)
@Label(OMSSTRAHLERRATIOS_LABEL)
@Name("_" + OMSSTRAHLERRATIOS_NAME)
@Status(OMSSTRAHLERRATIOS_STATUS)
@License(OMSSTRAHLERRATIOS_LICENSE)
public class StrahlerRatios extends HMModel {

    @Description(OMSSTRAHLERRATIOS_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSSTRAHLERRATIOS_inStrahler_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inStrahler = null;

    @Description(OMSSTRAHLERRATIOS_inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNet = null;

    @Description(OMSSTRAHLERRATIOS_outBisfurcation_DESCRIPTION)
    @In
    public double outBisfurcation;

    @Description(OMSSTRAHLERRATIOS_outArea_DESCRIPTION)
    @In
    public double outArea;

    @Description(OMSSTRAHLERRATIOS_outLength_DESCRIPTION)
    @In
    public double outLength;

    @Execute
    public void process() throws Exception {
        OmsStrahlerRatios omsstrahlerratios = new OmsStrahlerRatios();
        omsstrahlerratios.inFlow = getRaster(inFlow);
        omsstrahlerratios.inStrahler = getRaster(inStrahler);
        omsstrahlerratios.inNet = getVector(inNet);
        omsstrahlerratios.pm = pm;
        omsstrahlerratios.doProcess = doProcess;
        omsstrahlerratios.doReset = doReset;
        omsstrahlerratios.process();
        outBisfurcation = omsstrahlerratios.outBisfurcation;
        outArea = omsstrahlerratios.outArea;
        outLength = omsstrahlerratios.outLength;
    }

}
