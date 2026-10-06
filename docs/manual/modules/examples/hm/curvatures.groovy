import org.hortonmachine.modules.Curvatures

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var curvatures = new Curvatures()
curvatures.inElev = folder + "pit.tif"
curvatures.outProf = folder + "prof.tif"
curvatures.outPlan = folder + "plan.tif"
curvatures.outTang = folder + "tang.tif"
curvatures.process()
