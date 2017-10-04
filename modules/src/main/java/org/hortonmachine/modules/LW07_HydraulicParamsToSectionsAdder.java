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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.doMaxWidening_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.inDischarge_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.inDtm_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.inNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.inNet_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.outNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.outTransSect_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.outputDischargeFile_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.outputLevelFile_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.pDeltaTMillis_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder.pDeltaTMillis_UNIT;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.LWFields;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW07_HydraulicParamsToSectionsAdder;

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

@Description(OmsLW07_HydraulicParamsToSectionsAdder.DESCRIPTION)
@Author(name = OmsLW07_HydraulicParamsToSectionsAdder.AUTHORS, contact = OmsLW07_HydraulicParamsToSectionsAdder.CONTACTS)
@Keywords(OmsLW07_HydraulicParamsToSectionsAdder.KEYWORDS)
@Label(OmsLW07_HydraulicParamsToSectionsAdder.LABEL)
@Name("_" + OmsLW07_HydraulicParamsToSectionsAdder.NAME)
@Status(OmsLW07_HydraulicParamsToSectionsAdder.STATUS)
@License(OmsLW07_HydraulicParamsToSectionsAdder.LICENSE)
public class LW07_HydraulicParamsToSectionsAdder extends HMModel implements LWFields {
    @Description(inDtm_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm = null;

    @Description(inNet_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNet = null;

    @Description(inNetPoints_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNetPoints = null;

    @Description(inDischarge_DESCRIPTION)
    @Unit("m3/s")
    @In
    public double pDischarge;

    @Description(pDeltaTMillis_DESCRIPTION)
    @Unit(pDeltaTMillis_UNIT)
    @In
    public long pDeltaTMillis = 5000;

    @Description(doMaxWidening_DESCRIPTION)
    @In
    public boolean doMaxWidening = false;

    @Description(outputLevelFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outputLevelFile;

    @Description(outputDischargeFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outputDischargeFile;

    @Description(outNetPoints_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Description(outTransSect_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTransSect = null;

    @Execute
    public void process() throws Exception {
        OmsLW07_HydraulicParamsToSectionsAdder ex = new OmsLW07_HydraulicParamsToSectionsAdder();
        ex.inDtm = getRaster(inDtm);
        ex.inNet = getVector(inNet);
        ex.inNetPoints = getVector(inNetPoints);
        ex.pDischarge = pDischarge;
        ex.pDeltaTMillis = pDeltaTMillis;
        ex.doMaxWidening = doMaxWidening;
        ex.outputLevelFile = outputLevelFile;
        ex.outputDischargeFile = outputDischargeFile;
        ex.pm = pm;
        ex.process();
        dumpVector(ex.outNetPoints, outNetPoints);
        dumpVector(ex.outTransSect, outTransSect);
    }
}
