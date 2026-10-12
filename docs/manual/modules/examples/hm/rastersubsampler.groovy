import org.hortonmachine.modules.RasterSubsampler

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation belts of RangeLookup in cells of 100 m, with the most frequent belt of each
var subsampler = new RasterSubsampler()
subsampler.inRaster = folder + "elevation_belts.tif"
subsampler.pFactor = 10
subsampler.pMode = "majority"
subsampler.outRaster = folder + "belts_100m.tif"
subsampler.process()

// the mean elevation of the same cells
subsampler = new RasterSubsampler()
subsampler.inRaster = folder + "dtm_flanginec.tif"
subsampler.pFactor = 10
subsampler.pMode = "avg"
subsampler.outRaster = folder + "dtm_mean_100m.tif"
subsampler.process()
