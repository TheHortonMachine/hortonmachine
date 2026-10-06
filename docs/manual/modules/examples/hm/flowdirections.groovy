import org.hortonmachine.modules.FlowDirections

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var flowDirections = new FlowDirections()
flowDirections.inPit = folder + "pit.tif"
flowDirections.outFlow = folder + "flow.tif"
flowDirections.process()
