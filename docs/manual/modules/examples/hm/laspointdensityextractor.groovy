import org.hortonmachine.lesto.modules.utilities.LasPointDensityExtractor

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the count of the points, by return, in squares of 5 m
var density = new LasPointDensityExtractor()
density.inFile = folder + "uni_bz_plot777.las"
density.pGridStep = 5.0
density.outFile = folder + "las_density.shp"
density.process()
