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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.etp;

import java.util.HashMap;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.jgrasstools.gears.io.timedependent.TimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.TimeSeriesIteratorWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;

@Description("Calculates evapotranspiration.")
@Author(name = "Giuseppe Formetta, Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Evapotranspiration, Hydrologic")
@Label(JGTConstants.HYDROGEOMORPHOLOGY)
@UI(JGTConstants.ITERATOR_UI_HINT)
@Status(Status.EXPERIMENTAL)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class FaoEtpModel extends JGTModel {
    @UI(JGTConstants.FILEIN_UI_HINT)
    @Description("The net Radiation at the grass surface in W/m2 for the current hour.")
    @In
    @Unit("MJ m-2 hour-1")
    public String inNetradiation;

    @Description("The net Radiation default value in case of missing data.")
    @In
    @Unit("MJ m-2 hour-1")
    public double defaultNetradiation = 2.0;

    @UI(JGTConstants.FILEIN_UI_HINT)
    @Description("The average hourly wind speed.")
    @In
    @Unit("m s-1")
    public String inWind;

    @Description("The wind default value in case of missing data.")
    @In
    @Unit("m s-1")
    public double defaultWind = 2.0;

    @Description("The mean hourly air temperature.")
    @In
    @Unit("C")
    public String inTemp;

    @Description("The temperature default value in case of missing data.")
    @In
    @Unit("C")
    public double defaultTemp = 15.0;

    @Description("The average air hourly relative humidity.")
    @In
    @Unit("%")
    public String inRh;

    @Description("The humidity default value in case of missing data.")
    @In
    @Unit("%")
    public double defaultRh = 70.0;

    @Description("The atmospheric pressure in hPa.")
    @In
    @Unit("KPa")
    public String inPressure;

    @Description("The pressure default value in case of missing data.")
    @In
    @Unit("KPa")
    public double defaultPressure = 100.0;

    @Description("Station id field.")
    @In
    public String fId = "ID";

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @UI(JGTConstants.FILEOUT_UI_HINT)
    @Description("The reference evapotranspiration.")
    @Unit("mm hour-1")
    @Out
    public String outFaoEtp;

    @Execute
    public void process() throws Exception {
        checkNull(inNetradiation, inWind, inTemp, inRh, inPressure);

        FaoEtp faoEtp = new FaoEtp();

        TimeSeriesIteratorReader netradReader = getTimeseriesReader(inNetradiation, fId);
        TimeSeriesIteratorReader windReader = getTimeseriesReader(inWind, fId);
        TimeSeriesIteratorReader tempReader = getTimeseriesReader(inTemp, fId);
        TimeSeriesIteratorReader rhReader = getTimeseriesReader(inRh, fId);
        TimeSeriesIteratorReader pressureReader = getTimeseriesReader(inPressure, fId);

        TimeSeriesIteratorWriter outputWriter = new TimeSeriesIteratorWriter();
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

    private TimeSeriesIteratorReader getTimeseriesReader( String file, String id ) {
        TimeSeriesIteratorReader reader = new TimeSeriesIteratorReader();
        reader.file = file;
        reader.idfield = id;
        reader.fileNovalue = "-9999";
        reader.initProcess();
        return reader;
    }

}
