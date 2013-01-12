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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_inNet_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSTRAHLER_outStrahler_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.network.strahler.OmsStrahler;

@Description(OMSSTRAHLER_DESCRIPTION)
@Author(name = OMSSTRAHLER_AUTHORNAMES, contact = OMSSTRAHLER_AUTHORCONTACTS)
@Keywords(OMSSTRAHLER_KEYWORDS)
@Label(OMSSTRAHLER_LABEL)
@Name("_" + OMSSTRAHLER_NAME)
@Status(OMSSTRAHLER_STATUS)
@License(OMSSTRAHLER_LICENSE)
public class Strahler extends JGTModel {

    @Description(OMSSTRAHLER_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(OMSSTRAHLER_inNet_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNet = null;

    @Description(OMSSTRAHLER_outStrahler_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outStrahler = null;

    @Execute
    public void process() throws Exception {
        OmsStrahler omsstrahler = new OmsStrahler();
        omsstrahler.inFlow = getRaster(inFlow);
        omsstrahler.inNet = getRaster(inNet);
        omsstrahler.pm = pm;
        omsstrahler.doProcess = doProcess;
        omsstrahler.doReset = doReset;
        omsstrahler.process();
        dumpRaster(omsstrahler.outStrahler, outStrahler);
    }
}
