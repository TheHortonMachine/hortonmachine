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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_attributesVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_contourVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_lineVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_pCode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_pointsVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_polygonVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDWGCONVERTER_textVector_DESCRIPTION;
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
import org.jgrasstools.gears.modules.v.vectorconverter.OmsDwgConverter;

@Description(OMSDWGCONVERTER_DESCRIPTION)
@Author(name = OMSDWGCONVERTER_AUTHORNAMES, contact = OMSDWGCONVERTER_AUTHORCONTACTS)
@Keywords(OMSDWGCONVERTER_KEYWORDS)
@Label(OMSDWGCONVERTER_LABEL)
@Name("_" + OMSDWGCONVERTER_NAME)
@Status(OMSDWGCONVERTER_STATUS)
@License(OMSDWGCONVERTER_LICENSE)
public class DwgConverter extends JGTModel {

    @Description(OMSDWGCONVERTER_file_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description(OMSDWGCONVERTER_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSDWGCONVERTER_pointsVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String pointsVector = null;

    @Description(OMSDWGCONVERTER_lineVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String lineVector = null;

    @Description(OMSDWGCONVERTER_polygonVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String polygonVector = null;

    @Description(OMSDWGCONVERTER_textVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String textVector;

    @Description(OMSDWGCONVERTER_attributesVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String attributesVector;

    @Description(OMSDWGCONVERTER_contourVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String contourVector;

    @Execute
    public void readFeatureCollection() throws Exception {
        OmsDwgConverter dwgconverter = new OmsDwgConverter();
        dwgconverter.file = file;
        dwgconverter.pCode = pCode;
        dwgconverter.pm = pm;
        dwgconverter.doProcess = doProcess;
        dwgconverter.doReset = doReset;
        dwgconverter.readFeatureCollection();
        dumpVector(dwgconverter.pointsVector, pointsVector);
        dumpVector(dwgconverter.lineVector, lineVector);
        dumpVector(dwgconverter.polygonVector, polygonVector);
        dumpVector(dwgconverter.textVector, textVector);
        dumpVector(dwgconverter.attributesVector, attributesVector);
        dumpVector(dwgconverter.contourVector, contourVector);
    }

}
