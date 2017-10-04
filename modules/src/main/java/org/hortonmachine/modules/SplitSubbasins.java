/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_inHack_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_outNetnum_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_outSubbasins_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSPLITSUBBASINS_pHackorder_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.demmanipulation.splitsubbasin.OmsSplitSubbasins;

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

@Description(OMSSPLITSUBBASINS_DESCRIPTION)
@Author(name = OMSSPLITSUBBASINS_AUTHORNAMES, contact = OMSSPLITSUBBASINS_AUTHORCONTACTS)
@Keywords(OMSSPLITSUBBASINS_KEYWORDS)
@Label(OMSSPLITSUBBASINS_LABEL)
@Name("_" + OMSSPLITSUBBASINS_NAME)
@Status(OMSSPLITSUBBASINS_STATUS)
@License(OMSSPLITSUBBASINS_LICENSE)
public class SplitSubbasins extends HMModel {
    @Description(OMSSPLITSUBBASINS_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSSPLITSUBBASINS_inHack_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inHack = null;

    @Description(OMSSPLITSUBBASINS_pHackorder_DESCRIPTION)
    @In
    public Double pHackorder = null;

    @Description(OMSSPLITSUBBASINS_outNetnum_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetnum = null;

    @Description(OMSSPLITSUBBASINS_outSubbasins_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outSubbasins = null;

    @Execute
    public void process() throws Exception {
        OmsSplitSubbasins splitsubbasins = new OmsSplitSubbasins();
        splitsubbasins.inFlow = getRaster(inFlow);
        splitsubbasins.inHack = getRaster(inHack);
        splitsubbasins.pHackorder = pHackorder;
        splitsubbasins.pm = pm;
        splitsubbasins.doProcess = doProcess;
        splitsubbasins.doReset = doReset;
        splitsubbasins.process();
        dumpRaster(splitsubbasins.outNetnum, outNetnum);
        dumpRaster(splitsubbasins.outSubbasins, outSubbasins);
    }
}
