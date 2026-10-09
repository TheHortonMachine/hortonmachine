import org.hortonmachine.modules.ScanLineRasterizer

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the subbasins of BasinShape, each with its mean elevation, on the grid of the elevation model
var rasterizer = new ScanLineRasterizer()
rasterizer.inVector = folder + "subbasins.shp"
rasterizer.inRaster = folder + "dtm_flanginec.tif"
rasterizer.fCat = "avgelev"
rasterizer.outRaster = folder + "subbasins_avgelev.tif"
rasterizer.process()
