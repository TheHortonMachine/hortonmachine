import org.hortonmachine.modules.Gradient

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var gradient = new Gradient()
gradient.inElev = folder + "dtm_flanginec.tif"
gradient.pMode = "Horn"
gradient.outSlope = folder + "gradient.tif"
gradient.process()
