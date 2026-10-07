import org.hortonmachine.hmachine.geoframe.ermworkflow.ErmCopernicusImporter;

// the folder containing the input data: change it to yours
var workspace = "/data/COURSE_WATER/"

var ci = new ErmCopernicusImporter()
ci.inDtm = workspace + "dem.tif"
ci.inGpkg = workspace + "outputs/geoframe_data.gpkg"
// the time of the day has to be the one of the station data
ci.pStartTimestamp = "2015-01-01 01:00"
ci.pEndTimestamp = "2023-12-31 01:00"
ci.doDeleteExistingData = true
ci.process()
