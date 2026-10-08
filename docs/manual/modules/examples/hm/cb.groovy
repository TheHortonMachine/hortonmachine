import org.hortonmachine.modules.Cb

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var cb = new Cb()
// the histogram of the elevation in 20 bins
cb.inRaster1 = folder + "pit.tif"
cb.pBins = 20
cb.pFirst = 1
cb.pLast = 1
cb.process()

// each row: the mean elevation of the bin and its number of cells
var csv = new StringBuilder("elevation,cells\n")
for (row in cb.outCb) {
    csv.append(String.format("%.1f,%d%n", row[0], (long) row[1]))
}
new File(folder + "elevation_histogram.csv").text = csv.toString()
