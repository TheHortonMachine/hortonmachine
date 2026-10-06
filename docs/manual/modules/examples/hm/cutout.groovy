import org.hortonmachine.modules.CutOut

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// keep the drainage directions and the network only inside the basin
for (name in ["drain", "net"]) {
    var cutOut = new CutOut()
    cutOut.inRaster = folder + name + ".tif"
    cutOut.inMask = folder + "basin.tif"
    cutOut.outRaster = folder + name + "_basin.tif"
    cutOut.process()
}
