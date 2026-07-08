package org.hortonmachine.hmachine.geoframe;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.geoframe.utils.IWaterBudgetSimulationRunner;
import org.hortonmachine.hmachine.geoframe.utils.KrigingAtCentroid;
import org.hortonmachine.hmachine.geoframe.utils.PrestleyETAtCentroid;
import org.hortonmachine.hmachine.geoframe.utils.RadiationAtCentroid;

public class TestRadiation extends HMModel {
	// NOCE

	public TestRadiation() {
		String geoframeGpkg = TestIO.GEOFRAME_GPK;
		try {
			ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();
			db.open(geoframeGpkg);
			var temperatureReader = new GeoframeEnvDatabaseIterator();
			temperatureReader.db = db;
			temperatureReader.pParameterId = EnvironmentalVariableType.TEMPERATURE.getId(); // temperature
			temperatureReader.pMaxBasinId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
			temperatureReader.tStart = TestIO.FROM_TS + ":00";
			temperatureReader.tEnd = TestIO.TO_TS + ":00";
			temperatureReader.table = GeoFrameSimpleTable.HYDROMETEO.tableName();

			var netReader = new GeoframeEnvDatabaseIterator();
			netReader.db = db;
			netReader.pParameterId = EnvironmentalVariableType.RADIATION.getId(); // temperature
			netReader.pMaxBasinId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
			netReader.tStart = TestIO.FROM_TS + ":00";
			netReader.tEnd = TestIO.TO_TS + ":00";
			netReader.table = GeoFrameSimpleTable.HYDROMETEO.tableName();
;

			var radiation = new RadiationAtCentroid();
			radiation.inGeoframeDBPath = geoframeGpkg;
			radiation.variableReader = temperatureReader;
			radiation.doHourly = true;
			radiation.process();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		new TestRadiation();
	}

}
