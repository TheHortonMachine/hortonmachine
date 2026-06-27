package org.hortonmachine.hmachine.geoframe.io.database;

import java.util.List;

import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.TimeResolution;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.VarField;

/**
 * Utility class for creating and validating database tables and populating
 * standard reference data such as variable definitions ({@link VarField}).
 *
 * @author Daniele Andreis
 */
public class CreateTableUtils {
	

	

	public final static List<EnvironmentalVariable> getFixedEnviramentalVariable(TimeResolution resolution) {

		String mmFlux = "mm/";
		String temperatureUnit = "°C";
		String radiationUnit = null;
		String dischargeUnit = "m³/s";

		if (resolution != null) {
			switch (resolution) {
			case HOURLY -> {
				mmFlux = mmFlux + "h";
				radiationUnit = "W/m²";
			}
			case DAILY -> {
				mmFlux = mmFlux + "day";
				radiationUnit = "MJ/m²/day";
			}
			case MONTHLY -> {
				mmFlux = mmFlux + "month";
				radiationUnit = "MJ/m²/month";
			}
			case YEARLY -> {
				mmFlux = mmFlux + "year";
				radiationUnit = "MJ/m²/year";
			}
			}
		} else {
			mmFlux = null;
		}

		/**
		 * TODO we can define potential evapotranspiration and actual
		 * evapotranspiration????
		 */
		return List.of(new EnvironmentalVariable(EnvironmentalVariableType.EVAPOTRANSPIRATION.getId(), "Evapotranspiration", mmFlux, "Evapotraspiration"),
				new EnvironmentalVariable(EnvironmentalVariableType.PRECIPITATION.getId(), "Precipitation", mmFlux, "Accumulated precipitation"),
				new EnvironmentalVariable(EnvironmentalVariableType.TEMPERATURE.getId(), "Temperature", temperatureUnit, "Air temperature"),
				new EnvironmentalVariable(EnvironmentalVariableType.RADIATION.getId(), "Radiation", radiationUnit, "Incoming solar radiation"),
				new EnvironmentalVariable(EnvironmentalVariableType.DISCHARGE.getId(), "Discharge", dischargeUnit, "River discharge"));
	}
}
