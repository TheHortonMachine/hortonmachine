import org.hortonmachine.modules.LeastCostFlowDirections

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the drainage directions of the elevation, not depitted
var leastCost = new LeastCostFlowDirections()
leastCost.inElev = folder + "dtm_flanginec.tif"
leastCost.doSlope = false
leastCost.doAspect = false
leastCost.outFlow = folder + "flow_leastcost.tif"
leastCost.outTca = folder + "tca_leastcost.tif"
leastCost.process()
