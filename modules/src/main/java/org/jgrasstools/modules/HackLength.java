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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_inElevation_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_inTca_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSHACKLENGTH_outHacklength_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.network.hacklength.OmsHackLength;

@Description(OMSHACKLENGTH_DESCRIPTION)
@Author(name = OMSHACKLENGTH_AUTHORNAMES, contact = OMSHACKLENGTH_AUTHORCONTACTS)
@Keywords(OMSHACKLENGTH_KEYWORDS)
@Label(OMSHACKLENGTH_LABEL)
@Name("_" + OMSHACKLENGTH_NAME)
@Status(OMSHACKLENGTH_STATUS)
@License(OMSHACKLENGTH_LICENSE)
public class HackLength extends JGTModel {

    @Description(OMSHACKLENGTH_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(OMSHACKLENGTH_inTca_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inTca = null;

    @Description(OMSHACKLENGTH_inElevation_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElevation = null;

    @Description(OMSHACKLENGTH_outHacklength_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outHacklength = null;

    @Execute
    public void process() throws Exception {
        OmsHackLength hacklength = new OmsHackLength();
        hacklength.inFlow = getRaster(inFlow);
        hacklength.inTca = getRaster(inTca);
        hacklength.inElevation = getRaster(inElevation);
        hacklength.pm = pm;
        hacklength.doProcess = doProcess;
        hacklength.doReset = doReset;
        hacklength.process();
        dumpRaster(hacklength.outHacklength, outHacklength);
    }

}
