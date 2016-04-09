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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_FILE_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_IN_VECTOR_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSVECTORWRITER_P_TYPE_DESCRIPTION;

import java.io.IOException;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSVECTORWRITER_DESCRIPTION)
@Author(name = OMSVECTORWRITER_AUTHORNAMES, contact = OMSVECTORWRITER_AUTHORCONTACTS)
@Keywords(OMSVECTORWRITER_KEYWORDS)
@Label(OMSVECTORWRITER_LABEL)
@Name("_" + OMSVECTORWRITER_NAME)
@Status(OMSVECTORWRITER_STATUS)
@License(OMSVECTORWRITER_LICENSE)
public class VectorWriter extends JGTModel {

    @Description(OMSVECTORWRITER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OMSVECTORWRITER_P_TYPE_DESCRIPTION)
    @In
    // currently not used, for future compatibility
    public String pType = null;

    @Description(OMSVECTORWRITER_FILE_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Execute
    public void process() throws IOException {
        OmsVectorWriter vectorwriter = new OmsVectorWriter();
        vectorwriter.inVector = inVector;
        vectorwriter.pType = pType;
        vectorwriter.file = file;
        vectorwriter.pm = pm;
        vectorwriter.doProcess = doProcess;
        vectorwriter.doReset = doReset;
        vectorwriter.process();
    }

    public static void writeVector( String path, SimpleFeatureCollection featureCollection ) throws IOException {
        OmsVectorWriter.writeVector(path, featureCollection);
    }
}
