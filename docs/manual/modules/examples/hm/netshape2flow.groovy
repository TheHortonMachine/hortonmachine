import org.hortonmachine.modules.Netshape2Flow

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var netshape2Flow = new Netshape2Flow()
// the network lines of NetworkAttributesBuilder, drawn from upstream to downstream
netshape2Flow.inNet = folder + "net_attributes.shp"
// the grid of the output maps
netshape2Flow.inGrid = folder + "pit.tif"
netshape2Flow.outFlownet = folder + "flownet.tif"
netshape2Flow.outNet = folder + "netshape.tif"
netshape2Flow.outProblems = folder + "netshape_problems.shp"
netshape2Flow.process()
