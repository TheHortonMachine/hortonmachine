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

import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_UI;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_defaultMaxTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_defaultMinTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_defaultNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_defaultPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_defaultRh_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_defaultWind_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_inMaxTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_inMinTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_inNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_inPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_inRh_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_inWind_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETPDAILY_outFaoEtp_DESCRIPTION;

import java.util.HashMap;
import java.util.Map.Entry;

import org.hortonmachine.gears.libs.modules.HMModel;

import java.util.Set;

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

@Description(OMSFAOETPDAILY_DESCRIPTION)
@Author(name = OMSFAOETPDAILY_AUTHORNAMES, contact = OMSFAOETPDAILY_AUTHORCONTACTS)
@Keywords(OMSFAOETPDAILY_KEYWORDS)
@Label(OMSFAOETPDAILY_LABEL)
@Name(OMSFAOETPDAILY_NAME)
@Status(OMSFAOETPDAILY_STATUS)
@License(OMSFAOETPDAILY_LICENSE)
@UI(OMSFAOETPDAILY_UI)
public class OmsFaoEtpDaily extends HMModel {

    @Description(OMSFAOETPDAILY_inNetradiation_DESCRIPTION)
    @In
    @Unit("MJ m-2 day-1")
    public HashMap<Integer, double[]> inNetradiation;

    @Description(OMSFAOETPDAILY_defaultNetradiation_DESCRIPTION)
    @In
    @Unit("MJ m-2 day-1")
    public double defaultNetradiation = 2.0;

    @Description(OMSFAOETPDAILY_inWind_DESCRIPTION)
    @In
    @Unit("m s-1")
    public HashMap<Integer, double[]> inWind;

    @Description(OMSFAOETPDAILY_defaultWind_DESCRIPTION)
    @In
    @Unit("m s-1")
    public double defaultWind = 2.0;

    @Description(OMSFAOETPDAILY_inMaxTemp_DESCRIPTION)
    @In
    @Unit("C")
    public HashMap<Integer, double[]> inMaxTemp;

    @Description(OMSFAOETPDAILY_inMinTemp_DESCRIPTION)
    @In
    @Unit("C")
    public HashMap<Integer, double[]> inMinTemp;

    @Description(OMSFAOETPDAILY_defaultMaxTemp_DESCRIPTION)
    @In
    @Unit("C")
    public double defaultMaxTemp = 15.0;

    @Description(OMSFAOETPDAILY_defaultMinTemp_DESCRIPTION)
    @In
    @Unit("C")
    public double defaultMinTemp = 0.0;

    @Description(OMSFAOETPDAILY_inRh_DESCRIPTION)
    @In
    @Unit("%")
    public HashMap<Integer, double[]> inRh;

    @Description(OMSFAOETPDAILY_defaultRh_DESCRIPTION)
    @In
    @Unit("%")
    public double defaultRh = 70.0;

    @Description(OMSFAOETPDAILY_inPressure_DESCRIPTION)
    @In
    @Unit("KPa")
    public HashMap<Integer, double[]> inPressure;

    @Description(OMSFAOETPDAILY_defaultPressure_DESCRIPTION)
    @In
    @Unit("KPa")
    public double defaultPressure = 100.0;

    // TODO Add the elevation value in case of missing P data

    @Description(OMSFAOETPDAILY_outFaoEtp_DESCRIPTION)
    @Unit("mm day-1")
    @Out
    public HashMap<Integer, double[]> outFaoEtp;

    @Execute
    public void process() throws Exception {

        outFaoEtp = new HashMap<Integer, double[]>();

        Set<Entry<Integer, double[]>> entrySet = inMaxTemp.entrySet();
        for( Entry<Integer, double[]> entry : entrySet ) {
            Integer basinId = entry.getKey();

            double maxTemperature = defaultMaxTemp;
            if (inMaxTemp != null) {
                maxTemperature = entry.getValue()[0];
            }

            double minTemperature = defaultMinTemp;
            if (inMinTemp != null) {
                minTemperature = inMinTemp.get(basinId)[0];
            }

            double netradiation = defaultNetradiation;
            if (inNetradiation != null) {
                netradiation = inNetradiation.get(basinId)[0] * 3.6 / 1000.0;
            }

            double wind = defaultWind;
            if (inWind != null) {
                wind = inWind.get(basinId)[0];
            }

            double pressure = defaultPressure;
            if (inPressure != null) {
                pressure = inPressure.get(basinId)[0] / 10.0;
            }

            double rh = defaultRh;
            if (inRh != null) {
                rh = inRh.get(basinId)[0];
            }

            double etp = compute(netradiation, wind, maxTemperature, minTemperature, rh, pressure);
            outFaoEtp.put(basinId, new double[]{etp});
        }
    }

    private double compute( double netradiation, double wind, double maxtemperature, double mintemperature, double rh,
            double pressure ) {

        // Computation of Delta [KPa °C-1]
        double meanTemperature = (maxtemperature + mintemperature) / 2.0;
        double denDelta = pow(meanTemperature + 237.3, 2);
        double expDelta = (17.27 * meanTemperature) / (meanTemperature + 237.3);
        double expDeltaMax = (17.27 * maxtemperature) / (maxtemperature + 237.3);
        double expDeltaMin = (17.27 * mintemperature) / (mintemperature + 237.3);
        double numDelta = 4098 * (0.6108 * exp(expDelta));
        double delta = numDelta / denDelta;
        //pm.message("delta = " + delta); //$NON-NLS-1$

        // End Computation of Delta

        // Computation of Psicrometric constant gamma[kPa °C-1]

        double gamma = 0.665 * 0.001 * pressure;
        //pm.message("gamma = " + gamma); //$NON-NLS-1$
        // End Computation of Psicrometric constant gamma

        // Computation of mean saturation vapour pressure e0_AirTem [kPa]

        double e0_AirTemMax = 0.6108 * exp(expDeltaMax);
        double e0_AirTemMin = 0.6108 * exp(expDeltaMin);
        double es = (e0_AirTemMax + e0_AirTemMin) / 2.0;
        //pm.message("e0_AirTem = " + es); //$NON-NLS-1$

        // End of computation of mean saturation vapour pressure e0_AirTem

        // Computation of average hourly actual vapour pressure ea [kPa]

        double ea = rh / 100.0 * ((e0_AirTemMax + e0_AirTemMin) / 2.0);

        //pm.message("ea = " + ea); //$NON-NLS-1$

        // End of computation average hourly actual vapour pressure ea

        // compute the daily evapotranspiration in mm/day
        double num1 = 0.408 * delta * netradiation;
        double num2 = gamma * 900.0 / (meanTemperature + 273) * wind * (es - ea);
        double den = delta + gamma * (1 + 0.34 * wind);
        double result = (num1 + num2) / den;
        return result;
    }
}
