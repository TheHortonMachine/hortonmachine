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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_doSmoothing_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_doVector_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_inNetwork_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_outArea_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_outBasin_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_outOutlet_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_outVectorBasin_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_pEast_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_pNorth_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_pSnapbuffer_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSEXTRACTBASIN_pValue_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin;

@Description(OMSEXTRACTBASIN_DESCRIPTION)
@Author(name = OMSEXTRACTBASIN_AUTHORNAMES, contact = OMSEXTRACTBASIN_AUTHORCONTACTS)
@Keywords(OMSEXTRACTBASIN_KEYWORDS)
@Label(OMSEXTRACTBASIN_LABEL)
@Name("_" + OMSEXTRACTBASIN_NAME)
@Status(OMSEXTRACTBASIN_STATUS)
@License(OMSEXTRACTBASIN_LICENSE)
public class ExtractBasin extends JGTModel {
    @Description(OMSEXTRACTBASIN_pNorth_DESCRIPTION)
    @UI(JGTConstants.NORTHING_UI_HINT)
    @In
    public double pNorth = -1.0;

    @Description(OMSEXTRACTBASIN_pEast_DESCRIPTION)
    @UI(JGTConstants.EASTING_UI_HINT)
    @In
    public double pEast = -1.0;

    @Description(OMSEXTRACTBASIN_pValue_DESCRIPTION)
    @In
    public double pValue = 1.0;

    @Description(OMSEXTRACTBASIN_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow;

    @Description(OMSEXTRACTBASIN_inNetwork_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inNetwork;

    @Description(OMSEXTRACTBASIN_pSnapbuffer_DESCRIPTION)
    @In
    public double pSnapbuffer = 200;

    @Description(OMSEXTRACTBASIN_doVector_DESCRIPTION)
    @In
    public boolean doVector = true;

    @Description(OMSEXTRACTBASIN_doSmoothing_DESCRIPTION)
    @In
    public boolean doSmoothing = false;

    @Description(OMSEXTRACTBASIN_outArea_DESCRIPTION)
    @Out
    public double outArea = 0;

    @Description(OMSEXTRACTBASIN_outBasin_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outBasin = null;

    @Description(OMSEXTRACTBASIN_outOutlet_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outOutlet = null;

    @Description(OMSEXTRACTBASIN_outVectorBasin_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Out
    public String outVectorBasin = null;

    @Execute
    public void process() throws Exception {
        OmsExtractBasin extractbasin = new OmsExtractBasin();
        extractbasin.pNorth = pNorth;
        extractbasin.pEast = pEast;
        extractbasin.pValue = pValue;
        extractbasin.inFlow = getRaster(inFlow);
        extractbasin.inNetwork = getVector(inNetwork);
        extractbasin.pSnapbuffer = pSnapbuffer;
        extractbasin.doVector = doVector;
        extractbasin.doSmoothing = doSmoothing;
        extractbasin.pm = pm;
        extractbasin.doProcess = doProcess;
        extractbasin.doReset = doReset;
        extractbasin.process();
        dumpRaster(extractbasin.outBasin, outBasin);
        dumpVector(extractbasin.outOutlet, outOutlet);
        dumpVector(extractbasin.outVectorBasin, outVectorBasin);
        pm.message("Basin Area = " + outArea);
    }

}
