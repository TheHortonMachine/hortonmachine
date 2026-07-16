package org.hortonmachine.hmachine.geoframe.ermworkflow;

import java.nio.file.Files;
import java.nio.file.Path;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.database.importer.GeoframeRawDataImporter;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema.StationType;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.TimeResolution;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Importer of raw meteo and stream gauge data into the GeoFrame database.")
@Author(name = "Daniele Andreis", contact = "")
@Keywords("ERM, GeoFrame, DEM, basin, network, data preparation")
@Label("GeoFrame")
@Name("ermRawDataImporter")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class ErmStationDataImporter extends HMModel {

	@Description("Input geoframe data geopackage.")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inGpkg;

	@Description("Data import start timestamp.")
	@In
	public String pStartTimestamp;

	@Description("Data import end timestamp.")
	@In
	public String pEndTimestamp;

	@Description("Data time resolution.")
	@In
	public TimeResolution pTimeResolution = TimeResolution.HOURLY;

	@Description("Meteo stations layer.")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inMeteoStations;

	@Description("Station id field in meteo csv files.")
	@In
	public String pMeteoIdField = "ID";

	@Description("Temperatures csv file.")
	@UI(HMConstants.FILEIN_UI_HINT_CSV)
	@In
	public String inTemperaturesCsv;

	@Description("Precipitation csv file.")
	@UI(HMConstants.FILEIN_UI_HINT_CSV)
	@In
	public String inPrecipitationCsv;

	@Description("Stream Gauges layer.")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inStreamGauges;

	@Description("Streamgauges id field in csv files.")
	@In
	public String pStreamGaugesIdField = "ID";

	@Description("Stream Gauges data csv file.")
	@UI(HMConstants.FILEIN_UI_HINT_CSV)
	@In
	public String inStreamGaugesCsv;

	@Execute
	public void process() throws Exception {
		checkNull(pStartTimestamp, pEndTimestamp);
		checkFileExists(inGpkg);
		var gfImporter = new GeoframeRawDataImporter();
		gfImporter.inGeoframeDBPath = inGpkg;
		gfImporter.inStartDate = pStartTimestamp;
		gfImporter.inEndDate = pEndTimestamp;
		gfImporter.inElevationField = "z_dem";
		gfImporter.inIdField = pMeteoIdField;
		gfImporter.timeResolution = pTimeResolution;
		gfImporter.doOverWrite = true;

		// import TEMPERATURE
		if (Files.exists(Path.of(inTemperaturesCsv))) {
			gfImporter.inMeasurementsPointFilePath = inMeteoStations;
			gfImporter.inMeasurementDataFilePath = inTemperaturesCsv;
			gfImporter.stationType = StationType.METEO;
			gfImporter.inVariableType = EnvironmentalVariableType.TEMPERATURE.getId();
			gfImporter.process();
		}

		// import the precipitation
		gfImporter.doOverWrite = false;
		if (Files.exists(Path.of(inPrecipitationCsv))) {
			gfImporter.inMeasurementsPointFilePath = null;
			gfImporter.inMeasurementDataFilePath = inPrecipitationCsv;
			gfImporter.stationType = StationType.METEO;
			gfImporter.inVariableType = EnvironmentalVariableType.PRECIPITATION.getId();
			gfImporter.process();
		}

		if (Files.exists(Path.of(inStreamGauges))) {
			gfImporter.inMeasurementDataFilePath = inStreamGaugesCsv;
			gfImporter.inMeasurementsPointFilePath = inStreamGauges;
			gfImporter.inIdField = pStreamGaugesIdField;
			gfImporter.stationType = StationType.STREAM_GAUGE;
			gfImporter.inVariableType = EnvironmentalVariableType.DISCHARGE.getId();
			gfImporter.isStreamGauge = true;
			gfImporter.process();
		}
	}

	public static void main(String[] args) throws Exception {
		String workspace = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/workspace/";
		ErmStationDataImporter ei = new ErmStationDataImporter();
		ei.inGpkg = workspace + "outputs/geoframe_data.gpkg";
		ei.pStartTimestamp = ErmCommonData.START_TIMESTAMP;
		ei.pEndTimestamp = ErmCommonData.END_TIMESTAMP;
		ei.pTimeResolution = ErmCommonData.TIME_RESOLUTION;
		ei.inMeteoStations = workspace + "stations_tot.shp";
		ei.inTemperaturesCsv = workspace + "temperature_gf_2.csv";
		ei.inPrecipitationCsv = workspace + "precipitation_gf.csv";
		ei.inStreamGauges = workspace + "idrometri.shp";
		ei.pStreamGaugesIdField = "idstazione";
		ei.inStreamGaugesCsv = workspace + "Q_vermiglio_2000-2024.csv";
		ei.process();
	}

}
