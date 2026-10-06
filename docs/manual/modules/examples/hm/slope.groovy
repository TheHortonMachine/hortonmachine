import org.hortonmachine.modules.Slope

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var slope = new Slope()
slope.inPit = folder + "pit.tif"
slope.inFlow = folder + "drain.tif"
slope.outSlope = folder + "slope.tif"
slope.process()
