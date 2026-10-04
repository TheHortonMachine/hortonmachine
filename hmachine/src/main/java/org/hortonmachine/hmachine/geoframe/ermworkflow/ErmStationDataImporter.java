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

@Description("Second step of the ERM/GeoFrame water budget workflow: imports the measurements of the meteo stations (temperature and precipitation) and of the stream gauges (discharge) into the GeoFrame database. The data files are CSV time series in the HortonMachine format, with one column per station.")
@Author(name = "Daniele Andreis", contact = "")
@Keywords("ERM, GeoFrame, meteo, stations, discharge, import")
@Label("GeoFrame")
@Name("ermRawDataImporter")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class ErmStationDataImporter extends HMModel {

	@Description("The GeoFrame database created by the data preparation.")
	@UI(HMConstants.FILEIN_UI_HINT_GPKG)
	@In
	public String inGpkg;

	@Description("Start of the period to import. Format: yyyy-MM-dd HH:mm, in UTC.")
	@In
	public String pStartTimestamp;

	@Description("End of the period to import. Format: yyyy-MM-dd HH:mm, in UTC.")
	@In
	public String pEndTimestamp;

	@Description("Time resolution of the data: HOURLY or DAILY.")
	@UI("combo:HOURLY,DAILY")
	@In
	public String pTimeResolution = "HOURLY";

	@Description("The meteo stations (point vector layer).")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inMeteoStations;

	@Description("Field of the meteo stations layer holding the station id, matching the ids of the CSV columns.")
	@In
	public String pMeteoIdField = "ID";

	@Description("CSV time series of the air temperatures of the meteo stations [°C].")
	@UI(HMConstants.FILEIN_UI_HINT_CSV)
	@In
	public String inTemperaturesCsv;

	@Description("CSV time series of the precipitations of the meteo stations [mm].")
	@UI(HMConstants.FILEIN_UI_HINT_CSV)
	@In
	public String inPrecipitationCsv;

	@Description("The stream gauges (point vector layer).")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inStreamGauges;

	@Description("Field of the stream gauges layer holding the station id, matching the ids of the CSV columns.")
	@In
	public String pStreamGaugesIdField = "ID";

	@Description("CSV time series of the discharges of the stream gauges [m³/s].")
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
		gfImporter.timeResolution = TimeResolution.valueOf(pTimeResolution);
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
