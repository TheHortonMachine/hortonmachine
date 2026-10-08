import org.hortonmachine.modules.Cb

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var cb = new Cb()
// the elevation model as both maps: the moments describe how the elevation
// is distributed inside each of the 20 elevation intervals
cb.inRaster1 = folder + "pit.tif"
cb.inRaster2 = folder + "pit.tif"
cb.pBins = 20
cb.pFirst = 1
cb.pLast = 3
cb.process()

// each row: the mean elevation of the interval, its number of cells, again the mean
// elevation, the variance and the third moment, here as standard deviation and skewness
var csv = new StringBuilder("elevation,cells,elevation_mean,elevation_stddev,elevation_skewness\n")
for (row in cb.outCb) {
    var stddev = Math.sqrt(row[3])
    var skewness = stddev > 0 ? row[4] / (stddev * stddev * stddev) : 0.0
    csv.append(String.format("%.1f,%d,%.1f,%.1f,%.2f%n", row[0], (long) row[1], row[2], stddev, skewness))
}
new File(folder + "elevation_moments.csv").text = csv.toString()
