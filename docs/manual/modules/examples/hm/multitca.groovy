import org.hortonmachine.modules.MultiTca

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var multiTca = new MultiTca()
multiTca.inPit = folder + "pit.tif"
multiTca.inFlow = folder + "drain.tif"
// the 9 topographic classes of Tc
multiTca.inCp9 = folder + "tc9.tif"
multiTca.outMultiTca = folder + "multitca.tif"
multiTca.process()
