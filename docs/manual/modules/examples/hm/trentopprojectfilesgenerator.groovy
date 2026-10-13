import org.hortonmachine.modules.TrentoPProjectFilesGenerator

// the folder where to create the database: change it to yours
var folder = "/data/sewer/"

// a new GeoPackage for the design of a network, with its empty layers, the parameters
// with their defaults and an example series of diameters
var generator = new TrentoPProjectFilesGenerator()
generator.outDatabase = folder + "sewer_new.gpkg"
generator.pMode = "Project"
generator.pCode = "EPSG:32632"
generator.process()

// the same, run in Calibration mode on the same GeoPackage, adds the layer and the
// parameters of the verification
generator.pMode = "Calibration"
generator.process()
