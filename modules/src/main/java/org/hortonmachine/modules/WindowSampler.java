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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_IN_GEODATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_OUT_GEODATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_P_COLS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_P_MODE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_P_ROWS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_P_X_STEP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_P_Y_STEP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSWINDOWSAMPLER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.windowsampler.OmsWindowSampler;

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

@Description(OMSWINDOWSAMPLER_DESCRIPTION)
@Author(name = OMSWINDOWSAMPLER_AUTHORNAMES, contact = OMSWINDOWSAMPLER_AUTHORCONTACTS)
@Keywords(OMSWINDOWSAMPLER_KEYWORDS)
@Label(OMSWINDOWSAMPLER_LABEL)
@Name("_" + OMSWINDOWSAMPLER_NAME)
@Status(OMSWINDOWSAMPLER_STATUS)
@License(OMSWINDOWSAMPLER_LICENSE)
public class WindowSampler extends HMModel {

    @Description(OMSWINDOWSAMPLER_IN_GEODATA_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inGeodata;

    @Description(OMSWINDOWSAMPLER_P_MODE_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSWINDOWSAMPLER_P_ROWS_DESCRIPTION)
    @In
    public int pRows = 3;

    @Description(OMSWINDOWSAMPLER_P_COLS_DESCRIPTION)
    @In
    public int pCols = 3;

    @Description(OMSWINDOWSAMPLER_P_X_STEP_DESCRIPTION)
    @In
    public Integer pXstep;

    @Description(OMSWINDOWSAMPLER_P_Y_STEP_DESCRIPTION)
    @In
    public Integer pYstep;

    @Description(OMSWINDOWSAMPLER_OUT_GEODATA_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
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
