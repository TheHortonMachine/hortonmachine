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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.BIBLIO;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.DESCRIPTION;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.KEYWORDS;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.LABEL;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.NAME;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.inFlow_DESCR;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.inNet_DESCR;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.inSlope_DESCR;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.inWeights_DESCR;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.outConnectivity_DESCR;
import static org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity.pWeight_DESCR;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name("_" + NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@Bibliography(BIBLIO)
public class DownSlopeConnectivity extends HMModel {
    @Description(inFlow_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow;

    @Description(inNet_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet;

    @Description(inSlope_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("m/m")
    @In
    public String inSlope;

    @Description(inWeights_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inWeights;

    @Description(pWeight_DESCR)
    @In
    public Double pWeight;

    @Description(outConnectivity_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outConnectivity = null;

    @Execute
    public void process() throws Exception {
        OmsDownSlopeConnectivity odsc = new OmsDownSlopeConnectivity();
        odsc.inFlow = getRaster(inFlow);
        odsc.inNet = getRaster(inNet);
        odsc.inSlope = getRaster(inSlope);
        odsc.inWeights = getRaster(inWeights);
        odsc.pWeight = pWeight;
        odsc.process();
        dumpRaster(odsc.outConnectivity, outConnectivity);
    }
    

}
