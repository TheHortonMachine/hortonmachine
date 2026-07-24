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
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_inNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_inPressure_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_inTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_outPTEtp_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pAlpha_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pDailyDefaultNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pGmorn_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pGnight_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pHourlyDefaultNetradiation_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_time_DESCRIPTION;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

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

@Description(OMSPRESTEYTAYLORETPMODEL_DESCRIPTION)
@Author(name = OMSPRESTEYTAYLORETPMODEL_AUTHORNAMES, contact = OMSPRESTEYTAYLORETPMODEL_AUTHORCONTACTS)
@Keywords(OMSPRESTEYTAYLORETPMODEL_KEYWORDS)
@Label(OMSPRESTEYTAYLORETPMODEL_LABEL)
@Name(OMSPRESTEYTAYLORETPMODEL_NAME)
@Status(OMSPRESTEYTAYLORETPMODEL_STATUS)
@License(OMSPRESTEYTAYLORETPMODEL_LICENSE)
@UI(OMSPRESTEYTAYLORETPMODEL_UI)
public class OmsPresteyTaylorEtpModel extends HMModel {

	public static final double DEFAULT_DAILY_NET_RADIATION = 300.0;
	public static final double DEFAULT_HOURLY_NET_RADIATION = 100.0;
	public static final double DEFAULT_TEMPERATURE = 15.0;
	public static final double DEFAULT_PRESSURE = 100.0;

	private static final double RAD_TO_DAY = 0.0864;
	private static final double RAD_TO_HOUR = 0.0864 / 24.0;

	@Description(OMSPRESTEYTAYLORETPMODEL_inNetradiation_DESCRIPTION)
	@In
	@Unit("Watt m-2 ")
	public HashMap<Integer, double[]> inNetradiation;

	@Description(OMSPRESTEYTAYLORETPMODEL_pDailyDefaultNetradiation_DESCRIPTION)
	@In
	@Unit("Watt m-2")
	public double defaultDailyNetradiation = DEFAULT_DAILY_NET_RADIATION;

	@Description(OMSPRESTEYTAYLORETPMODEL_pHourlyDefaultNetradiation_DESCRIPTION)
	@In
	@Unit("Watt m-2")
	public double defaultHourlyNetradiation = DEFAULT_HOURLY_NET_RADIATION;

	@Description(OMSPRESTEYTAYLORETPMODEL_doHourly_DESCRIPTION)
	@In
	public boolean doHourly;

	@Description(OMSPRESTEYTAYLORETPMODEL_inTemp_DESCRIPTION)
	@In
	@Unit("C")
	public HashMap<Integer, double[]> inTemp;

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
	public double defaultTemp = DEFAULT_TEMPERATURE;

	@Description(OMSPRESTEYTAYLORETPMODEL_inPressure_DESCRIPTION)
	@In
	@Unit("KPa")
	public HashMap<Integer, double[]> inPressure;

	@Description(OMSPRESTEYTAYLORETPMODEL_defaultPressure_DESCRIPTION)
	@In
	@Unit("KPa")
	public double defaultPressure = DEFAULT_PRESSURE;

	@Description(OMSPRESTEYTAYLORETPMODEL_time_DESCRIPTION)
	@In
	public String tCurrent;

	@Description(OMSPRESTEYTAYLORETPMODEL_outPTEtp_DESCRIPTION)
	@Unit("mm hour-1")
	@Out
	public HashMap<Integer, double[]> outPTEtp;

	private static DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMMSS;

	@Execute
	public void process() throws Exception {
		checkNull(inTemp);

		outPTEtp = new HashMap<Integer, double[]>();
		Set<Entry<Integer, double[]>> entrySet = inTemp.entrySet();
		for (Entry<Integer, double[]> entry : entrySet) {
			Integer basinId = entry.getKey();

			double t = entry.getValue()[0];

			double inputNetRadiation = HMConstants.doubleNovalue;
			if (inNetradiation != null && inNetradiation.containsKey(basinId)) {
				inputNetRadiation = inNetradiation.get(basinId)[0];
			}

			double pressure = HMConstants.doubleNovalue;
			;
			if (inPressure != null && inPressure.containsKey(basinId)) {
				pressure = inPressure.get(basinId)[0];
			}

			double etp = getET(pGmorn, pGnight, pAlpha, inputNetRadiation,
					doHourly ? defaultHourlyNetradiation : defaultDailyNetradiation, t, defaultTemp, pressure,
					defaultPressure, doHourly, tCurrent);
			outPTEtp.put(basinId, new double[] { etp });
		}
	}

	public static double getNetRadiation(boolean doHourly, double inputNetRadiation,
			double defaulRadiationNetradiation) {

		double radiation;

		if (!isNovalue(inputNetRadiation)) {
			radiation = inputNetRadiation;
		} else {
			radiation = defaulRadiationNetradiation;
		}

		return doHourly ? radiation * RAD_TO_HOUR : radiation * RAD_TO_DAY;
	}

	public static double getET(double ggm, double ggn, double alpha, double netRad, double defaultNetradiation,
			double airTem, double defaultTemeperature, double atmPres, double defaultAtm, boolean isHourlyo,
			String tCurrent) {
		// check here

		double netradiation = getNetRadiation(isHourlyo, netRad, defaultNetradiation);
		DateTime currentDatetime = formatter.parseDateTime(tCurrent);

		int ora = currentDatetime.getHourOfDay();
		boolean isLigth = false;
		if (ora > 6 && ora < 18) {
			isLigth = true;
		}

		double temperature = !isNovalue(airTem) ? airTem : defaultTemeperature;
		double pressure = !isNovalue(atmPres) ? atmPres : defaultAtm;

		return compute(ggm, ggn, alpha, netradiation, temperature, pressure, isLigth, isHourlyo);
	}

	public static double compute(double ggm, double ggn, double alpha, double netRad, double airTem, double atmPres,
			boolean islight, boolean ishourlyo) {
		double result = 0;
		if (ishourlyo == true) {
			double den_Delta = (airTem + 237.3) * (airTem + 237.3);
			double exp_Delta = (17.27 * airTem) / (airTem + 237.3);
			double num_Delta = 4098 * (0.6108 * Math.exp(exp_Delta));
			double Delta = num_Delta / den_Delta;

			double lambda = 2.501 - 0.002361 * airTem;
			double gamma = 0.001013 * atmPres / (0.622 * lambda);

			double coeff_G;
			if (islight == true) {
				coeff_G = ggm;
			} else {
				coeff_G = ggn;
			}

			double G = coeff_G * netRad;

			result = (alpha) * Delta * (netRad - G) / ((gamma + Delta) * lambda);

		} else {
			double den_Delta = (airTem + 237.3) * (airTem + 237.3);
			double exp_Delta = (17.27 * airTem) / (airTem + 237.3);
			double num_Delta = 4098 * (0.6108 * Math.exp(exp_Delta));
			double Delta = num_Delta / den_Delta;

			double lambda = 2.501 - 0.002361 * airTem;
			double gamma = 0.001013 * atmPres / (0.622 * lambda);

			result = (alpha) * Delta * (netRad) / ((gamma + Delta) * lambda);

		}
		return result;
	}

}