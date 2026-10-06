import org.hortonmachine.modules.H2cA
import org.hortonmachine.modules.Mapcalc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation of the channel into which each cell drains, in the basin of ExtractBasin:
// all the paths must end in a channel or in a marked outlet
var h2ca = new H2cA()
h2ca.inFlow = folder + "drain_marked.tif"
h2ca.inNet = folder + "net_basin.tif"
h2ca.inAttribute = folder + "pit.tif"
h2ca.outAttribute = folder + "channel_elev.tif"
h2ca.process()

// the height above the nearest drainage
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = folder + "pit.tif"
mapcalc.inRaster2 = folder + "channel_elev.tif"
// the module needs the full Jiffle script, with the maps declared in the images block
mapcalc.pFunction = """
images {
    pit = read;
    channel_elev = read;
    hand = write;
}
hand = pit - channel_elev;
"""
mapcalc.outRaster = folder + "hand.tif"
mapcalc.process()
