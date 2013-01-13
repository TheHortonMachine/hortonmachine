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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_fId_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_inFans_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSMELTONNUMBER_outMelton_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.melton.OmsMeltonNumber;

@Description(OMSMELTONNUMBER_DESCRIPTION)
@Author(name = OMSMELTONNUMBER_AUTHORNAMES, contact = OMSMELTONNUMBER_AUTHORCONTACTS)
@Keywords(OMSMELTONNUMBER_KEYWORDS)
@Label(OMSMELTONNUMBER_LABEL)
@Name("_" + OMSMELTONNUMBER_NAME)
@Status(OMSMELTONNUMBER_STATUS)
@License(OMSMELTONNUMBER_LICENSE)
public class MeltonNumber extends JGTModel {

    @Description(OMSMELTONNUMBER_inElev_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev = null;

    @Description(OMSMELTONNUMBER_inFans_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
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
