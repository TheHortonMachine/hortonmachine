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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_inMap1_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_inMap2_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_outMap_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTOROVERLAYOPERATORS_pType_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.Variables.DIFFERENCE;
import static org.jgrasstools.gears.libs.modules.Variables.INTERSECTION;
import static org.jgrasstools.gears.libs.modules.Variables.SYMDIFFERENCE;
import static org.jgrasstools.gears.libs.modules.Variables.UNION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators;

@Description(OMSVECTOROVERLAYOPERATORS_DESCRIPTION)
@Author(name = OMSVECTOROVERLAYOPERATORS_AUTHORNAMES, contact = OMSVECTOROVERLAYOPERATORS_AUTHORCONTACTS)
@Keywords(OMSVECTOROVERLAYOPERATORS_KEYWORDS)
@Label(OMSVECTOROVERLAYOPERATORS_LABEL)
@Name("_" + OMSVECTOROVERLAYOPERATORS_NAME)
@Status(OMSVECTOROVERLAYOPERATORS_STATUS)
@License(OMSVECTOROVERLAYOPERATORS_LICENSE)
public class VectorOverlayOperators extends JGTModel {

    @Description(OMSVECTOROVERLAYOPERATORS_inMap1_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap1 = null;

    @Description(OMSVECTOROVERLAYOPERATORS_inMap2_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap2 = null;

    @Description(OMSVECTOROVERLAYOPERATORS_pType_DESCRIPTION)
    @UI("combo:" + INTERSECTION + "," + UNION + "," + DIFFERENCE + "," + SYMDIFFERENCE)
    @In
    public String pType = INTERSECTION;

    @Description(OMSVECTOROVERLAYOPERATORS_outMap_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsVectorOverlayOperators vectoroverlayoperators = new OmsVectorOverlayOperators();
        vectoroverlayoperators.inMap1 = getVector(inMap1);
        vectoroverlayoperators.inMap2 = getVector(inMap2);
        vectoroverlayoperators.pType = pType;
        vectoroverlayoperators.pm = pm;
        vectoroverlayoperators.doProcess = doProcess;
        vectoroverlayoperators.doReset = doReset;
        vectoroverlayoperators.process();
        dumpVector(vectoroverlayoperators.outMap, outMap);
    }
}
