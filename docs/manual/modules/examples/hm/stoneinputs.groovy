import org.hortonmachine.modules.StoneInputs

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var stoneInputs = new StoneInputs()
stoneInputs.inElev = folder + "dtm_flanginec.tif"
// the sources are the cells steeper than 45 degrees, each throwing 10 boulders
stoneInputs.pSourceSlope = 45.0
stoneInputs.pBouldersPerSource = 10
// no lithological map: the coefficients of the unclassified class everywhere
stoneInputs.outSources = folder + "sources.tif"
stoneInputs.outNormalRestitution = folder + "nrest.tif"
stoneInputs.outTangentialRestitution = folder + "trest.tif"
stoneInputs.outFriction = folder + "friction.tif"
stoneInputs.process()
