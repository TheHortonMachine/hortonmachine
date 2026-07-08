package org.hortonmachine.hmachine.geoframe;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema;
import org.hortonmachine.hmachine.geoframe.utils.IWaterBudgetSimulationRunner;
import org.hortonmachine.hmachine.geoframe.utils.KrigingAtCentroid;

public class TestKriging extends HMModel {
	// NOCE

	public TestKriging() {
		String geoframeGpkg = TestIO.GEOFRAME_GPK;
		try {
			ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();
			db.open(geoframeGpkg);
			var valueReader = new GeoframeEnvDatabaseIterator();
			valueReader.db = db;
			valueReader.pParameterId = 4; // temperature
			valueReader.pMaxBasinId = 200000;
			valueReader.tStart = TestIO.FROM_TS + ":00";
			valueReader.tEnd = TestIO.TO_TS + ":00";
			valueReader.table = GeoFrameSimpleTable.RAW_METEO.tableName();
			var krigingInterpolator = new KrigingAtCentroid();
			krigingInterpolator.inGeoframeDBPath = geoframeGpkg;
			krigingInterpolator.inVariableType = VarSchema.EnvironmentalVariableType.TEMPERATURE.getId();
			krigingInterpolator.variableReader = valueReader;
			krigingInterpolator.cutoffDivide = 10;
			krigingInterpolator.process();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		new TestKriging();
	}

}
