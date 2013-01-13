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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_fId_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_fNewId_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_inMap_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_inPoints_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_outMap_DESCRIPTION;
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
import org.jgrasstools.gears.modules.v.polygonize.OmsLinesPolygonizer;

@Description(OMSLINESPOLYGONIZER_DESCRIPTION)
@Documentation(OMSLINESPOLYGONIZER_DOCUMENTATION)
@Author(name = OMSLINESPOLYGONIZER_AUTHORNAMES, contact = OMSLINESPOLYGONIZER_AUTHORCONTACTS)
@Keywords(OMSLINESPOLYGONIZER_KEYWORDS)
@Label(OMSLINESPOLYGONIZER_LABEL)
@Name("_" + OMSLINESPOLYGONIZER_NAME)
@Status(OMSLINESPOLYGONIZER_STATUS)
@License(OMSLINESPOLYGONIZER_LICENSE)
public class LinesPolygonizer extends JGTModel {

    @Description(OMSLINESPOLYGONIZER_inMap_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap = null;

    @Description(OMSLINESPOLYGONIZER_inPoints_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inPoints = null;

    @Description(OMSLINESPOLYGONIZER_fId_DESCRIPTION)
    @In
    public String fId = null;

    @Description(OMSLINESPOLYGONIZER_fNewId_DESCRIPTION)
    @In
    public String fNewId = "id";

    @Description(OMSLINESPOLYGONIZER_outMap_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsLinesPolygonizer omslinespolygonizer = new OmsLinesPolygonizer();
        omslinespolygonizer.inMap = getVector(inMap);
        omslinespolygonizer.inPoints = getVector(inPoints);
        omslinespolygonizer.fId = fId;
        omslinespolygonizer.fNewId = fNewId;
        omslinespolygonizer.pm = pm;
        omslinespolygonizer.doProcess = doProcess;
        omslinespolygonizer.doReset = doReset;
        omslinespolygonizer.process();
        dumpVector(omslinespolygonizer.outMap, outMap);
    }
}
