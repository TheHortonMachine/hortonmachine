import org.hortonmachine.hmachine.geoframe.ermworkflow.ErmStationDataImporter

// the folder containing the input data: change it to yours
var workspace = "/data/COURSE_WATER/"

var ei = new ErmStationDataImporter()
ei.inGpkg = workspace + "outputs/geoframe_data.gpkg"

// time span and temporal resolution
ei.pStartTimestamp = "2006-01-01 01:00"
ei.pEndTimestamp = "2023-12-31 01:00"
ei.pTimeResolution = "DAILY"

// meteo data
ei.inMeteoStations = workspace + "meteo/meteo_stations.gpkg"
ei.pMeteoIdField = "stationid"
ei.inTemperaturesCsv = workspace + "meteo/temperatures.csv"
ei.inPrecipitationCsv = workspace + "meteo/precipitations.csv"

// discharge data
ei.inStreamGauges = workspace + "discharge/dams.shp"
ei.pStreamGaugesIdField = "stationid"
ei.inStreamGaugesCsv = workspace + "discharge/dam_data.csv"

ei.process()
