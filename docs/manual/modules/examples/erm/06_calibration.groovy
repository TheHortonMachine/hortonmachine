import org.hortonmachine.hmachine.geoframe.ermworkflow.ErmCalibration;

// the folder containing the input data: change it to yours
var workspace = "/data/COURSE_WATER/"

var cal = new ErmCalibration();
cal.inGeopackagePath = workspace + "outputs/geoframe_data.gpkg"
cal.inFromTimestamp = "2018-01-01 01:00:00"
cal.inToTimestamp = "2020-12-31 01:00:00"
cal.pTimeStepMinutes = 60*24 // daily

// days to get get the model up and running 
cal.pSpinUpDays = 365

// Particle swarming calibration parameters
cal.pPsoIterations = 300
cal.pParticlesNum = 50

// write 
cal.doWriteState = false
cal.printDebugInfo = false

cal.process()
