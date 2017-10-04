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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_inPit_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA3D_outTca_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.tca3d.OmsTca3d;

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

@Description(OMSTCA3D_DESCRIPTION)
@Author(name = OMSTCA3D_AUTHORNAMES, contact = OMSTCA3D_AUTHORCONTACTS)
@Keywords(OMSTCA3D_KEYWORDS)
@Label(OMSTCA3D_LABEL)
@Name("_" + OMSTCA3D_NAME)
@Status(OMSTCA3D_STATUS)
@License(OMSTCA3D_LICENSE)
public class Tca3d extends HMModel {
    @Description(OMSTCA3D_inPit_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inPit = null;

    @Description(OMSTCA3D_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSTCA3D_outTca_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTca = null;

    @Execute
    public void process() throws Exception {
        OmsTca3d tca3d = new OmsTca3d();
        tca3d.inPit = getRaster(inPit);
        tca3d.inFlow = getRaster(inFlow);
        tca3d.pm = pm;
        tca3d.doProcess = doProcess;
        tca3d.doReset = doReset;
        tca3d.process();
        dumpRaster(tca3d.outTca, outTca);
    }
}