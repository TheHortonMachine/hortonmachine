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
import java.util.Set;
import java.util.Map.Entry;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

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
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@Description("Calculates evapotranspiration based on the Prestey-Taylor model.")
@Author(name = "Giuseppe Formetta, Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Evapotranspiration, Hydrologic")
@Label(JGTConstants.HYDROGEOMORPHOLOGY)
@UI(JGTConstants.ITERATOR_UI_HINT)
@Name("ptetp")
@Documentation("PresteyTaylorEtpModel.html")
@Status(Status.EXPERIMENTAL)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class PresteyTaylorEtpModel extends JGTModel {
    @Description("The net Radiation at the grass surface in W/m2 for the current hour.")
    @In
    @Unit("Watt m-2 ")
    public HashMap<Integer, double[]> inNetradiation;

    @Description("The net Radiation default value in case of missing data.")
    @In
    @Unit("Watt m-2")
    public double pDailyDefaultNetradiation = 300.0;

    @Description("The net Radiation default value in case of missing data.")
    @In
    @Unit("Watt m-2")
    public double pHourlyDefaultNetradiation = 100.0;

    @Description("Switch that defines if it is hourly.")
    @In
    @Unit("")
    public boolean doHourly;

    @Description("The mean hourly air temperature.")
    @In
    @Unit("C")
    public HashMap<Integer, double[]> inTemp;

    @Description("The alpha.")
    @In
    @Unit("m")
    public double pAlpha = 0;

    @Description("The g morning.")
    @In
    @Unit("")
    public double pGmorn = 0;
    
    @Description("The g nigth.")
    @In
    @Unit("")
    public double pGnight = 0;

    @Description("The temperature default value in case of missing data.")
    @In
    @Unit("C")
    public double defaultTemp = 15.0;

    @Description("The atmospheric pressure in KPa.")
    @In
    @Unit("KPa")
    public HashMap<Integer, double[]> inPressure;

    @Description("The pressure default value in case of missing data.")
    @In
    @Unit("KPa")
    public double defaultPressure = 100.0;

    @Description("The current time.")
    @In
    @Unit("C")
    public String time;
    @Description("Station id field.")
    @In
    public String fId = "ID";

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The reference evapotranspiration.")
    @Unit("mm hour-1")
    @Out
    public HashMap<Integer, double[]> outPTEtp;

    @Execute
    public void process() throws Exception {
        checkNull(inTemp);
        
        outPTEtp = new HashMap<Integer, double[]>();
        Set<Entry<Integer, double[]>> entrySet = inTemp.entrySet();
        for( Entry<Integer, double[]> entry : entrySet ) {
            Integer basinId = entry.getKey();

            double temp = defaultTemp;
            if (inTemp != null) {
                double t = entry.getValue()[0];
                if (!isNovalue(t)) {
                    temp = t;
                }
            }

            double netradiation = 0;
            if (inNetradiation != null) {
                double n = inNetradiation.get(basinId)[0];
                if (isNovalue(n)) {

                    if (doHourly == true) {
                        netradiation = pHourlyDefaultNetradiation * 0.0864 / 24.0;
                    } else {
                        netradiation = pDailyDefaultNetradiation * 0.0864;

                    }

                } else {
                    if (doHourly == true) {
                        netradiation = n * 0.0864 / 24.0;
                    } else {
                        netradiation = n * 0.0864;

                    }
                }

            }

            double pressure = defaultPressure;
            if (inPressure != null) {
                double p = inPressure.get(basinId)[0];
                if (isNovalue(p)) {
                    pressure = defaultPressure;
                } else {
                    pressure = p;
                }
            }

            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").withZone(DateTimeZone.UTC);
            DateTime currentDatetime = formatter.parseDateTime(time);
            int ora = (currentDatetime.getHourOfDay());
            boolean isLigth = false;
            if (ora > 6 && ora < 18) {
                isLigth = true;
            }

            double etp = compute2(pGmorn, pGnight, pAlpha, netradiation, temp, pressure, isLigth, doHourly);
            outPTEtp.put(basinId, new double[]{etp});
        }
    }

    private double compute2( double ggm, double ggn, double alpha, double NetRad, double AirTem, double AtmPres, boolean islight,
            boolean ishourlyo ) {
        double result = 0;
        if (ishourlyo == true) {
            double den_Delta = (AirTem + 237.3) * (AirTem + 237.3);
            double exp_Delta = (17.27 * AirTem) / (AirTem + 237.3);
            double num_Delta = 4098 * (0.6108 * Math.exp(exp_Delta));
            double Delta = num_Delta / den_Delta;

            double lambda = 2.501 - 0.002361 * AirTem;
            double gamma = 0.001013 * AtmPres / (0.622 * lambda);

            double coeff_G;
            if (islight == true) {
                coeff_G = ggm;
            } else {
                coeff_G = ggn;
            }

            double G = coeff_G * NetRad;

            result = (alpha) * Delta * (NetRad - G) / ((gamma + Delta) * lambda);

        } else {
            double den_Delta = (AirTem + 237.3) * (AirTem + 237.3);
            double exp_Delta = (17.27 * AirTem) / (AirTem + 237.3);
            double num_Delta = 4098 * (0.6108 * Math.exp(exp_Delta));
            double Delta = num_Delta / den_Delta;

            double lambda = 2.501 - 0.002361 * AirTem;
            double gamma = 0.001013 * AtmPres / (0.622 * lambda);

            result = (alpha) * Delta * (NetRad) / ((gamma + Delta) * lambda);

        }
        return result;
    }

}