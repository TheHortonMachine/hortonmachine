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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_fNew_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCATTOFEATUREATTRIBUTE_pPos_DESCRIPTION;
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
import org.jgrasstools.gears.modules.v.rastercattofeatureattribute.OmsRasterCatToFeatureAttribute;

@Description(OMSRASTERCATTOFEATUREATTRIBUTE_DESCRIPTION)
@Documentation(OMSRASTERCATTOFEATUREATTRIBUTE_DOCUMENTATION)
@Author(name = OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORNAMES, contact = OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORCONTACTS)
@Keywords(OMSRASTERCATTOFEATUREATTRIBUTE_KEYWORDS)
@Label(OMSRASTERCATTOFEATUREATTRIBUTE_LABEL)
@Name("_" + OMSRASTERCATTOFEATUREATTRIBUTE_NAME)
@Status(OMSRASTERCATTOFEATUREATTRIBUTE_STATUS)
@License(OMSRASTERCATTOFEATUREATTRIBUTE_LICENSE)
public class RasterCatToFeatureAttribute extends JGTModel {

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_inRaster_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster;

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector = null;

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_fNew_DESCRIPTION)
    @In
    public String fNew = "new";

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_pPos_DESCRIPTION)
    @In
    public String pPos = "middle";

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_outVector_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outVector = null;

    @Execute
    public void process() throws Exception {
        OmsRasterCatToFeatureAttribute rastercattofeatureattribute = new OmsRasterCatToFeatureAttribute();
        rastercattofeatureattribute.inRaster = getRaster(inRaster);
        rastercattofeatureattribute.inVector = getVector(inVector);
        rastercattofeatureattribute.fNew = fNew;
        rastercattofeatureattribute.pPos = pPos;
        rastercattofeatureattribute.pm = pm;
        rastercattofeatureattribute.doProcess = doProcess;
        rastercattofeatureattribute.doReset = doReset;
        rastercattofeatureattribute.process();
        dumpVector(rastercattofeatureattribute.outVector, outVector);
    }
}
