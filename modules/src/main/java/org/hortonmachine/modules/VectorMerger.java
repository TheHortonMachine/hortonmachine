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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_IN_VECTOR1_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_IN_VECTOR2_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORMERGER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.FILEIN_UI_HINT_VECTOR;

import java.util.Arrays;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger;

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

@Description(OMSVECTORMERGER_DESCRIPTION)
@Author(name = OMSVECTORMERGER_AUTHORNAMES, contact = OMSVECTORMERGER_AUTHORCONTACTS)
@Keywords(OMSVECTORMERGER_KEYWORDS)
@Label(OMSVECTORMERGER_LABEL)
@Name("_" + OMSVECTORMERGER_NAME)
@Status(OMSVECTORMERGER_STATUS)
@License(OMSVECTORMERGER_LICENSE)
public class VectorMerger extends HMModel {

    @Description(OMSVECTORMERGER_IN_VECTOR1_DESCRIPTION)
    @UI(FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector1;

    @Description(OMSVECTORMERGER_IN_VECTOR2_DESCRIPTION)
    @UI(FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector2;

    @Description(OMSVECTORMERGER_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
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
