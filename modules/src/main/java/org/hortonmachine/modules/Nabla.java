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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_DOCUMENTATION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_outNabla_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNABLA_pThres_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.nabla.OmsNabla;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSNABLA_DESCRIPTION)
@Documentation(OMSNABLA_DOCUMENTATION)
@Author(name = OMSNABLA_AUTHORNAMES, contact = OMSNABLA_AUTHORCONTACTS)
@Keywords(OMSNABLA_KEYWORDS)
@Label(OMSNABLA_LABEL)
@Name(OMSNABLA_NAME)
@Status(OMSNABLA_STATUS)
@License(OMSNABLA_LICENSE)
public class Nabla extends HMModel {
    @Description(OMSNABLA_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSNABLA_pThres_DESCRIPTION)
    @In
    public Double pThreshold = null;

    @Description(OMSNABLA_outNabla_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNabla = null;

    @Execute
    public void process() throws Exception {
        OmsNabla omsnabla = new OmsNabla();
        omsnabla.inElev = getRaster(inElev);
        omsnabla.pThreshold = pThreshold;
        omsnabla.pm = pm;
        omsnabla.doProcess = doProcess;
        omsnabla.doReset = doReset;
        omsnabla.process();
        dumpRaster(omsnabla.outNabla, outNabla);
    }
}
