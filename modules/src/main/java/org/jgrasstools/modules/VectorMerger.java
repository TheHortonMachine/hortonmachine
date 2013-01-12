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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_inVector1_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_inVector2_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORMERGER_outVector_DESCRIPTION;

import java.util.Arrays;

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
import org.jgrasstools.gears.modules.v.vectormerger.OmsVectorMerger;

@Description(OMSVECTORMERGER_DESCRIPTION)
@Documentation(OMSVECTORMERGER_DOCUMENTATION)
@Author(name = OMSVECTORMERGER_AUTHORNAMES, contact = OMSVECTORMERGER_AUTHORCONTACTS)
@Keywords(OMSVECTORMERGER_KEYWORDS)
@Label(OMSVECTORMERGER_LABEL)
@Name("_" + OMSVECTORMERGER_NAME)
@Status(OMSVECTORMERGER_STATUS)
@License(OMSVECTORMERGER_LICENSE)
public class VectorMerger extends JGTModel {

    @Description(OMSVECTORMERGER_inVector1_DESCRIPTION)
    @In
    public String inVector1;

    @Description(OMSVECTORMERGER_inVector2_DESCRIPTION)
    @In
    public String inVector2;

    @Description(OMSVECTORMERGER_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outVector;

    @Execute
    public void process() throws Exception {
        checkNull(inVector1, inVector2);
        OmsVectorMerger vectormerger = new OmsVectorMerger();
        vectormerger.inVectors = Arrays.asList(getVector(inVector1), getVector(inVector2));
        vectormerger.pm = pm;
        vectormerger.doProcess = doProcess;
        vectormerger.doReset = doReset;
        vectormerger.process();
        dumpVector(vectormerger.outVector, outVector);
    }
}
