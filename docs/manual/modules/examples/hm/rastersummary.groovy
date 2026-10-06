import org.hortonmachine.modules.RasterSummary

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var summary = new RasterSummary()
summary.inRaster = folder + "dtm_flanginec.tif"
summary.process()

println "min: " + summary.outMin + " max: " + summary.outMax + " mean: " + summary.outMean + " sdev: " + summary.outSdev
