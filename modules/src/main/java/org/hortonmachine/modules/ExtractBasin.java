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

import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_KEYWORDS;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_LABEL;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_LICENSE;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_NAME;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_STATUS;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_doSmoothing_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_inNetwork_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_outArea_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_outBasin_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_outOutlet_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_outVectorBasin_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_pEast_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_pNorth_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin.OMSEXTRACTBASIN_pSnapbuffer_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSEXTRACTBASIN_DESCRIPTION)
@Author(name = OMSEXTRACTBASIN_AUTHORNAMES, contact = OMSEXTRACTBASIN_AUTHORCONTACTS)
@Keywords(OMSEXTRACTBASIN_KEYWORDS)
@Label(OMSEXTRACTBASIN_LABEL)
@Name("_" + OMSEXTRACTBASIN_NAME)
@Status(OMSEXTRACTBASIN_STATUS)
@License(OMSEXTRACTBASIN_LICENSE)
public class ExtractBasin extends HMModel {
    @Description(OMSEXTRACTBASIN_pNorth_DESCRIPTION)
    @UI(HMConstants.NORTHING_UI_HINT)
    @In
    public double pNorth = -1.0;

    @Description(OMSEXTRACTBASIN_pEast_DESCRIPTION)
    @UI(HMConstants.EASTING_UI_HINT)
    @In
    public double pEast = -1.0;

    @Description(OMSEXTRACTBASIN_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow;

    @Description(OMSEXTRACTBASIN_inNetwork_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNetwork;

    @Description(OMSEXTRACTBASIN_pSnapbuffer_DESCRIPTION)
    @In
    public double pSnapbuffer = 200;

    @Description(OMSEXTRACTBASIN_doSmoothing_DESCRIPTION)
    @In
    public boolean doSmoothing = false;

    @Description(OMSEXTRACTBASIN_outArea_DESCRIPTION)
    @Out
    public double outArea = 0;

    @Description(OMSEXTRACTBASIN_outBasin_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBasin = null;

    @Description(OMSEXTRACTBASIN_outOutlet_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outOutlet = null;

    @Description(OMSEXTRACTBASIN_outVectorBasin_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVectorBasin = null;

    @Execute
    public void process() throws Exception {
        OmsExtractBasin extractbasin = new OmsExtractBasin();
        extractbasin.pNorth = pNorth;
        extractbasin.pEast = pEast;
        extractbasin.inFlow = getRaster(inFlow);
        extractbasin.inNetwork = getVector(inNetwork);
        extractbasin.pSnapbuffer = pSnapbuffer;
        boolean doVector = false;
        if (outVectorBasin != null) {
            doVector = true;
        }
        extractbasin.doVector = doVector;
        extractbasin.doSmoothing = doSmoothing;
        extractbasin.pm = pm;
        extractbasin.doProcess = doProcess;
        extractbasin.doReset = doReset;
        extractbasin.process();
        dumpRaster(extractbasin.outBasin, outBasin);
        dumpVector(extractbasin.outOutlet, outOutlet);
        dumpVector(extractbasin.outVectorBasin, outVectorBasin);
        outArea = extractbasin.outArea;
        pm.message("Basin Area = " + extractbasin.outArea);
    }
}
