import org.hortonmachine.modules.NetDiff

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var netDiff = new NetDiff()
// the drainage directions of the basin of ExtractBasin, with the outlet marked
netDiff.inFlow = folder + "drain_marked.tif"
// the links of NetNumbering
netDiff.inStream = folder + "netnum.tif"
// the drop of the elevation along each link
netDiff.inRaster = folder + "pit.tif"
netDiff.outDiff = folder + "netdiff.tif"
netDiff.process()
