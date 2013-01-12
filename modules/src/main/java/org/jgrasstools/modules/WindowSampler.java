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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_inGeodata_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_outGeodata_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pCols_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pMode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pRows_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pXstep_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_pYstep_DESCRIPTION;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
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
import org.jgrasstools.gears.modules.r.windowsampler.OmsWindowSampler;

@Description(OMSWINDOWSAMPLER_DESCRIPTION)
@Documentation(OMSWINDOWSAMPLER_DOCUMENTATION)
@Author(name = OMSWINDOWSAMPLER_AUTHORNAMES, contact = OMSWINDOWSAMPLER_AUTHORCONTACTS)
@Keywords(OMSWINDOWSAMPLER_KEYWORDS)
@Label(OMSWINDOWSAMPLER_LABEL)
@Name("_" + OMSWINDOWSAMPLER_NAME)
@Status(OMSWINDOWSAMPLER_STATUS)
@License(OMSWINDOWSAMPLER_LICENSE)
public class WindowSampler extends JGTModel {

    @Description(OMSWINDOWSAMPLER_inGeodata_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inGeodata;

    @Description(OMSWINDOWSAMPLER_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSWINDOWSAMPLER_pRows_DESCRIPTION)
    @In
    public int pRows = 3;

    @Description(OMSWINDOWSAMPLER_pCols_DESCRIPTION)
    @In
    public int pCols = 3;

    @Description(OMSWINDOWSAMPLER_pXstep_DESCRIPTION)
    @In
    public Integer pXstep;

    @Description(OMSWINDOWSAMPLER_pYstep_DESCRIPTION)
    @In
    public Integer pYstep;

    @Description(OMSWINDOWSAMPLER_outGeodata_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outGeodata;

    @Execute
    public void process() throws Exception {
        OmsWindowSampler windowsampler = new OmsWindowSampler();
        windowsampler.inGeodata = getRaster(inGeodata);
        windowsampler.pMode = pMode;
        windowsampler.pRows = pRows;
        windowsampler.pCols = pCols;
        windowsampler.pXstep = pXstep;
        windowsampler.pYstep = pYstep;
        windowsampler.pm = pm;
        windowsampler.doProcess = doProcess;
        windowsampler.doReset = doReset;
        windowsampler.process();
        dumpRaster(windowsampler.outGeodata, outGeodata);
    }
}
