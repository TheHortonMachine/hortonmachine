package org.hortonmachine.hmachine.geoframe.io.database;

import java.util.HashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema.Station;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema.StationType;
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
public class TableUtils {

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
		return List.of(
				new EnvironmentalVariable(EnvironmentalVariableType.EVAPOTRANSPIRATION.getId(), "Evapotranspiration",
						mmFlux, "Evapotraspiration"),
				new EnvironmentalVariable(EnvironmentalVariableType.PRECIPITATION.getId(), "Precipitation", mmFlux,
						"Accumulated precipitation"),
				new EnvironmentalVariable(EnvironmentalVariableType.TEMPERATURE.getId(), "Temperature", temperatureUnit,
						"Air temperature"),
				new EnvironmentalVariable(EnvironmentalVariableType.RADIATION.getId(), "Radiation", radiationUnit,
						"Incoming solar radiation"),
				new EnvironmentalVariable(EnvironmentalVariableType.DISCHARGE.getId(), "Discharge", dischargeUnit,
						"River discharge"));
	}

	public final static HashMap<Integer, double[]> getLegacyHMInput(double[] h, int[] ids) {
		// TODO Auto-generated method stub
		HashMap<Integer, double[]> data = new HashMap<Integer, double[]>();
	

		try {
			for (int i = 0; i < ids.length; i++) {
				data.put(ids[i], new double[] { h[ids[i]] });
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}

	public final static int[] getIntIdArray(ASpatialDb inGeoframeDb, String tableName, String columnName,
			String where) {
		// TODO: pass as parameter result.data
		QueryResult result;
		try {
			String sql = "select * from " + tableName;
			if (where != null) {
				sql = sql + " " + where;
			}
			result = inGeoframeDb.getTableRecordsMapFromRawSql(sql, -1);

			int idIndex = result.names.indexOf(columnName);

			var rows = result.data;
			int l = result.data.size();
			int[] ids = new int[l];
			for (int i = 0; i < l; i++) {
				ids[i] = ((Number) rows.get(i)[idIndex]).intValue();
			}
			return ids;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	public final static HashMap<Integer, double[]> getLegacyHMInputNaN(int[] id) {
		// TODO Auto-generated method stub
		HashMap<Integer, double[]> data = new HashMap<Integer, double[]>();
		for (int i = 0; i < id.length; i++) {
			data.put(id[i], new double[] { HMConstants.doubleNovalue });
		}
		return data;
	}

}
