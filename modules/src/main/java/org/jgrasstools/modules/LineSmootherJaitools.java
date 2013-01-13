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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_pAlpha_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.smoothing.OmsLineSmootherJaitools;

@Description(OMSLINESMOOTHERJAITOOLS_DESCRIPTION)
@Documentation(OMSLINESMOOTHERJAITOOLS_DOCUMENTATION)
@Author(name = OMSLINESMOOTHERJAITOOLS_AUTHORNAMES, contact = OMSLINESMOOTHERJAITOOLS_AUTHORCONTACTS)
@Keywords(OMSLINESMOOTHERJAITOOLS_KEYWORDS)
@Label(OMSLINESMOOTHERJAITOOLS_LABEL)
@Name("_" + OMSLINESMOOTHERJAITOOLS_NAME)
@Status(OMSLINESMOOTHERJAITOOLS_STATUS)
@License(OMSLINESMOOTHERJAITOOLS_LICENSE)
public class LineSmootherJaitools extends JGTModel {

    @Description(OMSLINESMOOTHERJAITOOLS_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector;

    @Description(OMSLINESMOOTHERJAITOOLS_pAlpha_DESCRIPTION)
    @In
    public double pAlpha = 0;

    @Description(OMSLINESMOOTHERJAITOOLS_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
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
