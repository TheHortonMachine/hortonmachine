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

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.AUTHORS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.CONTACTS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.STATUS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inConnectivity_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inDsm_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inDtm_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inFlow_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inInundationArea_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inNetPoints_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inNet_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inSlope_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inStand_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.inTca_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.outBasins_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.outNetPoints_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.outNetnum_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator.pConnectivityThreshold_DESCR;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW10_CHM_AreaToNetpointAssociator;

@Description(DESCRIPTION)
@Author(name = AUTHORS, contact = CONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class LW09_AreaToNetpointAssociator extends JGTModel {
    @Description(inNetPoints_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNetPoints = null;

    @Description(inInundationArea_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inInundationArea = null;

    @Description(inFlow_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(inTca_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inTca = null;

    @Description(inNet_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNet = null;

    @Description(inDtm_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDtm = null;

    @Description(inDsm_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDsm = null;

    @Description(inStand_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inStand = null;

    @Description(inSlope_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inSlope = null;

    @Description(inConnectivity_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inConnectivity = null;

    @Description(pConnectivityThreshold_DESCR)
    @In
    public double pConnectivityThreshold = 4.0;

    @Description(outNetPoints_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Description(outNetnum_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outNetnum = null;

    @Description(outBasins_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outBasins = null;

    @Execute
    public void process() throws Exception {
        OmsLW10_CHM_AreaToNetpointAssociator m = new OmsLW10_CHM_AreaToNetpointAssociator();
        m.inNetPoints = getVector(inNetPoints);
        m.inInundationArea = getVector(inInundationArea);
        m.inFlow = getRaster(inFlow);
        m.inTca = getRaster(inTca);
        m.inNet = getRaster(inNet);
        m.inDtm = getRaster(inDtm);
        m.inDsm = getRaster(inDsm);
        m.inStand = getRaster(inStand);
        m.inSlope = getRaster(inSlope);
        m.inConnectivity = getRaster(inConnectivity);
        m.pConnectivityThreshold = pConnectivityThreshold;
        m.process();
        dumpVector(m.outNetPoints, outNetPoints);
        dumpRaster(m.outNetnum, outNetnum);
        dumpRaster(m.outBasins, outBasins);
    }

}
