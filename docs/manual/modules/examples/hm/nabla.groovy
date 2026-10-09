import org.hortonmachine.modules.Nabla

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var nabla = new Nabla()
nabla.inElev = folder + "pit.tif"
nabla.outNabla = folder + "nabla.tif"
nabla.process()
