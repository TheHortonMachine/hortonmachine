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
// a sample of the trajectories, as 3D lines
stone.pMaxTrajectories = 300
stone.outTrajectories = folder + "trajectories.shp"
stone.process()
