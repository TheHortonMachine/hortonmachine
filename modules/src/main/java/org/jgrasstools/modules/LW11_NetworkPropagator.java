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

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.inNetPoints_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.outNetPoints_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.ratioLogsDiameterWaterDepth_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.ratioLogsLengthChannelWidthChannel_DESCR;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator.ratioLogsLengthChannelWidthHillslope_DESCR;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.LWFields;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW11_NetworkPropagator;

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

@Description(OmsLW11_NetworkPropagator.DESCRIPTION)
@Author(name = OmsLW11_NetworkPropagator.AUTHORS, contact = OmsLW11_NetworkPropagator.CONTACTS)
@Label(OmsLW11_NetworkPropagator.LABEL)
@Keywords(OmsLW11_NetworkPropagator.KEYWORDS)
@Name("_" + OmsLW11_NetworkPropagator.NAME)
@Status(OmsLW11_NetworkPropagator.STATUS)
@License(OmsLW11_NetworkPropagator.LICENSE)
public class LW11_NetworkPropagator extends JGTModel implements LWFields {

    @Description(inNetPoints_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNetPoints = null;

    @Description(ratioLogsLengthChannelWidthHillslope_DESCR)
    @In
    public double pRatioLogsLengthChannelWidthHillslope = 0.8;

    @Description(ratioLogsLengthChannelWidthChannel_DESCR)
    @In
    public double pRatioLogsLengthChannelWidthChannel = 1.0;

    @Description(ratioLogsDiameterWaterDepth_DESCR)
    @In
    public double pRatioLogsDiameterWaterDepth = 0.8;

    @Description(outNetPoints_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outNetPoints = null;

    @Execute
    public void process() throws Exception {
        OmsLW11_NetworkPropagator ex = new OmsLW11_NetworkPropagator();
        ex.inNetPoints = getVector(inNetPoints);
        ex.pRatioLogsDiameterWaterDepth = pRatioLogsDiameterWaterDepth;
        ex.pRatioLogsLengthChannelWidthChannel = pRatioLogsLengthChannelWidthChannel;
        ex.pRatioLogsLengthChannelWidthHillslope = pRatioLogsLengthChannelWidthHillslope;
        ex.process();
        dumpVector(ex.outNetPoints, outNetPoints);
    }

}
