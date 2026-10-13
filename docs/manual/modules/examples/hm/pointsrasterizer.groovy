import org.hortonmachine.modules.PointsRasterizer

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the centroids of the subbasins, with their mean elevation, on the 100 m grid of RasterSubsampler
var rasterizer = new PointsRasterizer()
rasterizer.inVector = folder + "subbasins_centroids.shp"
rasterizer.fCat = "avgelev"
rasterizer.inRaster = folder + "belts_100m.tif"
rasterizer.outRaster = folder + "centroids_avgelev.tif"
rasterizer.process()
