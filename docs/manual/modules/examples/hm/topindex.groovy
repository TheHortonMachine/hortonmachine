import org.hortonmachine.modules.TopIndex

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var topIndex = new TopIndex()
topIndex.inTca = folder + "tca.tif"
topIndex.inSlope = folder + "slope.tif"
topIndex.outTopindex = folder + "topindex.tif"
topIndex.process()
