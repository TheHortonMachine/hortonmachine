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

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.AUTHORS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.CONTACTS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.STATUS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.doKeepBridgeDamWidth_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.inGeo_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.inNetPoints_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.inTransSect_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.outInundationArea_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.outInundationSections_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.outNetPoints_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.pK_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.pMinSlope_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.pN_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator.pPrePostCount4Slope_DESCR;
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
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator;

@Description(DESCRIPTION)
@Author(name = AUTHORS, contact = CONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class LW07_NetworkBufferWidthCalculator extends JGTModel {
    @Description(inNetPoints_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNetPoints = null;

    @Description(inGeo_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inGeo = null;

    @Description(inTransSect_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inSectWidth = null;

    @Description(pPrePostCount4Slope_DESCR)
    @In
    public int pPrePostCount4Slope = 10;

    @Description(pK_DESCR)
    @In
    public double pK = 20.0;

    @Description(pN_DESCR)
    @In
    public double pN = -0.2;

    @Description(doKeepBridgeDamWidth_DESCR)
    @In
    public boolean doKeepBridgeDamWidth = true;

    @Description(pMinSlope_DESCR)
    @In
    public double pMinSlope = 0.001;

    @Description(outNetPoints_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Description(outInundationArea_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outInundationArea = null;

    @Description(outInundationSections_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outInundationSections = null;

    @Execute
    public void process() throws Exception {
        OmsLW08_NetworkBufferWidthCalculator m = new OmsLW08_NetworkBufferWidthCalculator();
        m.inNetPoints = getVector(inNetPoints);
        m.inGeo = getVector(inNetPoints);
        m.inSectWidth = getVector(inSectWidth);
        m.pPrePostCount4Slope = pPrePostCount4Slope;
        m.pK = pK;
        m.pN = pN;
        m.doKeepBridgeDamWidth = doKeepBridgeDamWidth;
        m.pMinSlope = pMinSlope;
        m.process();
        dumpVector(m.outNetPoints, outNetPoints);
        dumpVector(m.outInundationArea, outInundationArea);
        dumpVector(m.outInundationSections, outInundationSections);
    }

}
