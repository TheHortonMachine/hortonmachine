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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_outHill_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_pAzimuth_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_pElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSHILLSHADE_pMinDiffuse_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.hillshade.OmsHillshade;

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

@Description(OMSHILLSHADE_DESCRIPTION)
@Author(name = OMSHILLSHADE_AUTHORNAMES, contact = OMSHILLSHADE_AUTHORCONTACTS)
@Keywords(OMSHILLSHADE_KEYWORDS)
@Label(OMSHILLSHADE_LABEL)
@Name("_" + OMSHILLSHADE_NAME)
@Status(OMSHILLSHADE_STATUS)
@License(OMSHILLSHADE_LICENSE)
public class Hillshade extends HMModel {

    @Description(OMSHILLSHADE_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSHILLSHADE_pMinDiffuse_DESCRIPTION)
    @In
    public double pMinDiffuse = 0.0;

    @Description(OMSHILLSHADE_pAzimuth_DESCRIPTION)
    @In
    public double pAzimuth = 360;

    @Description(OMSHILLSHADE_pElev_DESCRIPTION)
    @In
    public double pElev = 90;

    @Description(OMSHILLSHADE_outHill_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outHill;

    @Execute
    public void process() throws Exception {
        OmsHillshade hillshade = new OmsHillshade();
        hillshade.inElev = getRaster(inElev);
        hillshade.pMinDiffuse = pMinDiffuse;
        hillshade.pAzimuth = pAzimuth;
        hillshade.pElev = pElev;
        hillshade.pm = pm;
        hillshade.doProcess = doProcess;
        hillshade.doReset = doReset;
        hillshade.process();
        dumpRaster(hillshade.outHill, outHill);
    }

}
