import org.hortonmachine.modules.DownSlopeConnectivity

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var connectivity = new DownSlopeConnectivity()
connectivity.inFlow = folder + "drain.tif"
connectivity.inNet = folder + "net.tif"
connectivity.inSlope = folder + "slope.tif"
// the same weight everywhere: only the flow lengths and the slopes count
connectivity.pWeight = 1.0
connectivity.outConnectivity = folder + "connectivity.tif"
connectivity.process()
