import org.hortonmachine.modules.Pitfiller

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var pitfiller = new Pitfiller()
pitfiller.inElev = folder + "dtm_flanginec.tif"
pitfiller.outPit = folder + "pit.tif"
pitfiller.process()
