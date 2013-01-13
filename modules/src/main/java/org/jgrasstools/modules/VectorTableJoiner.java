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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_fCommon_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_pFields_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORTABLEJOINER_tabledata_DESCRIPTION;

import java.util.HashMap;
import java.util.List;

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
import org.jgrasstools.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner;

@Description(OMSVECTORTABLEJOINER_DESCRIPTION)
@Documentation(OMSVECTORTABLEJOINER_DOCUMENTATION)
@Author(name = OMSVECTORTABLEJOINER_AUTHORNAMES, contact = OMSVECTORTABLEJOINER_AUTHORCONTACTS)
@Keywords(OMSVECTORTABLEJOINER_KEYWORDS)
@Label(OMSVECTORTABLEJOINER_LABEL)
@Name("_" + OMSVECTORTABLEJOINER_NAME)
@Status(OMSVECTORTABLEJOINER_STATUS)
@License(OMSVECTORTABLEJOINER_LICENSE)
public class VectorTableJoiner extends JGTModel {

    @Description(OMSVECTORTABLEJOINER_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector;

    @Description(OMSVECTORTABLEJOINER_tabledata_DESCRIPTION)
    @In
    public HashMap<String, List<Object>> tabledata = null;

    @Description(OMSVECTORTABLEJOINER_fCommon_DESCRIPTION)
    @In
    public String fCommon = null;

    @Description(OMSVECTORTABLEJOINER_pFields_DESCRIPTION)
    @In
    public String pFields = null;

    @Description(OMSVECTORTABLEJOINER_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
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
