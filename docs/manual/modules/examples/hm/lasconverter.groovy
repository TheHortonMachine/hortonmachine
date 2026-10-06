import org.hortonmachine.modules.LasConverter

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the points of a lidar survey of a forest plot
var converter = new LasConverter()
converter.inFile = folder + "uni_bz_plot777.las"
converter.outFile = folder + "las_points.shp"
converter.process()
