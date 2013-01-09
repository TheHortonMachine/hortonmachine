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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_doSinglesided_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_inMap_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_outMap_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_pBuffer_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_pCapstyle_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSBUFFER_pJoinstyle_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.Variables.CAP_FLAT;
import static org.jgrasstools.gears.libs.modules.Variables.CAP_ROUND;
import static org.jgrasstools.gears.libs.modules.Variables.CAP_SQUARE;
import static org.jgrasstools.gears.libs.modules.Variables.JOIN_BEVEL;
import static org.jgrasstools.gears.libs.modules.Variables.JOIN_MITRE;
import static org.jgrasstools.gears.libs.modules.Variables.JOIN_ROUND;
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
import org.jgrasstools.gears.modules.v.vectoroperations.OmsBuffer;

@Description(OMSBUFFER_DESCRIPTION)
@Documentation(OMSBUFFER_DOCUMENTATION)
@Author(name = OMSBUFFER_AUTHORNAMES, contact = OMSBUFFER_AUTHORCONTACTS)
@Keywords(OMSBUFFER_KEYWORDS)
@Label(OMSBUFFER_LABEL)
@Name(OMSBUFFER_NAME)
@Status(OMSBUFFER_STATUS)
@License(OMSBUFFER_LICENSE)
public class Buffer extends JGTModel {

    @Description(OMSBUFFER_inMap_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap = null;

    @Description(OMSBUFFER_pBuffer_DESCRIPTION)
    @In
    public double pBuffer = 10.0;

    @Description(OMSBUFFER_doSinglesided_DESCRIPTION)
    @In
    public boolean doSinglesided = false;

    @Description(OMSBUFFER_pJoinstyle_DESCRIPTION)
    @UI("combo:" + JOIN_ROUND + "," + JOIN_MITRE + "," + JOIN_BEVEL)
    @In
    public String pJoinstyle = JOIN_ROUND;

    @Description(OMSBUFFER_pCapstyle_DESCRIPTION)
    @UI("combo:" + CAP_ROUND + "," + CAP_FLAT + "," + CAP_SQUARE)
    @In
    public String pCapstyle = CAP_ROUND;

    @Description(OMSBUFFER_outMap_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsBuffer buffer = new OmsBuffer();
        buffer.inMap = getVector(inMap);
        buffer.pBuffer = pBuffer;
        buffer.doSinglesided = doSinglesided;
        buffer.pJoinstyle = pJoinstyle;
        buffer.pCapstyle = pCapstyle;
        buffer.pm = pm;
        buffer.process();
        dumpVector(buffer.outMap, outMap);
    }

}
