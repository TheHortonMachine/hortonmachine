import org.hortonmachine.modules.Stone

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var stone = new Stone()
stone.inElev = folder + "dtm_flanginec.tif"
// the inputs prepared by StoneInputs
stone.inSources = folder + "sources.tif"
stone.inNormalRestitution = folder + "nrest.tif"
stone.inTangentialRestitution = folder + "trest.tif"
stone.inFriction = folder + "friction.tif"
stone.outCounter = folder + "counter.tif"
stone.outMaxVelocity = folder + "velocity.tif"
stone.outMaxDz = folder + "height.tif"
stone.process()
