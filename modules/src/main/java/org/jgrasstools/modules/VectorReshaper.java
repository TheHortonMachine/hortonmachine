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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_pCql_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORRESHAPER_pRemove_DESCRIPTION;
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
import org.jgrasstools.gears.modules.v.vectorreshaper.OmsVectorReshaper;

@Description(OMSVECTORRESHAPER_DESCRIPTION)
@Documentation(OMSVECTORRESHAPER_DOCUMENTATION)
@Author(name = OMSVECTORRESHAPER_AUTHORNAMES, contact = OMSVECTORRESHAPER_AUTHORCONTACTS)
@Keywords(OMSVECTORRESHAPER_KEYWORDS)
@Label(OMSVECTORRESHAPER_LABEL)
@Name("_" + OMSVECTORRESHAPER_NAME)
@Status(OMSVECTORRESHAPER_STATUS)
@License(OMSVECTORRESHAPER_LICENSE)
public class VectorReshaper extends JGTModel {

    @Description(OMSVECTORRESHAPER_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector;

    @Description(OMSVECTORRESHAPER_pCql_DESCRIPTION)
    @UI(JGTConstants.MULTILINE_UI_HINT + "5")
    @In
    public String pCql = null;

    @Description(OMSVECTORRESHAPER_pRemove_DESCRIPTION)
    @In
    public String pRemove = null;

    @Description(OMSVECTORRESHAPER_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
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
