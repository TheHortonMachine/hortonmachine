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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_pDensify_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_pLimit_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_pLookahead_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_pSimplify_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERMCMASTER_pSlide_DESCRIPTION;
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
import org.jgrasstools.gears.modules.v.smoothing.OmsLineSmootherMcMaster;

@Description(OMSLINESMOOTHERMCMASTER_DESCRIPTION)
@Documentation(OMSLINESMOOTHERMCMASTER_DOCUMENTATION)
@Author(name = OMSLINESMOOTHERMCMASTER_AUTHORNAMES, contact = OMSLINESMOOTHERMCMASTER_AUTHORCONTACTS)
@Keywords(OMSLINESMOOTHERMCMASTER_KEYWORDS)
@Label(OMSLINESMOOTHERMCMASTER_LABEL)
@Name("_" + OMSLINESMOOTHERMCMASTER_NAME)
@Status(OMSLINESMOOTHERMCMASTER_STATUS)
@License(OMSLINESMOOTHERMCMASTER_LICENSE)
public class LineSmootherMcMaster extends JGTModel {

    @Description(OMSLINESMOOTHERMCMASTER_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector;

    @Description(OMSLINESMOOTHERMCMASTER_pLookahead_DESCRIPTION)
    @In
    public int pLookahead = 7;

    @Description(OMSLINESMOOTHERMCMASTER_pLimit_DESCRIPTION)
    @In
    public int pLimit = 0;

    @Description(OMSLINESMOOTHERMCMASTER_pSlide_DESCRIPTION)
    @In
    public double pSlide = 0.9;

    @Description(OMSLINESMOOTHERMCMASTER_pDensify_DESCRIPTION)
    @In
    public Double pDensify = null;

    @Description(OMSLINESMOOTHERMCMASTER_pSimplify_DESCRIPTION)
    @In
    public Double pSimplify = null;

    @Description(OMSLINESMOOTHERMCMASTER_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
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
