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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.LWFields;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW08_NetworkBufferWidthCalculator;

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

@Description(OmsLW08_NetworkBufferWidthCalculator.DESCRIPTION)
@Author(name = OmsLW08_NetworkBufferWidthCalculator.AUTHORS, contact = OmsLW08_NetworkBufferWidthCalculator.CONTACTS)
@Keywords(OmsLW08_NetworkBufferWidthCalculator.KEYWORDS)
@Label(OmsLW08_NetworkBufferWidthCalculator.LABEL)
@Name("_" + OmsLW08_NetworkBufferWidthCalculator.NAME)
@Status(OmsLW08_NetworkBufferWidthCalculator.STATUS)
@License(OmsLW08_NetworkBufferWidthCalculator.LICENSE)
public class LW08_NetworkBufferWidthCalculator extends JGTModel implements LWFields {
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
    public String inTransSect = null;

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
        OmsLW08_NetworkBufferWidthCalculator ex = new OmsLW08_NetworkBufferWidthCalculator();
        ex.inNetPoints = getVector(inNetPoints);
        ex.inGeo = getVector(inGeo);
        ex.inTransSect = getVector(inTransSect);
        ex.pPrePostCount4Slope = pPrePostCount4Slope;
        ex.pK = pK;
        ex.pN = pN;
        ex.doKeepBridgeDamWidth = doKeepBridgeDamWidth;
        ex.pMinSlope = pMinSlope;
        ex.pm = pm;
        ex.process();
        dumpVector(ex.outNetPoints, outNetPoints);
        dumpVector(ex.outInundationArea, outInundationArea);
        dumpVector(ex.outInundationSections, outInundationSections);
    }
}
