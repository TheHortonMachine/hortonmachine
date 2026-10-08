import org.hortonmachine.modules.Cb

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var cb = new Cb()
// how the slope changes with the elevation: the cells are split in 20 elevation
// intervals and in each the slope is described by its moments from 1 to 3
cb.inRaster1 = folder + "pit.tif"
cb.inRaster2 = folder + "slope.tif"
cb.pBins = 20
cb.pFirst = 1
cb.pLast = 3
cb.process()

// each row: the mean elevation of the interval, its number of cells, the mean slope,
// the variance and the third moment of the slope, here as standard deviation and skewness
var csv = new StringBuilder("elevation,cells,slope_mean,slope_stddev,slope_skewness\n")
for (row in cb.outCb) {
    var stddev = Math.sqrt(row[3])
    var skewness = stddev > 0 ? row[4] / (stddev * stddev * stddev) : 0.0
    csv.append(String.format("%.1f,%d,%.3f,%.3f,%.2f%n", row[0], (long) row[1], row[2], stddev, skewness))
}
new File(folder + "elevation_slope.csv").text = csv.toString()
