import org.hortonmachine.modules.Gc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var gc = new Gc()
// the slope of Slope, the network of ExtractNetwork and the 9 classes of Tc
gc.inSlope = folder + "slope.tif"
gc.inNetwork = folder + "net.tif"
gc.inCp9 = folder + "tc9.tif"
// the cells with a slope of 45 degrees or more are steep
gc.pTh = 1
gc.outClasses = folder + "gc.tif"
gc.outAggregateClasses = folder + "gc_aggregated.tif"
gc.process()
