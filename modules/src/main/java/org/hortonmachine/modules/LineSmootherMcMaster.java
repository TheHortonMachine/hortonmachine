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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_DENSIFY_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_LIMIT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_LOOK_AHEAD_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_SIMPLIFY_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_P_SLIDE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster;

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

@Description(OMSLINESMOOTHERMCMASTER_DESCRIPTION)
@Author(name = OMSLINESMOOTHERMCMASTER_AUTHORNAMES, contact = OMSLINESMOOTHERMCMASTER_AUTHORCONTACTS)
@Keywords(OMSLINESMOOTHERMCMASTER_KEYWORDS)
@Label(OMSLINESMOOTHERMCMASTER_LABEL)
@Name("_" + OMSLINESMOOTHERMCMASTER_NAME)
@Status(OMSLINESMOOTHERMCMASTER_STATUS)
@License(OMSLINESMOOTHERMCMASTER_LICENSE)
public class LineSmootherMcMaster extends HMModel {

    @Description(OMSLINESMOOTHERMCMASTER_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description(OMSLINESMOOTHERMCMASTER_P_LOOK_AHEAD_DESCRIPTION)
    @In
    public int pLookahead = 7;

    @Description(OMSLINESMOOTHERMCMASTER_P_LIMIT_DESCRIPTION)
    @In
    public int pLimit = 0;

    @Description(OMSLINESMOOTHERMCMASTER_P_SLIDE_DESCRIPTION)
    @In
    public double pSlide = 0.9;

    @Description(OMSLINESMOOTHERMCMASTER_P_DENSIFY_DESCRIPTION)
    @In
    public Double pDensify = null;

    @Description(OMSLINESMOOTHERMCMASTER_P_SIMPLIFY_DESCRIPTION)
    @In
    public Double pSimplify = null;

    @Description(OMSLINESMOOTHERMCMASTER_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVector;

    @Execute
    public void process() throws Exception {
        OmsLineSmootherMcMaster linesmoothermcmaster = new OmsLineSmootherMcMaster();
        linesmoothermcmaster.inVector = getVector(inVector);
        linesmoothermcmaster.pLookahead = pLookahead;
        linesmoothermcmaster.pLimit = pLimit;
        linesmoothermcmaster.pSlide = pSlide;
        linesmoothermcmaster.pDensify = pDensify;
        linesmoothermcmaster.pSimplify = pSimplify;
        linesmoothermcmaster.pm = pm;
        linesmoothermcmaster.doProcess = doProcess;
        linesmoothermcmaster.doReset = doReset;
        linesmoothermcmaster.process();
        dumpVector(linesmoothermcmaster.outVector, outVector);
    }
}
