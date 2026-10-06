import org.hortonmachine.modules.Tca

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the contributing areas of the D8 directions, not corrected by DrainDir
var tca = new Tca()
tca.inFlow = folder + "flow.tif"
tca.outTca = folder + "tca_d8.tif"
tca.process()
