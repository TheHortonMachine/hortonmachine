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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.AUTHORS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.CONTACTS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.KEYWORDS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.LABEL;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.LICENSE;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.NAME;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.STATUS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.inNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.inSlope_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.outNetPoints_DESCR;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder;

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

@Description(DESCRIPTION)
@Author(name = AUTHORS, contact = CONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class LW06_SlopeToNetworkAdder extends HMModel {

    @Description(inNetPoints_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNetPoints = null;

    @Description(inSlope_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSlope = null;

    @Description(outNetPoints_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Execute
    public void process() throws Exception {
        OmsLW06_SlopeToNetworkAdder m = new OmsLW06_SlopeToNetworkAdder();
        m.inNetPoints = getVector(inNetPoints);
        m.inSlope = getRaster(inSlope);
        m.pm = pm;
        m.process();
        dumpVector(m.outNetPoints, outNetPoints);
    }

}
