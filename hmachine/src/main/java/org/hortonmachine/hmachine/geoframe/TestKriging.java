package org.hortonmachine.hmachine.geoframe;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoStationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoStationSchema.HydroMeteoStation;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema;
import org.hortonmachine.hmachine.geoframe.utils.KrigingAtCentroid;

public class TestKriging extends HMModel {
	// NOCE

	public TestKriging() {
		String geoframeGpkg ="/home/andreisd/Documents/project/data_hm/vermiglio_dtm/inputs/outputs/geoframe_data.gpkg";
		try {
			
			
			ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();
			db.open(geoframeGpkg);
			var valueReader = new GeoframeEnvDatabaseIterator();
			valueReader.db = db;
			valueReader.pParameterId = 4; // temperature
			valueReader.pMaxId = 200000;

			valueReader.tStart = TestIO.FROM_TS + ":00";
			valueReader.tEnd = TestIO.TO_TS + ":00";
			valueReader.doRawData = true;
			
			int maxId = db.getLong("select max("+ HydroMeteoStation.ID.columnName() +") from " + //
					GeoFrameGeoTable.HYDRO_METEO_STATION.tableName() + " WHERE " + //
					HydroMeteoStation.TYPE.columnName() + " = '" + HydroMeteoStationSchema.StationType.METEO + "'").intValue();
			valueReader.pMaxId = maxId;
			valueReader.preCacheData();
			
			var krigingInterpolator = new KrigingAtCentroid();
			krigingInterpolator.inGeoframeDBPath = geoframeGpkg;
			krigingInterpolator.inVariableType = VarSchema.EnvironmentalVariableType.TEMPERATURE.getId();
			krigingInterpolator.variableReader = valueReader;
			krigingInterpolator.cutoffDivide = 10;
			krigingInterpolator.init();
			krigingInterpolator.process();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		new TestKriging();
	}

}
