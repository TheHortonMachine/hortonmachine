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

import static org.hortonmachine.gears.libs.modules.Variables.AVERAGING;
import static org.hortonmachine.gears.libs.modules.Variables.CATEGORIES;
import static org.hortonmachine.gears.libs.modules.Variables.IDW;
import static org.hortonmachine.gears.libs.modules.Variables.LDW;
import static org.hortonmachine.gears.libs.modules.Variables.TPS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_IN_RASTERMASK_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_LABEL;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_LICENSE;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_NAME;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_P_MODE_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_STATUS;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_doUseOnlyBorderValues_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_pMaxDistance_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller.OMSRASTERNULLFILLER_pMinDistance_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller;

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

@Description(OMSRASTERNULLFILLER_DESCRIPTION)
@Documentation(OMSRASTERNULLFILLER_DOCUMENTATION)
@Author(name = OMSRASTERNULLFILLER_AUTHORNAMES, contact = OMSRASTERNULLFILLER_AUTHORCONTACTS)
@Keywords(OMSRASTERNULLFILLER_KEYWORDS)
@Label(OMSRASTERNULLFILLER_LABEL)
@Name(OMSRASTERNULLFILLER_NAME)
@Status(OMSRASTERNULLFILLER_STATUS)
@License(OMSRASTERNULLFILLER_LICENSE)
public class RasterMissingValuesFiller extends HMModel {

    @Description(OMSRASTERNULLFILLER_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OMSRASTERNULLFILLER_IN_RASTERMASK_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inMask;

    @Description(OMSRASTERNULLFILLER_pMinDistance_DESCRIPTION)
    @In
    public int pMinDistance = 0;

    @Description(OMSRASTERNULLFILLER_pMaxDistance_DESCRIPTION)
    @In
    public int pMaxDistance = 10;

    @Description(OMSRASTERNULLFILLER_doUseOnlyBorderValues_DESCRIPTION)
    @In
    public boolean doUseOnlyBorderValues = false;

    @Description(OMSRASTERNULLFILLER_P_MODE_DESCRIPTION)
    @UI("combo:" + IDW + "," + LDW + "," + TPS + "," + AVERAGING + "," + CATEGORIES)
    @In
    public String pMode = IDW;

    @Description(OMSRASTERNULLFILLER_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsRasterMissingValuesFiller ormvf = new OmsRasterMissingValuesFiller();
        ormvf.pm = pm;
        ormvf.inRaster = getRaster(inRaster);
        ormvf.inMask = getRaster(inMask);
        ormvf.pMinDistance = pMinDistance;
        ormvf.pMaxDistance = pMaxDistance;
        ormvf.doUseOnlyBorderValues = doUseOnlyBorderValues;
        ormvf.pMode = pMode;
        ormvf.process();
        dumpRaster(ormvf.outRaster, outRaster);
    }
}
