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

import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_DO_HISTOGRAM_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_KEYWORDS;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_LABEL;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_LICENSE;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_NAME;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_OUT_CB_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_OUT_MAX_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_OUT_MEAN_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_OUT_MIN_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_OUT_RANGE_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_OUT_SUM_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_OUT_S_DEV_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_P_BINS_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.summary.OmsRasterSummary.OMSRASTERSUMMARY_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;

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

@Description(OMSRASTERSUMMARY_DESCRIPTION)
@Author(name = OMSRASTERSUMMARY_AUTHORNAMES, contact = OMSRASTERSUMMARY_AUTHORCONTACTS)
@Keywords(OMSRASTERSUMMARY_KEYWORDS)
@Label(OMSRASTERSUMMARY_LABEL)
@Name("_" + OMSRASTERSUMMARY_NAME)
@Status(OMSRASTERSUMMARY_STATUS)
@License(OMSRASTERSUMMARY_LICENSE)
public class RasterSummary extends HMModel {

    @Description(OMSRASTERSUMMARY_IN_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(OMSRASTERSUMMARY_P_BINS_DESCRIPTION)
    @In
    public int pBins = 100;

    @Description(OMSRASTERSUMMARY_DO_HISTOGRAM_DESCRIPTION)
    @In
    public boolean doHistogram = false;

    @Description(OMSRASTERSUMMARY_OUT_MIN_DESCRIPTION)
    @Out
    public Double outMin = null;

    @Description(OMSRASTERSUMMARY_OUT_MAX_DESCRIPTION)
    @Out
    public Double outMax = null;

    @Description(OMSRASTERSUMMARY_OUT_MEAN_DESCRIPTION)
    @Out
    public Double outMean = null;

    @Description(OMSRASTERSUMMARY_OUT_S_DEV_DESCRIPTION)
    @Out
    public Double outSdev = null;

    @Description(OMSRASTERSUMMARY_OUT_RANGE_DESCRIPTION)
    @Out
    public Double outRange = null;

    @Description(OMSRASTERSUMMARY_OUT_SUM_DESCRIPTION)
    @Out
    public Double outSum = null;

    @Description(OMSRASTERSUMMARY_OUT_CB_DESCRIPTION)
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
