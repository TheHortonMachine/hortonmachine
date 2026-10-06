import org.hortonmachine.modules.Wateroutlet

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var wateroutlet = new Wateroutlet()
wateroutlet.inFlow = folder + "drain.tif"
// the outlet, on the network
wateroutlet.pEast = 1639885.0
wateroutlet.pNorth = 5110955.0
wateroutlet.outBasin = folder + "outlet_basin.tif"
wateroutlet.process()
println "Basin area: " + wateroutlet.outArea + " m2"
