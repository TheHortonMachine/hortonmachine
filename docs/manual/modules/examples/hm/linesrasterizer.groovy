import org.hortonmachine.modules.LinesRasterizer

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the network of NetworkAttributesBuilder, with the Strahler order of each reach, on the grid of the elevation model
var rasterizer = new LinesRasterizer()
rasterizer.inVector = folder + "net_attributes.shp"
rasterizer.fCat = "strahler"
rasterizer.inRaster = folder + "dtm_flanginec.tif"
rasterizer.outRaster = folder + "net_strahler.tif"
rasterizer.process()
