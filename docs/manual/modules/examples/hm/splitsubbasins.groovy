import org.hortonmachine.modules.SplitSubbasins

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var split = new SplitSubbasins()
split.inFlow = folder + "drain_marked.tif"
// the Hack orders of NetworkAttributesBuilder
split.inHack = folder + "hack.tif"
split.pHackorder = 2
split.outNetnum = folder + "split_net.tif"
split.outSubbasins = folder + "split_subbasins.tif"
split.process()
