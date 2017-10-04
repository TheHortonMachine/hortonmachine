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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.inConnectivity_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.inFlow_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.inInundationArea_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.inNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.inNet_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.inTca_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.inTreePoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.outBasins_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.outNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.outNetnum_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.outTreePoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.pAllometricCoeff1stOrder_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.pAllometricCoeff2ndOrder_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.pAllometricCoeffVolume_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.pConnectivityThreshold_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.pFlexibleDiameterLimit_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.pRepresentingHeightDbhPercentile_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator.pTreeTaper_DESCR;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_SingleTree_AreaToNetpointAssociator;

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
import oms3.annotations.Unit;

@Description(OmsLW10_SingleTree_AreaToNetpointAssociator.DESCRIPTION)
@Author(name = OmsLW10_SingleTree_AreaToNetpointAssociator.AUTHORS, contact = OmsLW10_SingleTree_AreaToNetpointAssociator.CONTACTS)
@Keywords(OmsLW10_SingleTree_AreaToNetpointAssociator.KEYWORDS)
@Label(OmsLW10_SingleTree_AreaToNetpointAssociator.LABEL)
@Name("_" + OmsLW10_SingleTree_AreaToNetpointAssociator.NAME)
@Status(OmsLW10_SingleTree_AreaToNetpointAssociator.STATUS)
@License(OmsLW10_SingleTree_AreaToNetpointAssociator.LICENSE)
public class LW10_SingleTree_AreaToNetpointAssociator extends HMModel {

    @Description(inNetPoints_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNetPoints = null;

    @Description(inTreePoints_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inTreePoints = null;

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

    @Description(inConnectivity_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inConnectivity = null;

    @Description(pConnectivityThreshold_DESCR)
    @In
    public double pConnectivityThreshold = 4.0;

    @Description(pAllometricCoeff2ndOrder_DESCR)
    @In
    public double pAllometricCoeff2ndOrder = 0.0096;

    @Description(pAllometricCoeff1stOrder_DESCR)
    @In
    public double pAllometricCoeff1stOrder = 1.298;

    @Description(pAllometricCoeffVolume_DESCR)
    @In
    public double pAllometricCoeffVolume = 0.0000368048;

    @Description(pRepresentingHeightDbhPercentile_DESCR)
    @In
    public int pRepresentingHeightDbhPercentile = 50;

    @Description(pTreeTaper_DESCR)
    @Unit("cm/m")
    @In
    public double pTreeTaper = 3.0;

    @Description(pFlexibleDiameterLimit_DESCR)
    @Unit("cm")
    @In
    public double pFlexibleDiameterLimit = 5.0;

    @Description(outNetPoints_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Description(outNetnum_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetnum = null;

    @Description(outTreePoints_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTreePoints = null;

    @Description(outBasins_DESCR)

    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBasins = null;

    @Execute
    public void process() throws Exception {
        OmsLW10_SingleTree_AreaToNetpointAssociator ex = new OmsLW10_SingleTree_AreaToNetpointAssociator();
        ex.inNetPoints = getVector(inNetPoints);
        ex.inTreePoints = getVector(inTreePoints);
        ex.inInundationArea = getVector(inInundationArea);
        ex.inFlow = getRaster(inFlow);
        ex.inTca = getRaster(inTca);
        ex.inNet = getRaster(inNet);
        ex.inConnectivity = getRaster(inConnectivity);
        ex.pConnectivityThreshold = pConnectivityThreshold;
        ex.pAllometricCoeff2ndOrder = pAllometricCoeff2ndOrder;
        ex.pAllometricCoeff1stOrder = pAllometricCoeff1stOrder;
        ex.pAllometricCoeffVolume = pAllometricCoeffVolume;
        ex.pRepresentingHeightDbhPercentile = pRepresentingHeightDbhPercentile;
        ex.pTreeTaper = pTreeTaper;
        ex.pFlexibleDiameterLimit = pFlexibleDiameterLimit;
        ex.pm = pm;
        ex.process();
        dumpVector(ex.outNetPoints, outNetPoints);
        dumpVector(ex.outTreePoints, outTreePoints);
        dumpRaster(ex.outNetnum, outNetnum);
        dumpRaster(ex.outBasins, outBasins);
    }

}
