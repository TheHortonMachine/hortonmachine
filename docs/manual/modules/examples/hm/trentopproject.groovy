import org.hortonmachine.modules.TrentoPProject

// the folder with the sample data: change it to yours
var folder = "/data/sewer/"

// the GeoPackage holds the layers network_project, areas and junctions, and the
// tables parameters and diameters
var project = new TrentoPProject()
project.inDatabase = folder + "sewer_project.gpkg"
project.pOutPipe = 31
project.process()

// the designed network is written into the same GeoPackage, as network_project_output
