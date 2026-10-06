import org.hortonmachine.modules.Aspect

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var aspect = new Aspect()
aspect.inElev = folder + "dtm_flanginec.tif"
aspect.outAspect = folder + "aspect.tif"
aspect.process()
