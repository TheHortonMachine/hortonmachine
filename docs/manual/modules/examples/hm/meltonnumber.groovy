import org.hortonmachine.modules.MeltonNumber

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var melton = new MeltonNumber()
melton.inElev = folder + "dtm_flanginec.tif"
// the polygon of the basin of ExtractBasin, identified by its area
melton.inFans = folder + "basin.shp"
melton.fId = "area"
melton.process()

for (row in melton.outMelton) {
    println "Basin " + row[0] + ": Melton number " + row[1]
}
