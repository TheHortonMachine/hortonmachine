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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_UI;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_defaultPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_defaultTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_doHourly_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_fDataID_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_inNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_inPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_inTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_outPTEtp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pAlpha_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pDailyDefaultNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pGmorn_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pGnight_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pHourlyDefaultNetradiation_DESCRIPTION;

import java.net.URISyntaxException;
import java.util.HashMap;

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

import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPresteyTaylorEtpModel;

@Description(OMSPRESTEYTAYLORETPMODEL_DESCRIPTION)
@Author(name = OMSPRESTEYTAYLORETPMODEL_AUTHORNAMES, contact = OMSPRESTEYTAYLORETPMODEL_AUTHORCONTACTS)
@Keywords(OMSPRESTEYTAYLORETPMODEL_KEYWORDS)
@Label(OMSPRESTEYTAYLORETPMODEL_LABEL)
@Name("_" + OMSPRESTEYTAYLORETPMODEL_NAME)
@Status(OMSPRESTEYTAYLORETPMODEL_STATUS)
@License(OMSPRESTEYTAYLORETPMODEL_LICENSE)
@UI(OMSPRESTEYTAYLORETPMODEL_UI)
public class PresteyTaylorEtpModel extends HMModel {

    @Description(OMSPRESTEYTAYLORETPMODEL_inNetradiation_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    @Unit("Watt m-2 ")
    public String inNetradiation;

    @Description(OMSPRESTEYTAYLORETPMODEL_pDailyDefaultNetradiation_DESCRIPTION)
    @In
    @Unit("Watt m-2")
    public double defaultDailyNetradiation = 300.0;

    @Description(OMSPRESTEYTAYLORETPMODEL_pHourlyDefaultNetradiation_DESCRIPTION)
    @In
    @Unit("Watt m-2")
    public double defaultHourlyNetradiation = 100.0;

    @Description(OMSPRESTEYTAYLORETPMODEL_doHourly_DESCRIPTION)
    @In
    public boolean doHourly;

    @Description(OMSPRESTEYTAYLORETPMODEL_fDataID_DESCRIPTION)
    @In
    public String fDataID;

    @Description(OMSPRESTEYTAYLORETPMODEL_inTemp_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    @Unit("C")
    public String inTemp;

    @Description(OMSPRESTEYTAYLORETPMODEL_pAlpha_DESCRIPTION)
    @In
    @Unit("m")
    public double pAlpha = 0;

    @Description(OMSPRESTEYTAYLORETPMODEL_pGmorn_DESCRIPTION)
    @In
    public double pGmorn = 0;

    @Description(OMSPRESTEYTAYLORETPMODEL_pGnight_DESCRIPTION)
    @In
    public double pGnight = 0;

    @Description(OMSPRESTEYTAYLORETPMODEL_defaultTemp_DESCRIPTION)
    @In
    @Unit("C")
    public double defaultTemp = 15.0;

    @Description(OMSPRESTEYTAYLORETPMODEL_inPressure_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    @Unit("KPa")
    public String inPressure;

    @Description(OMSPRESTEYTAYLORETPMODEL_defaultPressure_DESCRIPTION)
    @In
    @Unit("KPa")
    public double defaultPressure = 101.3;

    @Description(OMSPRESTEYTAYLORETPMODEL_outPTEtp_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Unit("mm hour-1")
    @In
    public String outPTEtp;

    @Execute
    public void process() throws Exception {

        OmsTimeSeriesIteratorReader tempReader = getTimeseriesReader(inTemp, fDataID);

        OmsTimeSeriesIteratorReader pressReader = null;
        if (inPressure != null)
            pressReader = getTimeseriesReader(inPressure, fDataID);
        OmsTimeSeriesIteratorReader netradReader = null;
        if (inNetradiation != null)
            netradReader = getTimeseriesReader(inNetradiation, fDataID);

        OmsPresteyTaylorEtpModel omspresteytayloretpmodel = new OmsPresteyTaylorEtpModel();
        omspresteytayloretpmodel.defaultPressure = defaultPressure;
        omspresteytayloretpmodel.pAlpha = pAlpha;
        omspresteytayloretpmodel.pGmorn = pGmorn;
        omspresteytayloretpmodel.pGnight = pGnight;
        omspresteytayloretpmodel.doHourly = doHourly;
        omspresteytayloretpmodel.pm = pm;
        omspresteytayloretpmodel.defaultDailyNetradiation = defaultDailyNetradiation;
        omspresteytayloretpmodel.defaultHourlyNetradiation = defaultHourlyNetradiation;
        omspresteytayloretpmodel.defaultTemp = defaultTemp;

        OmsTimeSeriesIteratorWriter writerCalculatedEtp = null;

        pm.beginTask("Processing...", IHMProgressMonitor.UNKNOWN);
        while( tempReader.doProcess ) {
            tempReader.nextRecord();

            HashMap<Integer, double[]> id2ValueMap = tempReader.outData;
            omspresteytayloretpmodel.inTemp = id2ValueMap;
            omspresteytayloretpmodel.tCurrent = tempReader.tCurrent;

            if (writerCalculatedEtp == null) {
                writerCalculatedEtp = new OmsTimeSeriesIteratorWriter();
                writerCalculatedEtp.file = outPTEtp;
                writerCalculatedEtp.tStart = tempReader.tStart;
                writerCalculatedEtp.tTimestep = tempReader.tTimestep;
            }

            pm.message("timestep: " + tempReader.tCurrent);

            if (pressReader != null) {
                pressReader.nextRecord();
                id2ValueMap = pressReader.outData;
                omspresteytayloretpmodel.inPressure = id2ValueMap;
            }

            if (netradReader != null) {
                netradReader.nextRecord();
                id2ValueMap = netradReader.outData;
                omspresteytayloretpmodel.inNetradiation = id2ValueMap;
            }
            omspresteytayloretpmodel.process();
            HashMap<Integer, double[]> outEtp = omspresteytayloretpmodel.outPTEtp;
            // write csv data
            writerCalculatedEtp.inData = outEtp;
            writerCalculatedEtp.writeNextLine();
        }
        pm.done();

        tempReader.close();
        if (pressReader != null)
            pressReader.close();
        if (netradReader != null)
            netradReader.close();
        writerCalculatedEtp.close();

    }

    private OmsTimeSeriesIteratorReader getTimeseriesReader( String path, String id ) throws URISyntaxException {
        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = path;
        reader.idfield = id;
        reader.fileNovalue = "-9999";
        reader.initProcess();
        return reader;
    }

}