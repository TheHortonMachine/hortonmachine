import org.hortonmachine.modules.Ab

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var ab = new Ab()
ab.inTca = folder + "tca.tif"
ab.inPlan = folder + "plan.tif"
ab.outAb = folder + "ab.tif"
ab.outB = folder + "b.tif"
ab.process()
