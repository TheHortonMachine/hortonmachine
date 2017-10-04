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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_F_ROUND_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_P_PATTERN_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORFIELDROUNDER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder;

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

@Description(OMSVECTORFIELDROUNDER_DESCRIPTION)
@Author(name = OMSVECTORFIELDROUNDER_AUTHORNAMES, contact = OMSVECTORFIELDROUNDER_AUTHORCONTACTS)
@Keywords(OMSVECTORFIELDROUNDER_KEYWORDS)
@Label(OMSVECTORFIELDROUNDER_LABEL)
@Name("_" + OMSVECTORFIELDROUNDER_NAME)
@Status(OMSVECTORFIELDROUNDER_STATUS)
@License(OMSVECTORFIELDROUNDER_LICENSE)
public class VectorFieldRounder extends HMModel {

    @Description(OMSVECTORFIELDROUNDER_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description(OMSVECTORFIELDROUNDER_F_ROUND_DESCRIPTION)
    @In
    public String fRound = null;

    @Description(OMSVECTORFIELDROUNDER_P_PATTERN_DESCRIPTION)
    @In
    public String pPattern = null;

    @Description(OMSVECTORFIELDROUNDER_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVector;

    @Execute
    public void process() throws Exception {
        OmsVectorFieldRounder vectorfieldrounder = new OmsVectorFieldRounder();
        vectorfieldrounder.inVector = getVector(inVector);
        vectorfieldrounder.fRound = fRound;
        vectorfieldrounder.pPattern = pPattern;
        vectorfieldrounder.pm = pm;
        vectorfieldrounder.doProcess = doProcess;
        vectorfieldrounder.doReset = doReset;
        vectorfieldrounder.process();
        dumpVector(vectorfieldrounder.outVector, outVector);
    }
}
