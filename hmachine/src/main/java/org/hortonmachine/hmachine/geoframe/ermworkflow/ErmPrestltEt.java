package org.hortonmachine.hmachine.geoframe.ermworkflow;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.geoframe.utils.IWaterBudgetSimulationRunner;
import org.hortonmachine.hmachine.geoframe.utils.PrestleyETAtCentroid;

public class ErmPrestltEt extends HMModel {
	// NOCE

	public ErmPrestltEt() {
		String geoframeGpkg = TestIO.GEOFRAME_GPK;
		try {
			ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();
			db.open(geoframeGpkg);
			var temperatureReader = new GeoframeEnvDatabaseIterator();
			temperatureReader.db = db;
			temperatureReader.pParameterId = EnvironmentalVariableType.TEMPERATURE.getId();

			temperatureReader.pMaxId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
			temperatureReader.tStart = TestIO.FROM_TS + ":00";
			temperatureReader.tEnd = TestIO.TO_TS + ":00";
			temperatureReader.doRawData = false;
			temperatureReader.preCacheData();

			var netReader = new GeoframeEnvDatabaseIterator();
			netReader.db = db;
			netReader.pParameterId = EnvironmentalVariableType.RADIATION.getId();
			netReader.pMaxId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
			netReader.tStart = TestIO.FROM_TS + ":00";
			netReader.tEnd = TestIO.TO_TS + ":00";
			netReader.doRawData = false;
			netReader.preCacheData();

			var ptEt = new PrestleyETAtCentroid();
			ptEt.inGeoframeDBPath = geoframeGpkg;
			ptEt.isHourly = true;
			ptEt.pAlpha = 1.26;
			ptEt.inTempReader = temperatureReader;
			ptEt.inNetReader = netReader;
			ptEt.pGmorn = 0.35;
			ptEt.pGnight = 0.75;
			ptEt.init();
			ptEt.process();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		new ErmPrestltEt();
	}

}
