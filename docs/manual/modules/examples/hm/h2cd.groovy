import org.hortonmachine.modules.H2cd

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var h2cd = new H2cd()
h2cd.inFlow = folder + "drain.tif"
h2cd.inNet = folder + "net.tif"
// the distance in meters, in 3D
h2cd.pMode = 1
h2cd.inElev = folder + "pit.tif"
h2cd.outH2cd = folder + "h2cd.tif"
h2cd.process()
