import org.hortonmachine.lesto.modules.vegetation.PointCloudMaximaFinder

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the tops of the trees as the points higher than all the others around them,
// within a radius that grows with their height over the ground
var finder = new PointCloudMaximaFinder()
finder.inLas = folder + "uni_bz_plot777.las"
finder.inDtm = folder + "dtm_777.tif"
finder.pThreshold = 5.0
finder.doDynamicRadius = true
finder.pMaxRadius = 3.0
finder.outTops = folder + "tops_points.shp"
finder.process()
