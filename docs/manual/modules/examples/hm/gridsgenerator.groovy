import org.hortonmachine.modules.GridsGenerator

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var grid = new GridsGenerator()
// the grid starts from the lower left corner of the elevation model
grid.inRaster = folder + "dtm_flanginec.tif"
// 7 x 7 cells of 500 m
grid.pWidth = 500.0
grid.pHeight = 500.0
grid.pCols = 7
grid.pRows = 7
grid.pType = "polygon"
grid.outMap = folder + "grid.shp"
grid.process()
