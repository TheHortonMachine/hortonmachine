package org.hortonmachine.hmachine.geoframe;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema;
import org.hortonmachine.hmachine.geoframe.utils.IWaterBudgetSimulationRunner;
import org.hortonmachine.hmachine.geoframe.utils.KrigingAtCentroid;
import org.hortonmachine.hmachine.geoframe.utils.VariableEvaluatorAtCentroid;

public class TestKriging extends HMModel {
	// NOCE

	public TestKriging() {
		String geoframeGpkg = "/home/andreisd/Desktop/geoframe_data.gpkg";

		try {
			ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();
			db.open(geoframeGpkg);
			var valueReader = new GeoframeEnvDatabaseIterator();
			valueReader.db = db;
			valueReader.pParameterId = 4; // temperature
			valueReader.pMaxBasinId = IWaterBudgetSimulationRunner.getMaxBasinId(db);

			valueReader.tStart = TestIO.FROM_TS;
			valueReader.tEnd = TestIO.TO_TS;

			var krigingInterpolator = new KrigingAtCentroid();
			krigingInterpolator.inGeoframeDBPath = geoframeGpkg;
			krigingInterpolator.inVariableType = VarSchema.EnvironmentalVariableType.TEMPERATURE.getId();
			krigingInterpolator.variableReader = valueReader;
			krigingInterpolator.cutoffDivide = 10;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		new TestKriging();
	}

}
