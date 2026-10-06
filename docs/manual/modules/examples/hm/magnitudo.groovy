import org.hortonmachine.modules.CutOut
import org.hortonmachine.modules.Magnitudo

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// keep the drainage directions of the network only, so that the sources are the channel heads
var cutOut = new CutOut()
cutOut.inRaster = folder + "drain_marked.tif"
cutOut.inMask = folder + "net_basin.tif"
cutOut.outRaster = folder + "drain_net.tif"
cutOut.process()

var magnitudo = new Magnitudo()
magnitudo.inFlow = folder + "drain_net.tif"
magnitudo.outMag = folder + "magnitudo.tif"
magnitudo.process()
