import org.hortonmachine.modules.Geomorphon

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var geomorphon = new Geomorphon()
geomorphon.inElev = folder + "dtm_flanginec.tif"
// the landforms within 300 m
geomorphon.pRadius = 300
geomorphon.pThreshold = 1
geomorphon.outRaster = folder + "geomorphon.tif"
geomorphon.process()
