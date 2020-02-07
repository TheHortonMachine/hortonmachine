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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.*;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_KEYWORDS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_LABEL;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_LICENSE;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_NAME;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_STATUS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_inRescaledsub_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_inRescaledsup_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_inSat_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_inTopindex_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_pA_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_pCelerity_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_pN_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_pOutputStepArg_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_pSat_DESCRIPTION;

import java.io.File;
import java.util.HashMap;

import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader;
import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow;
import org.joda.time.DateTime;

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
import oms3.annotations.Unit;

@Description(OMSPEAKFLOW_DESCRIPTION)
@Author(name = OMSPEAKFLOW_AUTHORNAMES, contact = OMSPEAKFLOW_AUTHORCONTACTS)
@Keywords(OMSPEAKFLOW_KEYWORDS)
@Label(OMSPEAKFLOW_LABEL)
@Name("_" + OMSPEAKFLOW_NAME)
@Status(OMSPEAKFLOW_STATUS)
@License(OMSPEAKFLOW_LICENSE)
public class Peakflow extends HMModel {

    @Description(OMSPEAKFLOW_pA_DESCRIPTION)
    @Unit("mm/h^m")
    @In
    public double pA = -1f;

    @Description(OMSPEAKFLOW_pN_DESCRIPTION)
    @In
    public double pN = -1f;

    @Description(OMSPEAKFLOW_pCelerity_DESCRIPTION)
    @Unit("m/s")
    @In
    public double pCelerity = -1f;

    @Description(OMSPEAKFLOW_pDiffusionSup_DESCRIPTION)
    @Unit("m2/s")
    @In
    public double pDiffusionSup = -9999.0;

    @Description(OMSPEAKFLOW_pDiffusionSubSup_DESCRIPTION)
    @Unit("m2/s")
    @In
    public double pDiffusionSubSup = -9999.0;

    @Description(OMSPEAKFLOW_pSat_DESCRIPTION)
    @Unit("%")
    @In
    public double pSat = -1f;

    @Description(OMSPEAKFLOW_pOutputStepArg_DESCRIPTION)
    @Unit("s")
    @In
    public double pOutputStepArg = 60;

    @Description(OMSPEAKFLOW_inTopindex_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTopindex = null;

    @Description(OMSPEAKFLOW_inSat_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSat = null;

    @Description(OMSPEAKFLOW_inRescaledsup_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRescaledsup = null;

    @Description(OMSPEAKFLOW_inRescaledsub_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRescaledsub = null;

    @Description("The oms csv of rainfall data per timestep.")
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inRainfall;

    @Description("The oms csv of peakflow output per timestep.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDischarge;

    public double[][] widthFunctionSuperficial;
    public double[][] widthFunctionSubSuperficial;
    public double[][] volumeCheckMatrix;

    @Execute
    public void process() throws Exception {

        OmsPeakflow peakflow = new OmsPeakflow();
        peakflow.pA = pA;
        peakflow.pN = pN;
        peakflow.pCelerity = pCelerity;
        peakflow.pDiffusionSup = pDiffusionSup;
        peakflow.pDiffusionSubSup = pDiffusionSubSup;
        peakflow.pSat = pSat;
        peakflow.pOutputStepArg = pOutputStepArg;
        peakflow.inTopindex = getRaster(inTopindex);
        peakflow.inSat = getRaster(inSat);
        peakflow.inRescaledsup = getRaster(inRescaledsup);
        peakflow.inRescaledsub = getRaster(inRescaledsub);
        if (inRainfall != null && new File(inRainfall).exists()) {
            OmsTimeSeriesReader reader = new OmsTimeSeriesReader();
            reader.file = inRainfall;
            reader.fileNovalue = "-9999";
            reader.read();
            HashMap<DateTime, double[]> outData = reader.outData;
            peakflow.inRainfall = outData;
        }
        peakflow.pm = pm;
        peakflow.doProcess = doProcess;
        peakflow.doReset = doReset;
        peakflow.process();

        widthFunctionSuperficial = peakflow.widthFunctionSuperficial;
        widthFunctionSubSuperficial = peakflow.widthFunctionSubSuperficial;
        volumeCheckMatrix = peakflow.volumeCheckMatrix;

        OmsTimeSeriesWriter writer = new OmsTimeSeriesWriter();
        writer.columns = "date, discharge";
        writer.file = outDischarge;
        writer.doDates = true;
        writer.inData = peakflow.outDischarge;
        writer.tablename = "discharge";
        writer.write();
        writer.close();

    }
}
