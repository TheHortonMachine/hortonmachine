import org.hortonmachine.modules.RasterResolutionResampler

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// from 10 m to 50 m cells
var resampler = new RasterResolutionResampler()
resampler.inGeodata = folder + "dtm_flanginec.tif"
resampler.pXres = 50.0
resampler.pYres = 50.0
resampler.outGeodata = folder + "dtm_50m.tif"
resampler.process()
