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

import static org.hortonmachine.gears.modules.v.rastercattofeatureattribute.OmsRasterCatToFeatureAttribute.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.rastercattofeatureattribute.OmsRasterCatToFeatureAttribute;

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

@Description(OMSRASTERCATTOFEATUREATTRIBUTE_DESCRIPTION)
@Author(name = OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORNAMES, contact = OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORCONTACTS)
@Keywords(OMSRASTERCATTOFEATUREATTRIBUTE_KEYWORDS)
@Label(OMSRASTERCATTOFEATUREATTRIBUTE_LABEL)
@Name("_" + OMSRASTERCATTOFEATUREATTRIBUTE_NAME)
@Status(OMSRASTERCATTOFEATUREATTRIBUTE_STATUS)
@License(OMSRASTERCATTOFEATUREATTRIBUTE_LICENSE)
public class RasterCatToFeatureAttribute extends HMModel {

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector = null;

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_F_NEW_DESCRIPTION)
    @In
    public String fNew = "new";

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_P_POS_DESCRIPTION)
    @In
    public String pPos = "middle";

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
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
