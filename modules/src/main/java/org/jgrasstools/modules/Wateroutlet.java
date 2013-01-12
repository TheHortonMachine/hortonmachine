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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_outArea_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_outBasin_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_pEast_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSWATEROUTLET_pNorth_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet.OmsWateroutlet;

@Description(OMSWATEROUTLET_DESCRIPTION)
@Author(name = OMSWATEROUTLET_AUTHORNAMES, contact = OMSWATEROUTLET_AUTHORCONTACTS)
@Keywords(OMSWATEROUTLET_KEYWORDS)
@Label(OMSWATEROUTLET_LABEL)
@Name("_" + OMSWATEROUTLET_NAME)
@Status(OMSWATEROUTLET_STATUS)
@License(OMSWATEROUTLET_LICENSE)
public class Wateroutlet extends JGTModel {
    @Description(OMSWATEROUTLET_pNorth_DESCRIPTION)
    @UI(JGTConstants.NORTHING_UI_HINT)
    @In
    public double pNorth = -1.0;

    @Description(OMSWATEROUTLET_pEast_DESCRIPTION)
    @UI(JGTConstants.EASTING_UI_HINT)
    @In
    public double pEast = -1.0;

    @Description(OMSWATEROUTLET_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow;

    @Description(OMSWATEROUTLET_outBasin_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outBasin = null;

    @Description(OMSWATEROUTLET_outArea_DESCRIPTION)
    @Out
    public double outArea = 0;

    @Execute
    public void process() throws Exception {
        OmsWateroutlet omswateroutlet = new OmsWateroutlet();
        omswateroutlet.pNorth = pNorth;
        omswateroutlet.pEast = pEast;
        omswateroutlet.inFlow = getRaster(inFlow);
        omswateroutlet.pm = pm;
        omswateroutlet.doProcess = doProcess;
        omswateroutlet.doReset = doReset;
        omswateroutlet.process();
        dumpRaster(omswateroutlet.outBasin, outBasin);
    }
}
