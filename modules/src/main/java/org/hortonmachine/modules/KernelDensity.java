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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_doConstant_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_inMap_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_outDensity_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_pKernel_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKERNELDENSITY_pRadius_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity;

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

@Description(OMSKERNELDENSITY_DESCRIPTION)
@Author(name = OMSKERNELDENSITY_AUTHORNAMES, contact = OMSKERNELDENSITY_AUTHORCONTACTS)
@Keywords(OMSKERNELDENSITY_KEYWORDS)
@Label(OMSKERNELDENSITY_LABEL)
@Name("_" + OMSKERNELDENSITY_NAME)
@Status(OMSKERNELDENSITY_STATUS)
@License(OMSKERNELDENSITY_LICENSE)
public class KernelDensity extends HMModel {

    @Description(OMSKERNELDENSITY_inMap_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inMap = null;

    @Description(OMSKERNELDENSITY_pKernel_DESCRIPTION)
    @In
    public int pKernel = 3;

    @Description(OMSKERNELDENSITY_pRadius_DESCRIPTION)
    @In
    public int pRadius = 10;

    @Description(OMSKERNELDENSITY_doConstant_DESCRIPTION)
    @In
    public boolean doConstant = false;

    @Description(OMSKERNELDENSITY_outDensity_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDensity = null;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        OmsKernelDensity omskerneldensity = new OmsKernelDensity();
        omskerneldensity.inMap = getRaster(inMap);
        omskerneldensity.pKernel = pKernel;
        omskerneldensity.pRadius = pRadius;
        omskerneldensity.doConstant = doConstant;
        omskerneldensity.pm = pm;
        omskerneldensity.doProcess = doProcess;
        omskerneldensity.doReset = doReset;
        omskerneldensity.process();
        dumpRaster(omskerneldensity.outDensity, outDensity);
    }
}
