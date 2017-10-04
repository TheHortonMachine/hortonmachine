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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_inRaster1_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_inRaster2_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_outCb_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_pBins_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_pFirst_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSCB_pLast_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.statistics.cb.OmsCb;

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

@Description(OMSCB_DESCRIPTION)
@Author(name = OMSCB_AUTHORNAMES, contact = OMSCB_AUTHORCONTACTS)
@Keywords(OMSCB_KEYWORDS)
@Label(OMSCB_LABEL)
@Name("_" + OMSCB_NAME)
@Status(OMSCB_STATUS)
@License(OMSCB_LICENSE)
public class Cb extends HMModel {

    @Description(OMSCB_inRaster1_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster1 = null;

    @Description(OMSCB_inRaster2_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
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
        cb.pm = pm;
        cb.process();
        outCb = cb.outCb;
    }

}
