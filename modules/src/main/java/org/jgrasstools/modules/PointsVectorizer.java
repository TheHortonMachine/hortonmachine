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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_F_DEFAULT_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_IN_RASTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_OUT_VECTOR_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.vectorize.OmsPointsVectorizer;

@Description(OMSPOINTSVECTORIZER_DESCRIPTION)
@Author(name = OMSPOINTSVECTORIZER_AUTHORNAMES, contact = OMSPOINTSVECTORIZER_AUTHORCONTACTS)
@Keywords(OMSPOINTSVECTORIZER_KEYWORDS)
@Label(OMSPOINTSVECTORIZER_LABEL)
@Name("_" + OMSPOINTSVECTORIZER_NAME)
@Status(OMSPOINTSVECTORIZER_STATUS)
@License(OMSPOINTSVECTORIZER_LICENSE)
public class PointsVectorizer extends JGTModel {

    @Description(OMSPOINTSVECTORIZER_IN_RASTER_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster;

    @Description(OMSPOINTSVECTORIZER_F_DEFAULT_DESCRIPTION)
    @In
    public String fDefault = "value";

    @Description(OMSPOINTSVECTORIZER_OUT_VECTOR_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outVector = null;

    @Execute
    public void process() throws Exception {
        OmsPointsVectorizer pointsvectorizer = new OmsPointsVectorizer();
        pointsvectorizer.inRaster = getRaster(inRaster);
        pointsvectorizer.fDefault = fDefault;
        pointsvectorizer.pm = pm;
        pointsvectorizer.doProcess = doProcess;
        pointsvectorizer.doReset = doReset;
        pointsvectorizer.process();
        dumpVector(pointsvectorizer.outVector, outVector);
    }
}
