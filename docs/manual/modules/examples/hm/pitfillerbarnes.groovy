import org.hortonmachine.modules.PitfillerBarnes

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var pitfiller = new PitfillerBarnes()
pitfiller.inElev = folder + "dtm_flanginec.tif"
pitfiller.outPit = folder + "pit_barnes.tif"
// also the network of the cells draining at least 100 cells
pitfiller.pThres = 100
pitfiller.outNet = folder + "net_barnes.tif"
pitfiller.process()
