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

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.scsrunoff.OmsScsRunoff;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.scsrunoff.OmsScsRunoff.*;

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

@Description(OmsScsRunoff.DESCRIPTION)
@Author(name = OmsScsRunoff.AUTHORNAMES, contact = OmsScsRunoff.AUTHORCONTACTS)
@Keywords(OmsScsRunoff.KEYWORDS)
@Label(OmsScsRunoff.LABEL)
@Name(OmsScsRunoff.NAME)
@Status(OmsScsRunoff.STATUS)
@License(OmsScsRunoff.LICENSE)
public class ScsRunoff extends HMModel {
    @Description(inRainfall_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit(pRainfall_UNIT)
    @In
    public String inRainfall = null;

    @Description(inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(inCN_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inCurveNumber = null;

    @Description(inNumberEvents_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNumberOfEvents;

    @Description(outputDischarge_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outputDischarge;

    @Execute
    public void process() throws Exception {
        OmsScsRunoff rf = new OmsScsRunoff();
        rf.inRainfall = getRaster(inRainfall);
        rf.inNet = getRaster(inNet);
        rf.inCurveNumber = getRaster(inCurveNumber);
        rf.inNumberOfEvents = getRaster(inNumberOfEvents);
        rf.pm = pm;
        rf.process();

        dumpRaster(rf.outputDischarge, outputDischarge);

    }

}