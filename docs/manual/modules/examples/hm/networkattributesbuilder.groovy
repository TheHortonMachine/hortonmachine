import org.hortonmachine.modules.NetworkAttributesBuilder

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var attributes = new NetworkAttributesBuilder()
// the network and the drainage directions of the basin of ExtractBasin, with the outlet marked
attributes.inNet = folder + "net_basin.tif"
attributes.inFlow = folder + "drain_marked.tif"
attributes.inTca = folder + "tca.tif"
attributes.doHack = true
attributes.outNet = folder + "net_attributes.shp"
attributes.outHack = folder + "hack.tif"
attributes.process()
