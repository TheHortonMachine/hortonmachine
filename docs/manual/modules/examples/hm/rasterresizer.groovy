import org.hortonmachine.modules.RasterResizer

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// cut the elevation to the extent of the basin
var resizer = new RasterResizer()
resizer.inRaster = folder + "dtm_flanginec.tif"
resizer.inVector = folder + "basin.shp"
resizer.outRaster = folder + "dtm_basin.tif"
resizer.process()
