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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.AUTHORS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.CONTACTS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.KEYWORDS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.LABEL;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.LICENSE;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.NAME;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.STATUS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.inBankfull_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.inNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.outBankfullSections_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.outNetPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.outProblemPoints_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.pMaxDistanceFromNetpoint_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.pMaxNetworkWidth_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer.pMinNetworkWidth_DESCR;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW04_BankfullWidthAnalyzer;

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
public class LW04_BankfullWidthAnalyzer extends HMModel {

    @Description(inBankfull_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inBankfull = null;

    @Description(inNetPoints_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inNetPoints = null;

    @Description(pMaxDistanceFromNetpoint_DESCR)
    @Unit("m")
    @In
    public double pMaxDistanceFromNetpoint = 100.0;

    @Description(pMaxNetworkWidth_DESCR)
    @Unit("m")
    @In
    public double pMaxNetworkWidth = 100;

    @Description(pMinNetworkWidth_DESCR)
    @Unit("m")
    @In
    public double pMinNetworkWidth = 0.5;

    @Description(outNetPoints_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Description(outProblemPoints_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outProblemPoints = null;

    @Description(outBankfullSections_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBankfullSections = null;

    @Execute
    public void process() throws Exception {
        OmsLW04_BankfullWidthAnalyzer m = new OmsLW04_BankfullWidthAnalyzer();
        m.inBankfull = getVector(inBankfull);
        m.inNetPoints = getVector(inNetPoints);
        m.pMaxDistanceFromNetpoint = pMaxDistanceFromNetpoint;
        m.pMaxNetworkWidth = pMaxNetworkWidth;
        m.pMinNetworkWidth = pMinNetworkWidth;
        m.pm = pm;
        m.process();
        dumpVector(m.outNetPoints, outNetPoints);
        dumpVector(m.outProblemPoints, outProblemPoints);
        dumpVector(m.outBankfullSections, outBankfullSections);
    }
}
