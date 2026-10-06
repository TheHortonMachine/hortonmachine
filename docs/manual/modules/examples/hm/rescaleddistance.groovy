import org.hortonmachine.modules.RescaledDistance

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var rescaledDistance = new RescaledDistance()
rescaledDistance.inFlow = folder + "drain_basin.tif"
rescaledDistance.inNet = folder + "net_basin.tif"
// the water is 10 times faster in the channels than on the hillslopes
rescaledDistance.pRatio = 10
rescaledDistance.outRescaled = folder + "rescaled.tif"
rescaledDistance.process()
