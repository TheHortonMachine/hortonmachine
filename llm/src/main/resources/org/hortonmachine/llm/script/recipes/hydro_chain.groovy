// Hydrological chain from a DEM: depitting, flow directions, corrected drainage directions with contributing areas, channel network
// keywords: dem, dtm, elevation, pits, pitfiller, depit, flowdirections, draindir, drainage, tca, contributing area, network, streams, channels, hydrology, chain, watershed analysis
import org.hortonmachine.* // the HM helper
import org.hortonmachine.modules.*

// the elevation model and the results, in one folder: change them to yours
def folder = "/path/to/folder/"
def dem = folder + "dem.tif"
def pit = folder + "pit.tif"
def flow = folder + "flow.tif"
def drain = folder + "drain.tif"
def tca = folder + "tca.tif"
def net = folder + "net.tif"
// the contributing area a cell needs to be a channel, in cells
def threshold = 100

// each step runs only if its result is missing, so the script can be run again after a change
if (!(pit as File).exists()) {
    def pitfiller = new Pitfiller()
    pitfiller.inElev = dem
    pitfiller.outPit = pit
    pitfiller.process()
    HM.makeQgisStyleForRaster("elev", pit)
}

if (!(flow as File).exists()) {
    def flowdirections = new FlowDirections()
    flowdirections.inPit = pit
    flowdirections.pMinElev = 0
    flowdirections.outFlow = flow
    flowdirections.process()
    HM.makeQgisStyleForRaster("flow", flow)
}

// the D8 directions corrected with the least angular deviation, and the contributing areas
if (!(drain as File).exists()) {
    def draindir = new DrainDir()
    draindir.inPit = pit
    draindir.inFlow = flow
    draindir.pLambda = 1.0
    draindir.doLad = true
    draindir.outFlow = drain
    draindir.outTca = tca
    draindir.process()
    HM.makeQgisStyleForRaster("flow", drain)
    HM.makeQgisStyleForRaster("logarithmic", tca)
}

if (!(net as File).exists()) {
    def extractnetwork = new ExtractNetwork()
    extractnetwork.inTca = tca
    extractnetwork.inFlow = drain
    extractnetwork.pThres = threshold
    extractnetwork.outNet = net
    extractnetwork.process()
    HM.makeQgisStyleForRaster("net", net)
}
