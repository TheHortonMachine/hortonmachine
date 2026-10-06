import org.hortonmachine.modules.ExtractBasin

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var extractBasin = new ExtractBasin()
extractBasin.inFlow = folder + "drain.tif"
// the outlet, on the network
extractBasin.pEast = 1638705.0
extractBasin.pNorth = 5112805.0
extractBasin.outBasin = folder + "basin.tif"
extractBasin.outVectorBasin = folder + "basin.shp"
extractBasin.outOutlet = folder + "outlet.shp"
extractBasin.process()
