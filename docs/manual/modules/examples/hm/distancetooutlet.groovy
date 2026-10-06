import org.hortonmachine.modules.DistanceToOutlet

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var distanceToOutlet = new DistanceToOutlet()
// the drainage directions with the outlet marked by MarkOutlets
distanceToOutlet.inFlow = folder + "drain_marked.tif"
// the elevation, to measure in 3D
distanceToOutlet.inPit = folder + "pit.tif"
// the distance in meters
distanceToOutlet.pMode = 0
distanceToOutlet.outDistance = folder + "distance.tif"
distanceToOutlet.process()
