import org.hortonmachine.modules.Hillshade

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var hillshade = new Hillshade()
hillshade.inElev = folder + "dtm_flanginec.tif"
// a low sun from the west, for long shadows
hillshade.pAzimuth = 270
hillshade.pElev = 25
hillshade.outHill = folder + "shade.tif"
hillshade.process()
