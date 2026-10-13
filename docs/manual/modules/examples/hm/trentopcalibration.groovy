import org.hortonmachine.modules.TrentoPCalibration

// the folder with the sample data: change it to yours
var folder = "/data/sewer/"

// the GeoPackage holds the layers network_calibration, with the diameters of the
// existing pipes, areas and junctions, and the table parameters: the rainfall curve,
// the time step, the duration of the simulation and the maximum rain duration are
// all taken from the parameters
var calibration = new TrentoPCalibration()
calibration.inDatabase = folder + "sewer_calibration.gpkg"
calibration.pOutPipe = 31
calibration.outDischarge = folder + "sewer_discharge.csv"
calibration.outFillDegree = folder + "sewer_filldegree.csv"
calibration.process()

// the verified network is written into the same GeoPackage, as network_calibration_output
println "Critical rain duration: " + calibration.outTpMax + " minutes"
