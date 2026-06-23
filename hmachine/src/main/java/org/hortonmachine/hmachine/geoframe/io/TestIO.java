package org.hortonmachine.hmachine.geoframe.io;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.database.importer.GeoFrameRawDataImporter;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoSationSchema.StationType;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.TimeResolution;

public class TestIO extends HMModel {
	// NOCE

	public TestIO() {
		String geoframeGpkg = "/home/andreisd/Desktop/geoframe_data.gpkg";

		ASpatialDb db;
		try {
			db = EDb.GEOPACKAGE.getSpatialDb();

			db.open(geoframeGpkg);

			var gfImporter = new GeoFrameRawDataImporter();
			gfImporter.stationType = StationType.METEO;
			gfImporter.inMeasurementDataFilePath ="/home/andreisd/Documents/project/uni/ARTICOLO_KRIGING/project_grid/data/meteo_data/temperature_gf_a.csv";
			gfImporter.inIdField ="ID";
			gfImporter.inElevationField ="z_dem";

			gfImporter.inMeasurementsPointFilePath="/home/andreisd/Documents/project/uni/ARTICOLO_KRIGING/project_grid/data/meteo_data/stations_tot.shp";
			gfImporter.inGeoframeDBPath=geoframeGpkg;
			gfImporter.inStartDate ="1990-01-01 01:00";
			gfImporter.inEndDate = "1991-01-03 01:00";
			gfImporter.inVariableType=2;
			gfImporter.timeResolution = TimeResolution.HOURLY;
			gfImporter.process();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		new TestIO();
		;
	}

}
