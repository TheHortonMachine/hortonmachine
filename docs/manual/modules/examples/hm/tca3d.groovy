import org.hortonmachine.modules.Tca3d

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var tca3d = new Tca3d()
tca3d.inPit = folder + "pit.tif"
tca3d.inFlow = folder + "drain.tif"
tca3d.outTca = folder + "tca3d.tif"
tca3d.process()
