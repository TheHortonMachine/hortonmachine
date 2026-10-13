import org.hortonmachine.modules.RasterMaximaFinder

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the tops of the trees in the canopy height model of Las2RasterInterpolator,
// with a window that grows with the height as the crowns of mixed forests
var finder = new RasterMaximaFinder()
finder.inDsmDtmDiff = folder + "chm_idw.tif"
finder.pMode = "mixed_pines_and_deciduous_trees"
finder.pThreshold = 5.0
finder.outMaxima = folder + "tops_raster.shp"
finder.outCircles = folder + "tops_raster_windows.shp"
finder.process()
