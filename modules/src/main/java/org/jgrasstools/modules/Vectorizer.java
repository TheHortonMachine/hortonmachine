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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_doRegioncheck_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_fDefault_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_pThres_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORIZER_pValue_DESCRIPTION;
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
import org.jgrasstools.gears.modules.v.vectorize.OmsVectorizer;

@Description(OMSVECTORIZER_DESCRIPTION)
@Documentation(OMSVECTORIZER_DOCUMENTATION)
@Author(name = OMSVECTORIZER_AUTHORNAMES, contact = OMSVECTORIZER_AUTHORCONTACTS)
@Keywords(OMSVECTORIZER_KEYWORDS)
@Label(OMSVECTORIZER_LABEL)
@Name("_" + OMSVECTORIZER_NAME)
@Status(OMSVECTORIZER_STATUS)
@License(OMSVECTORIZER_LICENSE)
public class Vectorizer extends JGTModel {

    @Description(OMSVECTORIZER_inRaster_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster;

    @Description(OMSVECTORIZER_pValue_DESCRIPTION)
    @In
    public Double pValue = null;

    @Description(OMSVECTORIZER_fDefault_DESCRIPTION)
    @In
    public String fDefault = "value";

    @Description(OMSVECTORIZER_pThres_DESCRIPTION)
    @In
    public double pThres = 0;

    @Description(OMSVECTORIZER_doRegioncheck_DESCRIPTION)
    @In
    public boolean doRegioncheck = false;

    @Description(OMSVECTORIZER_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outVector = null;

    @Execute
    public void process() throws Exception {
        OmsVectorizer vectorizer = new OmsVectorizer();
        vectorizer.inRaster = getRaster(inRaster);
        vectorizer.pValue = pValue;
        vectorizer.fDefault = fDefault;
        vectorizer.pThres = pThres;
        vectorizer.doRegioncheck = doRegioncheck;
        vectorizer.pm = pm;
        vectorizer.doProcess = doProcess;
        vectorizer.doReset = doReset;
        vectorizer.process();
        dumpVector(vectorizer.outVector, outVector);
    }
}
