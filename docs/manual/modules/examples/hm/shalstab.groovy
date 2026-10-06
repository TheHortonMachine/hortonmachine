import org.hortonmachine.modules.Shalstab

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var shalstab = new Shalstab()
shalstab.inSlope = folder + "slope.tif"
// the contributing area per unit contour length
shalstab.inTca = folder + "ab.tif"
// the soil, the same everywhere
shalstab.pTrasmissivity = 50.0
shalstab.pTgphi = 0.7
shalstab.pCohesion = 0.0
shalstab.pSdepth = 1.0
shalstab.pRho = 1.6
// the effective precipitation
shalstab.pQ = 100.0
shalstab.outQcrit = folder + "qcrit.tif"
shalstab.outShalstab = folder + "shalstab.tif"
shalstab.process()
