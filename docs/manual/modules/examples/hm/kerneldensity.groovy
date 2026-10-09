import org.hortonmachine.modules.KernelDensity
import org.hortonmachine.modules.Mapcalc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the channels of ExtractNetwork as presences: 1 on the network, 0 elsewhere
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = folder + "net.tif"
mapcalc.pFunction = """
images {
    net = read;
    channels = write;
}
channels = con(isnull(net), 0, 1);
"""
mapcalc.outRaster = folder + "channels.tif"
mapcalc.process()

// the density of the channels within 10 cells, 100 m
var density = new KernelDensity()
density.inMap = folder + "channels.tif"
density.pRadius = 10
density.outDensity = folder + "channels_density.tif"
density.process()
