import org.hortonmachine.lesto.modules.filter.LasHistogram

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the histogram of the intensity of the points, in 30 bins, written as a table
var histogram = new LasHistogram()
histogram.inLas = folder + "uni_bz_plot777.las"
histogram.pType = "intensity"
histogram.pBin = 30
histogram.doPlot = false
histogram.outCsv = folder + "las_intensity.csv"
histogram.process()
