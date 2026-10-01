import org.hortonmachine.hmachine.geoframe.ermworkflow.ErmPrestleyEt

// the folder containing the input data: change it to yours
var workspace = "/data/COURSE_WATER/"

var ept = new ErmPrestleyEt();
ept.inGpkg = workspace + "outputs/geoframe_data.gpkg"
ept.pStartTimestamp = "2015-01-01 01:00"
ept.pEndTimestamp = "2023-12-31 01:00"
ept.pTimeResolution = "DAILY"
ept.doOverwrite = true
ept.process()
