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

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_UI;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_defaultNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_defaultPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_defaultRh_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_defaultTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_defaultWind_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_inNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_inPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_inRh_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_inTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_inWind_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFAOETP_outFaoEtp_DESCRIPTION;

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

@Description(OMSFAOETP_DESCRIPTION)
@Author(name = OMSFAOETP_AUTHORNAMES, contact = OMSFAOETP_AUTHORCONTACTS)
@Keywords(OMSFAOETP_KEYWORDS)
@Label(OMSFAOETP_LABEL)
@Name(OMSFAOETP_NAME)
@Status(OMSFAOETP_STATUS)
@License(OMSFAOETP_LICENSE)
@UI(OMSFAOETP_UI)
public class OmsFaoEtp extends HMModel {

    @Description(OMSFAOETP_inNetradiation_DESCRIPTION)
    @In
    @Unit("MJ m-2 hour-1")
    public HashMap<Integer, double[]> inNetradiation;

    @Description(OMSFAOETP_defaultNetradiation_DESCRIPTION)
    @In
    @Unit("MJ m-2 hour-1")
    public double defaultNetradiation = 2.0;

    @Description(OMSFAOETP_inWind_DESCRIPTION)
    @In
    @Unit("m s-1")
    public HashMap<Integer, double[]> inWind;

    @Description(OMSFAOETP_defaultWind_DESCRIPTION)
    @In
    @Unit("m s-1")
    public double defaultWind = 2.0;

    @Description(OMSFAOETP_inTemp_DESCRIPTION)
    @In
    @Unit("C")
    public HashMap<Integer, double[]> inTemp;

    @Description(OMSFAOETP_defaultTemp_DESCRIPTION)
    @In
    @Unit("C")
    public double defaultTemp = 15.0;

    @Description(OMSFAOETP_inRh_DESCRIPTION)
    @In
    @Unit("%")
    public HashMap<Integer, double[]> inRh;

    @Description(OMSFAOETP_defaultRh_DESCRIPTION)
    @In
    @Unit("%")
    public double defaultRh = 70.0;

    @Description(OMSFAOETP_inPressure_DESCRIPTION)
    @In
    @Unit("KPa")
    public HashMap<Integer, double[]> inPressure;

    @Description(OMSFAOETP_defaultPressure_DESCRIPTION)
    @In
    @Unit("KPa")
    public double defaultPressure = 100.0;

    @Description(OMSFAOETP_outFaoEtp_DESCRIPTION)
    @Unit("mm hour-1")
    @Out
    public HashMap<Integer, double[]> outFaoEtp;

    @Execute
    public void process() throws Exception {

        checkNull(inNetradiation, inWind, inTemp, inRh, inPressure);

        outFaoEtp = new HashMap<Integer, double[]>();

        Set<Entry<Integer, double[]>> entrySet = inTemp.entrySet();
        for( Entry<Integer, double[]> entry : entrySet ) {
            Integer basinId = entry.getKey();

            double temperature = entry.getValue()[0];
            if (isNovalue(temperature)) {
                temperature = defaultTemp;
            }

            double netradiation = inNetradiation.get(basinId)[0];
            if (isNovalue(netradiation)) {
                netradiation = defaultNetradiation;
            } else {
                netradiation = inNetradiation.get(basinId)[0] * 3.6 / 1000.0;
            }

            double wind = inWind.get(basinId)[0];
            if (isNovalue(wind)) {
                wind = defaultWind;
            }

            double pressure = inPressure.get(basinId)[0];
            if (isNovalue(pressure)) {
                pressure = defaultPressure;
            } else {
                pressure = inPressure.get(basinId)[0] / 10.0;
            }

            double rh = inRh.get(basinId)[0];
            if (isNovalue(rh)) {
                rh = defaultRh;
            }

            double etp = compute(netradiation, wind, temperature, rh, pressure);
            outFaoEtp.put(basinId, new double[]{etp});
        }

    }

    private double compute( double netradiation, double wind, double temperature, double rh, double pressure ) {

        // Computation of Delta [KPa °C-1]

        double denDelta = (temperature + 237.3) * (temperature + 237.3);
        double expDelta = (17.27 * temperature) / (temperature + 237.3);
        double numDelta = 4098 * (0.6108 * Math.exp(expDelta));
        double delta = numDelta / denDelta;
        pm.message("delta = " + delta); //$NON-NLS-1$

        // End Computation of Delta

        // Computation of Psicrometric constant gamma[kPa °C-1]

        double gamma = 0.665 * 0.001 * pressure;
        pm.message("gamma = " + gamma); //$NON-NLS-1$
        // End Computation of Psicrometric constant gamma

        // Computation of mean saturation vapour pressure e0_AirTem [kPa]

        double e0_AirTem = 0.6108 * Math.exp(expDelta);
        pm.message("e0_AirTem = " + e0_AirTem); //$NON-NLS-1$

        // End of computation of mean saturation vapour pressure e0_AirTem

        // Computation of average hourly actual vapour pressure ea [kPa]

        double ea = e0_AirTem * rh / 100;

        pm.message("ea = " + ea); //$NON-NLS-1$

        // End of computation average hourly actual vapour pressure ea

        // Computation of Soil heat flux [MJ m-2 day-1]
        boolean islight = true;
        double coeff_G;
        if (islight == true) {
            coeff_G = 0.1;
        } else {
            coeff_G = 0.5;
        }
        double G = coeff_G * netradiation;
        pm.message("G = " + G); //$NON-NLS-1$

        // End of computation of Soil heat flux

        double num1 = 0.408 * delta * (netradiation - G);
        double num2 = (37 * gamma * wind * (e0_AirTem - ea)) / (temperature + 273);
        double den = delta + gamma * (1 + 0.34 * wind);
        double result = (num1 + num2) / den;
        return result;
    }
}
