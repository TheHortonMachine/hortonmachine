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

import static org.hortonmachine.gears.libs.modules.Variables.TCA;
import static org.hortonmachine.gears.libs.modules.Variables.TCA_CONVERGENT;
import static org.hortonmachine.gears.libs.modules.Variables.TCA_SLOPE;
import static org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;

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

@Description(OMSEXTRACTNETWORK_DESCRIPTION)
@Author(name = OMSEXTRACTNETWORK_AUTHORNAMES, contact = OMSEXTRACTNETWORK_AUTHORCONTACTS)
@Keywords(OMSEXTRACTNETWORK_KEYWORDS)
@Label(OMSEXTRACTNETWORK_LABEL)
@Name("_" + OMSEXTRACTNETWORK_NAME)
@Status(OMSEXTRACTNETWORK_STATUS)
@License(OMSEXTRACTNETWORK_LICENSE)
public class ExtractNetwork extends HMModel {

    @Description(OMSEXTRACTNETWORK_inTca_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTca = null;

    @Description(OMSEXTRACTNETWORK_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSEXTRACTNETWORK_inSlope_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSlope = null;

    @Description(OMSEXTRACTNETWORK_inTc3_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTc3 = null;

    @Description(OMSEXTRACTNETWORK_pThres_DESCRIPTION)
    @In
    public int pThres = 0;

    @Description(OMSEXTRACTNETWORK_pMode_DESCRIPTION)
    @UI("combo:" + TCA + "," + TCA_SLOPE + "," + TCA_CONVERGENT)
    @In
    public String pMode = TCA;

    @Description(OMSEXTRACTNETWORK_pExp_DESCRIPTION)
    @In
    public double pExp = 0.5;

    @Description(OMSEXTRACTNETWORK_outNet_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNet = null;

    @Execute
    public void process() throws Exception {
        OmsExtractNetwork extractnetwork = new OmsExtractNetwork();
        extractnetwork.inTca = getRaster(inTca);
        extractnetwork.inFlow = getRaster(inFlow);
        extractnetwork.inSlope = getRaster(inSlope);
        extractnetwork.inTc3 = getRaster(inTc3);
        extractnetwork.pThres = pThres;
        extractnetwork.pMode = pMode;
        extractnetwork.pExp = pExp;
        extractnetwork.pm = pm;
        extractnetwork.doProcess = doProcess;
        extractnetwork.doReset = doReset;
        extractnetwork.process();
        dumpRaster(extractnetwork.outNet, outNet);
    }
}
