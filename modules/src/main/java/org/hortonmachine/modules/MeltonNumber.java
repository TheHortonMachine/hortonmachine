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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_fId_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_inFans_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_outMelton_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.melton.OmsMeltonNumber;

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

@Description(OMSMELTONNUMBER_DESCRIPTION)
@Author(name = OMSMELTONNUMBER_AUTHORNAMES, contact = OMSMELTONNUMBER_AUTHORCONTACTS)
@Keywords(OMSMELTONNUMBER_KEYWORDS)
@Label(OMSMELTONNUMBER_LABEL)
@Name("_" + OMSMELTONNUMBER_NAME)
@Status(OMSMELTONNUMBER_STATUS)
@License(OMSMELTONNUMBER_LICENSE)
public class MeltonNumber extends HMModel {

    @Description(OMSMELTONNUMBER_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSMELTONNUMBER_inFans_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inFans = null;

    @Description(OMSMELTONNUMBER_fId_DESCRIPTION)
    @In
    public String fId;

    @Description(OMSMELTONNUMBER_outMelton_DESCRIPTION)
    @In
    public String[][] outMelton = null;

    @Execute
    public void process() throws Exception {
        OmsMeltonNumber meltonnumber = new OmsMeltonNumber();
        meltonnumber.inElev = getRaster(inElev);
        meltonnumber.inFans = getVector(inFans);
        meltonnumber.fId = fId;
        meltonnumber.pm = pm;
        meltonnumber.doProcess = doProcess;
        meltonnumber.doReset = doReset;
        meltonnumber.process();
        outMelton = meltonnumber.outMelton;
    }
}
