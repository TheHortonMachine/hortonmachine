import org.hortonmachine.lesto.modules.raster.Las2RasterInterpolator

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the height of the canopy: the first returns over the ground, with their height
// over the elevation model, interpolated on its grid
var interpolator = new Las2RasterInterpolator()
interpolator.inIndexFile = folder + "uni_bz_plot777.las"
interpolator.inDtm = folder + "dtm_777.tif"
interpolator.doNormalize = true
interpolator.pImpulse = 1
interpolator.outRaster = folder + "chm_idw.tif"
interpolator.process()
