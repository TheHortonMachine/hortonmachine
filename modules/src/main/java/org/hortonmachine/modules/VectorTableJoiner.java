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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_F_COMMON_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_P_FIELDS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_TABLEDATA_DESCRIPTION;

import java.util.HashMap;
import java.util.List;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner;

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

@Description(OMSVECTORTABLEJOINER_DESCRIPTION)
@Author(name = OMSVECTORTABLEJOINER_AUTHORNAMES, contact = OMSVECTORTABLEJOINER_AUTHORCONTACTS)
@Keywords(OMSVECTORTABLEJOINER_KEYWORDS)
@Label(OMSVECTORTABLEJOINER_LABEL)
@Name("_" + OMSVECTORTABLEJOINER_NAME)
@Status(OMSVECTORTABLEJOINER_STATUS)
@License(OMSVECTORTABLEJOINER_LICENSE)
public class VectorTableJoiner extends HMModel {

    @Description(OMSVECTORTABLEJOINER_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description(OMSVECTORTABLEJOINER_TABLEDATA_DESCRIPTION)
    @In
    public HashMap<String, List<Object>> tabledata = null;

    @Description(OMSVECTORTABLEJOINER_F_COMMON_DESCRIPTION)
    @In
    public String fCommon = null;

    @Description(OMSVECTORTABLEJOINER_P_FIELDS_DESCRIPTION)
    @In
    public String pFields = null;

    @Description(OMSVECTORTABLEJOINER_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVector;

    @Execute
    public void process() throws Exception {
        OmsVectorTableJoiner vectortablejoiner = new OmsVectorTableJoiner();
        vectortablejoiner.inVector = getVector(inVector);
        vectortablejoiner.tabledata = tabledata;
        vectortablejoiner.fCommon = fCommon;
        vectortablejoiner.pFields = pFields;
        vectortablejoiner.pm = pm;
        vectortablejoiner.doProcess = doProcess;
        vectortablejoiner.doReset = doReset;
        vectortablejoiner.process();
        dumpVector(vectortablejoiner.outVector, outVector);
    }
}
