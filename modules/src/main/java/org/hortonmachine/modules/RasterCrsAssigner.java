/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rastercrsassigner.OmsRasterCrsAssigner;

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

@Description(OmsRasterCrsAssigner.DESCRIPTION)
@Documentation(OmsRasterCrsAssigner.DOCUMENTATION)
@Author(name = OmsRasterCrsAssigner.AUTHORNAMES, contact = OmsRasterCrsAssigner.AUTHORCONTACTS)
@Keywords(OmsRasterCrsAssigner.KEYWORDS)
@Label(OmsRasterCrsAssigner.LABEL)
@Name("_" + OmsRasterCrsAssigner.NAME)
@Status(OmsRasterCrsAssigner.STATUS)
@License(OmsRasterCrsAssigner.LICENSE)
public class RasterCrsAssigner extends HMModel {

    @Description(OmsRasterCrsAssigner.inRaster_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OmsRasterCrsAssigner.pCode_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OmsRasterCrsAssigner.outRaster_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsRasterCrsAssigner assigner = new OmsRasterCrsAssigner();
        assigner.inRaster = inRaster;
        assigner.pCode = pCode;
        assigner.outRaster = outRaster;
        assigner.pm = pm;
        assigner.doProcess = doProcess;
        assigner.doReset = doReset;
        assigner.process();
    }
}
