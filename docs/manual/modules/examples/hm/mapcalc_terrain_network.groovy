import org.hortonmachine.modules.DrainDir
import org.hortonmachine.modules.ExtractNetwork
import org.hortonmachine.modules.FlowDirections
import org.hortonmachine.modules.Pitfiller

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the network of the synthetic terrain, with the usual chain of modules
var pitfiller = new Pitfiller()
pitfiller.inElev = folder + "terrain.tif"
pitfiller.outPit = folder + "terrain_pit.tif"
pitfiller.process()

var flowDirections = new FlowDirections()
flowDirections.inPit = folder + "terrain_pit.tif"
flowDirections.outFlow = folder + "terrain_flow.tif"
flowDirections.process()

var drainDir = new DrainDir()
drainDir.inPit = folder + "terrain_pit.tif"
drainDir.inFlow = folder + "terrain_flow.tif"
drainDir.outFlow = folder + "terrain_drain.tif"
drainDir.outTca = folder + "terrain_tca.tif"
drainDir.process()

var extractNetwork = new ExtractNetwork()
extractNetwork.inTca = folder + "terrain_tca.tif"
extractNetwork.inFlow = folder + "terrain_drain.tif"
extractNetwork.pThres = 200
extractNetwork.outNet = folder + "terrain_net.tif"
extractNetwork.process()
