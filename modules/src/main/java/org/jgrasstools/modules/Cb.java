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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_inRaster1_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_inRaster2_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_outCb_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_pBins_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_pFirst_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCB_pLast_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.statistics.cb.OmsCb;

@Description(OMSCB_DESCRIPTION)
@Author(name = OMSCB_AUTHORNAMES, contact = OMSCB_AUTHORCONTACTS)
@Keywords(OMSCB_KEYWORDS)
@Label(OMSCB_LABEL)
@Name(OMSCB_NAME)
@Status(OMSCB_STATUS)
@License(OMSCB_LICENSE)
public class Cb extends JGTModel {

    @Description(OMSCB_inRaster1_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster1 = null;

    @Description(OMSCB_inRaster2_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster2 = null;

    @Description(OMSCB_pBins_DESCRIPTION)
    @In
    public int pBins = 100;

    @Description(OMSCB_pFirst_DESCRIPTION)
    @In
    public int pFirst = 1;

    @Description(OMSCB_pLast_DESCRIPTION)
    @In
    public int pLast = 2;

    @Description(OMSCB_outCb_DESCRIPTION)
    @Out
    public double[][] outCb;

    @Execute
    public void process() throws Exception {
        OmsCb cb = new OmsCb();
        cb.inRaster1 = getRaster(inRaster1);
        cb.inRaster2 = getRaster(inRaster2);
        cb.pBins = pBins;
        cb.pFirst = pFirst;
        cb.pLast = pLast;
        cb.process();
        outCb = cb.outCb;
    }

}
