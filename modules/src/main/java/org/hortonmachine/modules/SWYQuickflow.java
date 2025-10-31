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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYQuickflow.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYQuickflow;

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

@Description(OmsSWYQuickflow.DESCRIPTION)
@Author(name = OmsSWYQuickflow.AUTHORNAMES, contact = OmsSWYQuickflow.AUTHORCONTACTS)
@Keywords(OmsSWYQuickflow.KEYWORDS)
@Label(OmsSWYQuickflow.LABEL)
@Name(OmsSWYQuickflow.NAME)
@Status(OmsSWYQuickflow.STATUS)
@License(OmsSWYQuickflow.LICENSE)
public class SWYQuickflow extends HMModel {
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

    @Description(outQuickflow_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Unit(quickflowUnit)
    @In
    public String outQuickflow;

    @Execute
    public void process() throws Exception {
        OmsSWYQuickflow rf = new OmsSWYQuickflow();
        rf.inRainfall = getRaster(inRainfall);
        rf.inNet = getRaster(inNet);
        rf.inCurveNumber = getRaster(inCurveNumber);
        rf.inNumberOfEvents = getRaster(inNumberOfEvents);
        rf.pm = pm;
        rf.process();

        dumpRaster(rf.outQuickflow, outQuickflow);

    }

}