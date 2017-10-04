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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_P_CQL_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_P_REMOVE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORRESHAPER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectorreshaper.OmsVectorReshaper;

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

@Description(OMSVECTORRESHAPER_DESCRIPTION)
@Author(name = OMSVECTORRESHAPER_AUTHORNAMES, contact = OMSVECTORRESHAPER_AUTHORCONTACTS)
@Keywords(OMSVECTORRESHAPER_KEYWORDS)
@Label(OMSVECTORRESHAPER_LABEL)
@Name("_" + OMSVECTORRESHAPER_NAME)
@Status(OMSVECTORRESHAPER_STATUS)
@License(OMSVECTORRESHAPER_LICENSE)
public class VectorReshaper extends HMModel {

    @Description(OMSVECTORRESHAPER_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description(OMSVECTORRESHAPER_P_CQL_DESCRIPTION)
    @UI(HMConstants.MULTILINE_UI_HINT + "5")
    @In
    public String pCql = null;

    @Description(OMSVECTORRESHAPER_P_REMOVE_DESCRIPTION)
    @In
    public String pRemove = null;

    @Description(OMSVECTORRESHAPER_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVector;

    @Execute
    public void process() throws Exception {
        OmsVectorReshaper vectorreshaper = new OmsVectorReshaper();
        vectorreshaper.inVector = getVector(inVector);
        vectorreshaper.pCql = pCql;
        vectorreshaper.pRemove = pRemove;
        vectorreshaper.pm = pm;
        vectorreshaper.doProcess = doProcess;
        vectorreshaper.doReset = doReset;
        vectorreshaper.process();
        dumpVector(vectorreshaper.outVector, outVector);
    }
}
