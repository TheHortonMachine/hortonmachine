package org.hortonmachine.hmachine.geoframe.io;

import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.database.importer.GeoFrameRawDataImporter;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoSationSchema.StationType;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.TimeResolution;

public class TestIO extends HMModel {
	// NOCE

	public TestIO() {
		String geoframeGpkg = "/home/andreisd/Desktop/geoframe_data.gpkg";

		try {

			var gfImporter = new GeoFrameRawDataImporter();
			// import the meteo network and the temperature (overwrite the previous)
			gfImporter.stationType = StationType.METEO;
			gfImporter.inMeasurementDataFilePath = "/home/andreisd/Documents/project/uni/ARTICOLO_KRIGING/project_grid/data/meteo_data/temperature_gf_a.csv";
			gfImporter.inIdField = "ID";
			gfImporter.inElevationField = "z_dem";
			gfImporter.doOverWrite = true;

			gfImporter.inMeasurementsPointFilePath = "/home/andreisd/Documents/project/uni/ARTICOLO_KRIGING/project_grid/data/meteo_data/stations_tot.shp";
			gfImporter.inGeoframeDBPath = geoframeGpkg;
			gfImporter.inStartDate = "1990-01-01 01:00";
			gfImporter.inEndDate = "1991-01-03 01:00";
			gfImporter.inVariableType = 4;
			gfImporter.timeResolution = TimeResolution.HOURLY;
			gfImporter.process();
			// import the precipitation

			gfImporter.inMeasurementDataFilePath = "/home/andreisd/Documents/project/uni/ARTICOLO_KRIGING/project_grid/data/meteo_data/precipitation_cleaned.csv";
			gfImporter.doOverWrite = false;
			gfImporter.inMeasurementsPointFilePath = null;
			gfImporter.inGeoframeDBPath = geoframeGpkg;
			gfImporter.inVariableType = 2;
			gfImporter.process();
			// import the stream gauge and the first stream gauge (overwrite the previous)
			gfImporter.stationType = StationType.STREAM_GAUGE;
			gfImporter.inMeasurementDataFilePath = "/home/andreisd/Documents/project/uni/ARTICOLO_KRIGING/project_grid/data/discharge_data/Q_male.csv";
			gfImporter.inIdField = "idstazione";
			gfImporter.inElevationField = null;
			gfImporter.doOverWrite = false;

			gfImporter.inMeasurementsPointFilePath = "/home/andreisd/Documents/project/uni/ARTICOLO_KRIGING/project_grid/data/Trentino/Noce/idrometri_sgiustinacorto_pochi.shp";
			gfImporter.inGeoframeDBPath = geoframeGpkg;
			gfImporter.inStartDate = null;
			gfImporter.inEndDate = null;
			gfImporter.inVariableType = 5;
			gfImporter.timeResolution = TimeResolution.HOURLY;
			gfImporter.process();

			// import the second stream gauge (overwrite the previous)
			gfImporter.doOverWrite = false;
			gfImporter.inMeasurementDataFilePath = "/home/andreisd/Documents/project/uni/ARTICOLO_KRIGING/project_grid/data/discharge_data/Q_vermiglio.csv";
			gfImporter.inGeoframeDBPath = geoframeGpkg;
			gfImporter.inVariableType = 5;
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
