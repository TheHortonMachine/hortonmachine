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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSKYVIEW_outSky_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.skyview.OmsSkyview;

@Description(OMSSKYVIEW_DESCRIPTION)
@Author(name = OMSSKYVIEW_AUTHORNAMES, contact = OMSSKYVIEW_AUTHORCONTACTS)
@Keywords(OMSSKYVIEW_KEYWORDS)
@Label(OMSSKYVIEW_LABEL)
@Name("_" + OMSSKYVIEW_NAME)
@Status(OMSSKYVIEW_STATUS)
@License(OMSSKYVIEW_LICENSE)
public class Skyview extends JGTModel {

    @Description(OMSSKYVIEW_inElev_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev = null;

    @Description(OMSSKYVIEW_outSky_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outSky;

    @Execute
    public void process() throws Exception {
        OmsSkyview skyview = new OmsSkyview();
        skyview.inElev = getRaster(inElev);
        skyview.pm = pm;
        skyview.doProcess = doProcess;
        skyview.doReset = doReset;
        skyview.process();
        dumpRaster(skyview.outSky, outSky);
    }
}
