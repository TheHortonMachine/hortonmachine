import org.hortonmachine.modules.Skyview

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var skyview = new Skyview()
skyview.inElev = folder + "dtm_flanginec.tif"
skyview.outSky = folder + "skyview.tif"
skyview.process()
