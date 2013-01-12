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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_doHistogram_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_outCb_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_outMax_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_outMean_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_outMin_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_outRange_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_outSdev_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_outSum_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERSUMMARY_pBins_DESCRIPTION;
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
import org.jgrasstools.gears.modules.r.summary.OmsRasterSummary;

@Description(OMSRASTERSUMMARY_DESCRIPTION)
@Documentation(OMSRASTERSUMMARY_DOCUMENTATION)
@Author(name = OMSRASTERSUMMARY_AUTHORNAMES, contact = OMSRASTERSUMMARY_AUTHORCONTACTS)
@Keywords(OMSRASTERSUMMARY_KEYWORDS)
@Label(OMSRASTERSUMMARY_LABEL)
@Name("_" + OMSRASTERSUMMARY_NAME)
@Status(OMSRASTERSUMMARY_STATUS)
@License(OMSRASTERSUMMARY_LICENSE)
public class RasterSummary extends JGTModel {

    @Description(OMSRASTERSUMMARY_inRaster_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster;

    @Description(OMSRASTERSUMMARY_pBins_DESCRIPTION)
    @In
    public int pBins = 100;

    @Description(OMSRASTERSUMMARY_doHistogram_DESCRIPTION)
    @In
    public boolean doHistogram = false;

    @Description(OMSRASTERSUMMARY_outMin_DESCRIPTION)
    @Out
    public Double outMin = null;

    @Description(OMSRASTERSUMMARY_outMax_DESCRIPTION)
    @Out
    public Double outMax = null;

    @Description(OMSRASTERSUMMARY_outMean_DESCRIPTION)
    @Out
    public Double outMean = null;

    @Description(OMSRASTERSUMMARY_outSdev_DESCRIPTION)
    @Out
    public Double outSdev = null;

    @Description(OMSRASTERSUMMARY_outRange_DESCRIPTION)
    @Out
    public Double outRange = null;

    @Description(OMSRASTERSUMMARY_outSum_DESCRIPTION)
    @Out
    public Double outSum = null;

    @Description(OMSRASTERSUMMARY_outCb_DESCRIPTION)
    @Out
    public double[][] outCb = null;

    @Execute
    public void process() throws Exception {
        OmsRasterSummary rastersummary = new OmsRasterSummary();
        rastersummary.inRaster = getRaster(inRaster);
        rastersummary.pBins = pBins;
        rastersummary.doHistogram = doHistogram;
        rastersummary.pm = pm;
        rastersummary.doProcess = doProcess;
        rastersummary.doReset = doReset;
        rastersummary.process();
        outMin = rastersummary.outMin;
        outMax = rastersummary.outMax;
        outMean = rastersummary.outMean;
        outSdev = rastersummary.outSdev;
        outRange = rastersummary.outRange;
        outSum = rastersummary.outSum;
        outCb = rastersummary.outCb;
    }
}
