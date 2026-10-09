import org.hortonmachine.lesto.modules.raster.AdaptiveTinFilter

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the ground of the survey, on the grid of the plot elevation model, starting from the
// lowest point of each square of 10 m
var filter = new AdaptiveTinFilter()
filter.inLas = folder + "uni_bz_plot777.las"
filter.inTemplate = folder + "dtm_777.tif"
filter.pSecRes = 10.0
filter.doTin = true
filter.outTin = folder + "ground_tin.shp"
filter.outNonGround = folder + "nonground_points.shp"
filter.outDem = folder + "dtm_tin.tif"
filter.process()
