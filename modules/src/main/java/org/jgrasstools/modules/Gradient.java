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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_doDegrees_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_outSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_pMode_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.geomorphology.gradient.OmsGradient;

@Description(OMSGRADIENT_DESCRIPTION)
@Documentation(OMSGRADIENT_DOCUMENTATION)
@Author(name = OMSGRADIENT_AUTHORNAMES, contact = OMSGRADIENT_AUTHORCONTACTS)
@Keywords(OMSGRADIENT_KEYWORDS)
@Label(OMSGRADIENT_LABEL)
@Name("_" + OMSGRADIENT_NAME)
@Status(OMSGRADIENT_STATUS)
@License(OMSGRADIENT_LICENSE)
public class Gradient extends JGTModel {
    @Description(OMSGRADIENT_inElev_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev = null;

    @Description(OMSGRADIENT_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSGRADIENT_doDegrees_DESCRIPTION)
    @In
    public boolean doDegrees = false;

    @Description(OMSGRADIENT_outSlope_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
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
