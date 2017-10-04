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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_LINE_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_POINTS_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_POLYGON_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_P_CODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDXFCONVERTER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectorconverter.OmsDxfConverter;

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

@Description(OMSDXFCONVERTER_DESCRIPTION)
@Author(name = OMSDXFCONVERTER_AUTHORNAMES, contact = OMSDXFCONVERTER_AUTHORCONTACTS)
@Keywords(OMSDXFCONVERTER_KEYWORDS)
@Label(OMSDXFCONVERTER_LABEL)
@Name("_" + OMSDXFCONVERTER_NAME)
@Status(OMSDXFCONVERTER_STATUS)
@License(OMSDXFCONVERTER_LICENSE)
public class DxfConverter extends HMModel {

    @Description(OMSDXFCONVERTER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String file = null;

    @Description(OMSDXFCONVERTER_P_CODE_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSDXFCONVERTER_POINTS_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String pointsVector = null;

    @Description(OMSDXFCONVERTER_LINE_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String lineVector = null;

    @Description(OMSDXFCONVERTER_POLYGON_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String polygonVector = null;

    @Execute
    public void process() throws Exception {
        OmsDxfConverter dxfconverter = new OmsDxfConverter();
        dxfconverter.file = file;
        dxfconverter.pCode = pCode;
        dxfconverter.pm = pm;
        dxfconverter.doProcess = doProcess;
        dxfconverter.doReset = doReset;
        dxfconverter.process();
        dumpVector(dxfconverter.pointsVector, pointsVector);
        dumpVector(dxfconverter.lineVector, lineVector);
        dumpVector(dxfconverter.polygonVector, polygonVector);
    }
}
