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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_P_ALPHA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools;

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

@Description(OMSLINESMOOTHERJAITOOLS_DESCRIPTION)
@Author(name = OMSLINESMOOTHERJAITOOLS_AUTHORNAMES, contact = OMSLINESMOOTHERJAITOOLS_AUTHORCONTACTS)
@Keywords(OMSLINESMOOTHERJAITOOLS_KEYWORDS)
@Label(OMSLINESMOOTHERJAITOOLS_LABEL)
@Name("_" + OMSLINESMOOTHERJAITOOLS_NAME)
@Status(OMSLINESMOOTHERJAITOOLS_STATUS)
@License(OMSLINESMOOTHERJAITOOLS_LICENSE)
public class LineSmootherJaitools extends HMModel {

    @Description(OMSLINESMOOTHERJAITOOLS_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description(OMSLINESMOOTHERJAITOOLS_P_ALPHA_DESCRIPTION)
    @In
    public double pAlpha = 0;

    @Description(OMSLINESMOOTHERJAITOOLS_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVector;

    @Execute
    public void process() throws Exception {
        OmsLineSmootherJaitools linesmootherjaitools = new OmsLineSmootherJaitools();
        linesmootherjaitools.inVector = getVector(inVector);
        linesmootherjaitools.pAlpha = pAlpha;
        linesmootherjaitools.pm = pm;
        linesmootherjaitools.doProcess = doProcess;
        linesmootherjaitools.doReset = doReset;
        linesmootherjaitools.process();
        dumpVector(linesmootherjaitools.outVector, outVector);
    }

}
