import org.hortonmachine.modules.RasterCompare

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation model against its rounding to the decimeter by RasterValueRounder
var compare = new RasterCompare()
compare.inRaster1 = folder + "dtm_flanginec.tif"
compare.inRaster2 = folder + "dtm_rounded.tif"
compare.pMaxReported = 3
compare.process()

println "different cells: " + compare.outDifferent + ", max difference: " + compare.outMaxDifference
