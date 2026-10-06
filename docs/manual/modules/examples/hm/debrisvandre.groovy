import org.hortonmachine.modules.DebrisVandre
import org.hortonmachine.modules.Gradient
import org.hortonmachine.modules.Markoutlets

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the slope in degrees
var gradient = new Gradient()
gradient.inElev = folder + "dtm_flanginec.tif"
gradient.doDegrees = true
gradient.outSlope = folder + "gradient_deg.tif"
gradient.process()

// the paths end at the outlets, which must be marked with 10
var markoutlets = new Markoutlets()
markoutlets.inFlow = folder + "drain.tif"
markoutlets.outFlow = folder + "drain_outlets.tif"
markoutlets.process()

var vandre = new DebrisVandre()
vandre.inElev = folder + "dtm_flanginec.tif"
vandre.inFlow = folder + "drain_outlets.tif"
vandre.inSlope = folder + "gradient_deg.tif"
// the trigger points of DebrisTriggerCnr
vandre.inTriggers = folder + "triggers.tif"
vandre.outPaths = folder + "debris_paths.shp"
vandre.outIndexedTriggers = folder + "triggers_indexed.shp"
vandre.process()
