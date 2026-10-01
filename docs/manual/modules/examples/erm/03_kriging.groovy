import org.hortonmachine.hmachine.geoframe.ermworkflow.ErmKriging;

// the folder containing the input data: change it to yours
var workspace = "/data/COURSE_WATER/"

var ek = new ErmKriging()
ek.inGpkg = workspace + "outputs/geoframe_data.gpkg"
ek.pStartTimestamp = "2015-01-01 01:00"
ek.pEndTimestamp = "2023-12-31 01:00"
ek.doDeleteExistingData = true
ek.process()
