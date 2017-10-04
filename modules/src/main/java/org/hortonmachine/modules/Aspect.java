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

import static org.hortonmachine.hmachine.modules.geomorphology.aspect.OmsAspect.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.aspect.OmsAspect;

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

@Description(OMSASPECT_DESCRIPTION)
@Author(name = OMSASPECT_AUTHORNAMES, contact = OMSASPECT_AUTHORCONTACTS)
@Keywords(OMSASPECT_KEYWORDS)
@Label(OMSASPECT_LABEL)
@Name("_" + OMSASPECT_NAME)
@Status(OMSASPECT_STATUS)
@License(OMSASPECT_LICENSE)
public class Aspect extends HMModel {
    @Description(OMSASPECT_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSASPECT_doRadiants_DESCRIPTION)
    @In
    public boolean doRadiants = false;

    @Description(OMSASPECT_doRound_DESCRIPTION)
    @In
    public boolean doRound = false;

    @Description(OMSASPECT_outAspect_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outAspect = null;

    @Execute
    public void process() throws Exception {
        OmsAspect aspect = new OmsAspect();
        aspect.inElev = getRaster(inElev);
        aspect.doRadiants = doRadiants;
        aspect.doRound = doRound;
        aspect.pm = pm;
        aspect.process();
        dumpRaster(aspect.outAspect, outAspect);
    }

}
