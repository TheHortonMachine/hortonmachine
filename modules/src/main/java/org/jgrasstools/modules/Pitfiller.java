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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPITFILLER_outPit_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller.OmsPitfiller;

@Description(OMSPITFILLER_DESCRIPTION)
@Author(name = OMSPITFILLER_AUTHORNAMES, contact = OMSPITFILLER_AUTHORCONTACTS)
@Keywords(OMSPITFILLER_KEYWORDS)
@Label(OMSPITFILLER_LABEL)
@Name("_" + OMSPITFILLER_NAME)
@Status(OMSPITFILLER_STATUS)
@License(OMSPITFILLER_LICENSE)
public class Pitfiller extends JGTModel {
    @Description(OMSPITFILLER_inElev_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev;

    @Description(OMSPITFILLER_outPit_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outPit = null;

    @Execute
    public void process() throws Exception {
        OmsPitfiller pitfiller = new OmsPitfiller();
        pitfiller.inElev = getRaster(inElev);
        pitfiller.pm = pm;
        pitfiller.doProcess = doProcess;
        pitfiller.doReset = doReset;
        pitfiller.process();
        dumpRaster(pitfiller.outPit, outPit);
    }
}
