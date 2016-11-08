/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.modules;

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.AUTHORS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.CONTACTS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.STATUS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.inNetPoints_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.inSlope_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder.outNetPoints_DESCR;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW06_SlopeToNetworkAdder;

@Description(DESCRIPTION)
@Author(name = AUTHORS, contact = CONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class LW06_SlopeToNetworkAdder extends JGTModel {

    @Description(inNetPoints_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNetPoints = null;

    @Description(inSlope_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inSlope = null;

    @Description(outNetPoints_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
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
