import org.hortonmachine.hmachine.geoframe.ermworkflow.ErmRadiation

// the folder containing the input data: change it to yours
var workspace = "/data/COURSE_WATER/"

var er = new ErmRadiation()
er.inDtm = workspace + "dem.tif"
er.inGpkg = workspace + "outputs/geoframe_data.gpkg"
er.pStartTimestamp = "2015-01-01 01:00"
er.pEndTimestamp = "2023-12-31 01:00"
er.pTimeResolution = "DAILY"
er.pDailySubSamples = 1
er.downscaleFactor = 4
er.doOverwrite = true
er.process()
