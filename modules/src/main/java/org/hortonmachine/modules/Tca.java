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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_outLoop_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_outTca_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.tca.OmsTca;

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

@Description(OMSTCA_DESCRIPTION)
@Author(name = OMSTCA_AUTHORNAMES, contact = OMSTCA_AUTHORCONTACTS)
@Keywords(OMSTCA_KEYWORDS)
@Label(OMSTCA_LABEL)
@Name("_" + OMSTCA_NAME)
@Status(OMSTCA_STATUS)
@License(OMSTCA_LICENSE)
public class Tca extends HMModel {
    @Description(OMSTCA_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSTCA_outTca_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTca = null;

    @Description(OMSTCA_outLoop_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outLoop = null;

    @Execute
    public void process() throws Exception {
        OmsTca omstca = new OmsTca();
        omstca.inFlow = getRaster(inFlow);
        omstca.pm = pm;
        omstca.doProcess = doProcess;
        omstca.doReset = doReset;
        if (outLoop != null && outLoop.trim().length() > 0) {
            omstca.doLoopCheck = true;
        }
        omstca.process();
        dumpRaster(omstca.outTca, outTca);
        if (omstca.outLoop != null) {
            dumpVector(omstca.outLoop, outLoop);
        }
    }

}