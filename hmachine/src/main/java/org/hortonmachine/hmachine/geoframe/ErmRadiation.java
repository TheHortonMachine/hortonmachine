package org.hortonmachine.hmachine.geoframe;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.geoframe.utils.IWaterBudgetSimulationRunner;
import org.hortonmachine.hmachine.geoframe.utils.RadiationAtCentroid;

/**
 * Simple launcher to get radiation.
 * 
 * It take all parameter possible as default and in clear square condition. Only
 * temperature are known no humidity and clearness index.
 */
public class ErmRadiation extends HMModel {
	// NOCE

	public ErmRadiation() {
		String geoframeGpkg = TestIO.GEOFRAME_GPK;
		try {
			ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();
			db.open(geoframeGpkg);
			var temperatureReader = new GeoframeEnvDatabaseIterator();
			temperatureReader.db = db;
			temperatureReader.pParameterId = EnvironmentalVariableType.TEMPERATURE.getId(); // temperature
			temperatureReader.pMaxId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
			temperatureReader.tStart = TestIO.FROM_TS + ":00";
			temperatureReader.tEnd = TestIO.TO_TS + ":00";
			temperatureReader.doRawData = false;
			temperatureReader.preCacheData();

			var radiation = new RadiationAtCentroid();
			radiation.inGeoframeDBPath = geoframeGpkg;
			radiation.inTemperatureReader = temperatureReader;
			radiation.dem = "/home/andreisd/Documents/project/data_hm/vermiglio_dtm/inputs/outputs/pit.tif";
			radiation.inSkyview = "/home/andreisd/Documents/project/data_hm/vermiglio_dtm/inputs/outputs/skyview.tif";
			radiation.lwrvModeel = "6";
			radiation.doHourly = true;
			radiation.init();
			radiation.process();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		new ErmRadiation();
	}

}
