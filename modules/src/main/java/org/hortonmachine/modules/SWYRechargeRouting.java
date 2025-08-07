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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYRechargeRouting.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYRechargeRouting;

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

@Description(OmsSWYRechargeRouting.DESCRIPTION)
@Author(name = OmsSWYRechargeRouting.AUTHORNAMES, contact = OmsSWYRechargeRouting.AUTHORCONTACTS)
@Keywords(OmsSWYRechargeRouting.KEYWORDS)
@Label(OmsSWYRechargeRouting.LABEL)
@Name(OmsSWYRechargeRouting.NAME)
@Status(OmsSWYRechargeRouting.STATUS)
@License(OmsSWYRechargeRouting.LICENSE)
public class SWYRechargeRouting extends HMModel {
    @Description(inPet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inPet = null;

    @Description(inRainfall_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRainfall;

    @Description(inFlowdirections_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlowdirections = null;

    @Description(inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(inRunoff_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRunoff = null;

    @Description(pAlpha_DESCRIPTION)
    @In
    public double pAlpha = 1.0;

    @Description(pBeta_DESCRIPTION)
    @In
    public double pBeta = 1.0;

    @Description(pGamma_DESCRIPTION)
    @In
    public double pGamma = 1.0;

    @Description(outAet_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outAet = null;

    @Description(outLsumAvailable_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outLsumAvailable = null;

    @Description(outNetInfiltration_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetInfiltration = null;

    @Description(outInfiltration_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outInfiltration = null;

    @Execute
    public void process() throws Exception {
        OmsSWYRechargeRouting inf = new OmsSWYRechargeRouting();
        inf.inPet = getRaster(inPet);
        inf.inRainfall = getRaster(inRainfall);
        inf.inFlowdirections = getRaster(inFlowdirections);
        inf.inNet = getRaster(inNet);
        inf.inRunoff = getRaster(inRunoff);
        inf.pAlpha = pAlpha;
        inf.pBeta = pBeta;
        inf.pGamma = pGamma;
        inf.pm = pm;
        inf.process();

        dumpRaster(inf.outRecharge, outInfiltration);
        dumpRaster(inf.outAvailableRecharge, outNetInfiltration);
        dumpRaster(inf.outAet, outAet);
        dumpRaster(inf.outUpslopeSubsidy, outLsumAvailable);
    }
}