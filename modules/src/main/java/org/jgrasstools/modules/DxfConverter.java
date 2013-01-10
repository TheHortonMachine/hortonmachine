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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_lineVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_pCode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_pointsVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSDXFCONVERTER_polygonVector_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.vectorconverter.OmsDxfConverter;

@Description(OMSDXFCONVERTER_DESCRIPTION)
@Documentation(OMSDXFCONVERTER_DOCUMENTATION)
@Author(name = OMSDXFCONVERTER_AUTHORNAMES, contact = OMSDXFCONVERTER_AUTHORCONTACTS)
@Keywords(OMSDXFCONVERTER_KEYWORDS)
@Label(OMSDXFCONVERTER_LABEL)
@Name("_" + OMSDXFCONVERTER_NAME)
@Status(OMSDXFCONVERTER_STATUS)
@License(OMSDXFCONVERTER_LICENSE)
public class DxfConverter extends JGTModel {

    @Description(OMSDXFCONVERTER_file_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description(OMSDXFCONVERTER_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSDXFCONVERTER_pointsVector_DESCRIPTION)
    @Out
    public String pointsVector = null;

    @Description(OMSDXFCONVERTER_lineVector_DESCRIPTION)
    @Out
    public String lineVector = null;

    @Description(OMSDXFCONVERTER_polygonVector_DESCRIPTION)
    @Out
    public String polygonVector = null;

    @Execute
    public void readFeatureCollection() throws Exception {
        OmsDxfConverter dxfconverter = new OmsDxfConverter();
        dxfconverter.file = file;
        dxfconverter.pCode = pCode;
        dxfconverter.pm = pm;
        dxfconverter.doProcess = doProcess;
        dxfconverter.doReset = doReset;
        dxfconverter.readFeatureCollection();
        dumpVector(dxfconverter.pointsVector, pointsVector);
        dumpVector(dxfconverter.lineVector, lineVector);
        dumpVector(dxfconverter.polygonVector, polygonVector);
    }

}
