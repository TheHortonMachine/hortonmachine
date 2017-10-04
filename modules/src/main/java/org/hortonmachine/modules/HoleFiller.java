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

import static org.hortonmachine.gears.libs.modules.Variables.IDW;
import static org.hortonmachine.gears.libs.modules.Variables.TPS;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_IN_ROI_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_LABEL;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_LICENSE;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_MODE_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_NAME;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_P_BUFFER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller.OMSHOLEFILLER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller;

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
import oms3.annotations.Unit;

@Description(OMSHOLEFILLER_DESCRIPTION)
@Documentation(OMSHOLEFILLER_DOCUMENTATION)
@Author(name = OMSHOLEFILLER_AUTHORNAMES, contact = OMSHOLEFILLER_AUTHORCONTACTS)
@Keywords(OMSHOLEFILLER_KEYWORDS)
@Label(OMSHOLEFILLER_LABEL)
@Name(OMSHOLEFILLER_NAME)
@Status(OMSHOLEFILLER_STATUS)
@License(OMSHOLEFILLER_LICENSE)
public class HoleFiller extends HMModel {

    @Description(OMSHOLEFILLER_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OMSHOLEFILLER_IN_ROI_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inROI;

    @Description(OMSHOLEFILLER_MODE_DESCRIPTION)
    @UI("combo:" + TPS + "," + IDW)
    @In
    public String pMode = TPS;

    @Description(OMSHOLEFILLER_P_BUFFER_DESCRIPTION)
    @Unit("m")
    @In
    public double pBuffer = 4.0;

    @Description(OMSHOLEFILLER_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {

        OmsHoleFiller hf = new OmsHoleFiller();
        hf.inRaster = getRaster(inRaster);
        hf.inROI = getVector(inROI);
        hf.pMode = pMode;
        hf.pBuffer = pBuffer;
        hf.pm = pm;
        hf.doProcess = doProcess;
        hf.doReset = doReset;
        hf.process();
        dumpRaster(hf.outRaster, outRaster);
    }

}
