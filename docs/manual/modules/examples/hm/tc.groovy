import org.hortonmachine.modules.Tc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var tc = new Tc()
// the curvatures of Curvatures
tc.inProf = folder + "prof.tif"
tc.inTan = folder + "tang.tif"
// the curvatures under which the terrain is planar
tc.pProfthres = 0.01
tc.pTanthres = 0.01
tc.outTc9 = folder + "tc9.tif"
tc.outTc3 = folder + "tc3.tif"
tc.process()
