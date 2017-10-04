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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.AUTHORS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.CONTACTS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.KEYWORDS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.LABEL;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.LICENSE;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.NAME;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.STATUS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.bridgeLenghtField_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.inBridges_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.inDams_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.inNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.outNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.outProblemBridges_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.pBridgesOnNetDistance_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.pDamsOnNetDistance_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder.pFixedDamsWidth_DESCR;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW05_BridgesDamsWidthAdder;

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

@Description(DESCRIPTION)
@Author(name = AUTHORS, contact = CONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class LW05_BridgesDamsWidthAdder extends HMModel {
    @Description(inNetPoints_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNetPoints = null;

    @Description(inBridges_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inBridges = null;

    @Description(inDams_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inDams = null;

    @Description(pDamsOnNetDistance_DESCR)
    @Unit("m")
    @In
    public double pDamsOnNetDistance = 15.0;

    @Description(pBridgesOnNetDistance_DESCR)
    @Unit("m")
    @In
    public double pBridgesOnNetDistance = 15.0;

    @Description(pFixedDamsWidth_DESCR)
    @Unit("m")
    @In
    public double pFixedDamsWidth = 0.1;

    @Description(bridgeLenghtField_DESCR)
    @In
    public String fBridgeLenght = "LENGHT";

    @Description(outNetPoints_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Description(outProblemBridges_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outProblemBridges = null;

    @Execute
    public void process() throws Exception {
        OmsLW05_BridgesDamsWidthAdder m = new OmsLW05_BridgesDamsWidthAdder();
        m.inNetPoints = getVector(inNetPoints);
        m.inBridges = getVector(inBridges);
        m.inDams = getVector(inDams);
        m.pDamsOnNetDistance = pDamsOnNetDistance;
        m.pBridgesOnNetDistance = pBridgesOnNetDistance;
        m.pFixedDamsWidth = pFixedDamsWidth;
        m.fBridgeLenght = fBridgeLenght;
        m.pm = pm;
        m.process();
        dumpVector(m.outNetPoints, outNetPoints);
        dumpVector(m.outProblemBridges, outProblemBridges);
    }

}
