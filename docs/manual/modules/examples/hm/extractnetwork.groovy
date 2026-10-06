import org.hortonmachine.modules.ExtractNetwork

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var extractNetwork = new ExtractNetwork()
extractNetwork.inTca = folder + "tca.tif"
extractNetwork.inFlow = folder + "drain.tif"
// the channels start where 100 cells drain into a cell
extractNetwork.pThres = 100
extractNetwork.outNet = folder + "net.tif"
extractNetwork.process()
