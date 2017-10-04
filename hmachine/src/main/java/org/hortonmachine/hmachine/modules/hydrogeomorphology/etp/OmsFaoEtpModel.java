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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.etp;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_UI;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_defaultNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_defaultPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_defaultRh_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_defaultTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_defaultWind_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_fId_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_inNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_inPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_inRh_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_inTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_inWind_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPMODEL_outFaoEtp_DESCRIPTION;

import java.util.HashMap;

import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

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
import oms3.annotations.Unit;

@Description(OMSFAOETPMODEL_DESCRIPTION)
@Author(name = OMSFAOETPMODEL_AUTHORNAMES, contact = OMSFAOETPMODEL_AUTHORCONTACTS)
@Keywords(OMSFAOETPMODEL_KEYWORDS)
@Label(OMSFAOETPMODEL_LABEL)
@Name(OMSFAOETPMODEL_NAME)
@Status(OMSFAOETPMODEL_STATUS)
@License(OMSFAOETPMODEL_LICENSE)
@UI(OMSFAOETPMODEL_UI)
public class OmsFaoEtpModel extends HMModel {

    @Description(OMSFAOETPMODEL_inNetradiation_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    @Unit("MJ m-2 hour-1")
    public String inNetradiation;

    @Description(OMSFAOETPMODEL_defaultNetradiation_DESCRIPTION)
    @In
    @Unit("MJ m-2 hour-1")
    public double defaultNetradiation = 2.0;

    @Description(OMSFAOETPMODEL_inWind_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    @Unit("m s-1")
    public String inWind;

    @Description(OMSFAOETPMODEL_defaultWind_DESCRIPTION)
    @In
    @Unit("m s-1")
    public double defaultWind = 2.0;

    @Description(OMSFAOETPMODEL_inTemp_DESCRIPTION)
    @In
    @Unit("C")
    public String inTemp;

    @Description(OMSFAOETPMODEL_defaultTemp_DESCRIPTION)
    @In
    @Unit("C")
    public double defaultTemp = 15.0;

    @Description(OMSFAOETPMODEL_inRh_DESCRIPTION)
    @In
    @Unit("%")
    public String inRh;

    @Description(OMSFAOETPMODEL_defaultRh_DESCRIPTION)
    @In
    @Unit("%")
    public double defaultRh = 70.0;

    @Description(OMSFAOETPMODEL_inPressure_DESCRIPTION)
    @In
    @Unit("KPa")
    public String inPressure;

    @Description(OMSFAOETPMODEL_defaultPressure_DESCRIPTION)
    @In
    @Unit("KPa")
    public double defaultPressure = 100.0;

    @Description(OMSFAOETPMODEL_fId_DESCRIPTION)
    @In
    public String fId = "ID";

    @Description(OMSFAOETPMODEL_outFaoEtp_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Unit("mm hour-1")
    @Out
    public String outFaoEtp;

    @Execute
    public void process() throws Exception {
        checkNull(inNetradiation, inWind, inTemp, inRh, inPressure);

        OmsFaoEtp faoEtp = new OmsFaoEtp();

        OmsTimeSeriesIteratorReader netradReader = getTimeseriesReader(inNetradiation, fId);
        OmsTimeSeriesIteratorReader windReader = getTimeseriesReader(inWind, fId);
        OmsTimeSeriesIteratorReader tempReader = getTimeseriesReader(inTemp, fId);
        OmsTimeSeriesIteratorReader rhReader = getTimeseriesReader(inRh, fId);
        OmsTimeSeriesIteratorReader pressureReader = getTimeseriesReader(inPressure, fId);

        OmsTimeSeriesIteratorWriter outputWriter = new OmsTimeSeriesIteratorWriter();
        outputWriter.file = outFaoEtp;

        try {
            while( netradReader.doProcess ) {
                netradReader.nextRecord();
                HashMap<Integer, double[]> id2ValueMap = netradReader.outData;
                faoEtp.inNetradiation = id2ValueMap;

                windReader.nextRecord();
                id2ValueMap = windReader.outData;
                faoEtp.inWind = id2ValueMap;

                tempReader.nextRecord();
                id2ValueMap = tempReader.outData;
                faoEtp.inTemp = id2ValueMap;

                rhReader.nextRecord();
                id2ValueMap = rhReader.outData;
                faoEtp.inRh = id2ValueMap;

                pressureReader.nextRecord();
                id2ValueMap = pressureReader.outData;
                faoEtp.inPressure = id2ValueMap;

                faoEtp.defaultNetradiation = defaultNetradiation;
                faoEtp.defaultPressure = defaultPressure;
                faoEtp.defaultRh = defaultRh;
                faoEtp.defaultTemp = defaultTemp;
                faoEtp.defaultWind = defaultWind;

                faoEtp.pm = pm;
                faoEtp.process();

                HashMap<Integer, double[]> outEtp = faoEtp.outFaoEtp;
                outputWriter.inData = outEtp;
                outputWriter.writeNextLine();
            }
        } finally {
            netradReader.close();
            pressureReader.close();
            windReader.close();
            tempReader.close();
            rhReader.close();
            outputWriter.close();
        }
    }

    private OmsTimeSeriesIteratorReader getTimeseriesReader( String file, String id ) {
        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = file;
        reader.idfield = id;
        reader.fileNovalue = "-9999";
        reader.initProcess();
        return reader;
    }

}
