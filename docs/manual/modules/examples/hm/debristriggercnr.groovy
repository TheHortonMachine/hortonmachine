import org.hortonmachine.modules.DebrisTriggerCnr

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var triggers = new DebrisTriggerCnr()
triggers.inElev = folder + "dtm_flanginec.tif"
triggers.inNet = folder + "net.tif"
triggers.inTca = folder + "tca.tif"
triggers.outTriggers = folder + "triggers.tif"
triggers.process()
