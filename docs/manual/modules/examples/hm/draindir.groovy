import org.hortonmachine.modules.DrainDir

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var drainDir = new DrainDir()
drainDir.inPit = folder + "pit.tif"
drainDir.inFlow = folder + "flow.tif"
drainDir.outFlow = folder + "drain.tif"
drainDir.outTca = folder + "tca.tif"
drainDir.process()
