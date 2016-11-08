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

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.AUTHORS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.CONTACTS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.STATUS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.inNetPoints_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.outNetPoints_DESCR;
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
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator;

@Description(DESCRIPTION)
@Author(name = AUTHORS, contact = CONTACTS)
@Label(LABEL)
@Keywords(KEYWORDS)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class LW10_NetworkPropagator extends JGTModel {

    @Description(inNetPoints_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNetPoints = null;

    @Description(outNetPoints_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Execute
    public void process() throws Exception {
        OmsLW11_NetworkPropagator m = new OmsLW11_NetworkPropagator();
        m.inNetPoints = getVector(inNetPoints);
        m.process();
        dumpVector(m.outNetPoints, outNetPoints);
    }

}
