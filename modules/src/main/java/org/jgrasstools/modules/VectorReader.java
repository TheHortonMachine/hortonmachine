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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORREADER_outVector_DESCRIPTION;

import java.io.IOException;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSVECTORREADER_DESCRIPTION)
@Documentation(OMSVECTORREADER_DOCUMENTATION)
@Author(name = OMSVECTORREADER_AUTHORNAMES, contact = OMSVECTORREADER_AUTHORCONTACTS)
@Keywords(OMSVECTORREADER_KEYWORDS)
@Label(OMSVECTORREADER_LABEL)
@Name("_" + OMSVECTORREADER_NAME)
@Status(OMSVECTORREADER_STATUS)
@License(OMSVECTORREADER_LICENSE)
public class VectorReader extends JGTModel {

    @Description(OMSVECTORREADER_file_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description(OMSVECTORREADER_outVector_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector = null;

    @Execute
    public void process() throws IOException {
        OmsVectorReader vectorreader = new OmsVectorReader();
        vectorreader.file = file;
        vectorreader.pm = pm;
        vectorreader.doProcess = doProcess;
        vectorreader.doReset = doReset;
        vectorreader.process();
        outVector = vectorreader.outVector;
    }
}
