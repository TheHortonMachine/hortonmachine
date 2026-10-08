import org.hortonmachine.modules.BasinShape

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var basinShape = new BasinShape()
// the sub-basins of NetNumbering
basinShape.inBasins = folder + "subbasins.tif"
basinShape.inElev = folder + "pit.tif"
basinShape.outBasins = folder + "subbasins.shp"
basinShape.process()
