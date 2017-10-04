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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inConnectivity_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inDsm_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inDtm_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inFlow_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inInundationArea_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inNet_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inStand_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inTca_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.outBasins_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.outNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.outNetnum_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.pConnectivityThreshold_DESCR;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator;

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

@Description(OmsLW10_CHM_AreaToNetpointAssociator.DESCRIPTION)
@Author(name = OmsLW10_CHM_AreaToNetpointAssociator.AUTHORS, contact = OmsLW10_CHM_AreaToNetpointAssociator.CONTACTS)
@Keywords(OmsLW10_CHM_AreaToNetpointAssociator.KEYWORDS)
@Label(OmsLW10_CHM_AreaToNetpointAssociator.LABEL)
@Name("_" + OmsLW10_CHM_AreaToNetpointAssociator.NAME)
@Status(OmsLW10_CHM_AreaToNetpointAssociator.STATUS)
@License(OmsLW10_CHM_AreaToNetpointAssociator.LICENSE)
public class LW10_CHM_AreaToNetpointAssociator extends HMModel {

    @Description(inNetPoints_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNetPoints = null;

    @Description(inInundationArea_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inInundationArea = null;

    @Description(inFlow_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(inTca_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTca = null;

    @Description(inNet_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(inDtm_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm = null;

    @Description(inDsm_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDsm = null;

    @Description(inStand_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inStand = null;

    @Description(inConnectivity_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inConnectivity = null;

    @Description(pConnectivityThreshold_DESCR)
    @In
    public double pConnectivityThreshold = 4.0;

    @Description(outNetPoints_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Description(outNetnum_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetnum = null;

    @Description(outBasins_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBasins = null;

    @Execute
    public void process() throws Exception {
        OmsLW10_CHM_AreaToNetpointAssociator npa = new OmsLW10_CHM_AreaToNetpointAssociator();
        npa.inNetPoints = getVector(inNetPoints);
        npa.pm = pm;
        npa.inInundationArea = getVector(inInundationArea);
        npa.inFlow = getRaster(inFlow);
        npa.inTca = getRaster(inTca);
        npa.inNet = getRaster(inNet);
        npa.inDtm = getRaster(inDtm);
        npa.inDsm = getRaster(inDsm);
        npa.inStand = getRaster(inStand);
        npa.inConnectivity = getRaster(inConnectivity);
        npa.pConnectivityThreshold = pConnectivityThreshold;
        npa.process();

        dumpVector(npa.outNetPoints, outNetPoints);
        dumpRaster(npa.outNetnum, outNetnum);
        dumpRaster(npa.outBasins, outBasins);
    }

}
