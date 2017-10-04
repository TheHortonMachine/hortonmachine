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

import static org.hortonmachine.gears.libs.modules.Variables.EVANS;
import static org.hortonmachine.gears.libs.modules.Variables.FINITE_DIFFERENCES;
import static org.hortonmachine.gears.libs.modules.Variables.HORN;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_KEYWORDS;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_LABEL;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_LICENSE;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_NAME;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_STATUS;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_doDegrees_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_outSlope_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient.OMSGRADIENT_pMode_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient;

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

@Description(OMSGRADIENT_DESCRIPTION)
@Author(name = OMSGRADIENT_AUTHORNAMES, contact = OMSGRADIENT_AUTHORCONTACTS)
@Keywords(OMSGRADIENT_KEYWORDS)
@Label(OMSGRADIENT_LABEL)
@Name("_" + OMSGRADIENT_NAME)
@Status(OMSGRADIENT_STATUS)
@License(OMSGRADIENT_LICENSE)
public class Gradient extends HMModel {
    @Description(OMSGRADIENT_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSGRADIENT_pMode_DESCRIPTION)
    @UI("combo:" + FINITE_DIFFERENCES + "," + HORN + "," + EVANS)
    @In
    public String pMode = FINITE_DIFFERENCES;

    @Description(OMSGRADIENT_doDegrees_DESCRIPTION)
    @In
    public boolean doDegrees = false;

    @Description(OMSGRADIENT_outSlope_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outSlope = null;

    @Execute
    public void process() throws Exception {
        OmsGradient gradient = new OmsGradient();
        gradient.inElev = getRaster(inElev);
        gradient.pMode = pMode;
        gradient.doDegrees = doDegrees;
        gradient.pm = pm;
        gradient.doProcess = doProcess;
        gradient.doReset = doReset;
        gradient.process();
        dumpRaster(gradient.outSlope, outSlope);
    }

}
