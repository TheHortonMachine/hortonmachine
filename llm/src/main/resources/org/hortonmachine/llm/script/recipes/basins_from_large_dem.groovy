// Cut a large DEM on each polygon of a vector (e.g. watersheds) and run the hydrological chain on each piece
// keywords: large, huge, big, dem, polygons, watersheds, basins, cut, clip, extract, loop, each, pitfiller, flowdirections, draindir, network, hmraster
import org.hortonmachine.* // the HM helper
import org.hortonmachine.modules.*
import org.hortonmachine.gears.libs.modules.HMRaster

// the large elevation model, the polygons (path#layer for a GeoPackage) and the output folder
def bigDem = "/path/to/big_dem.tif"
def polygons = "/path/to/watersheds.gpkg#watersheds"
def idField = "id"
def outFolder = "/path/to/outputs/"
def threshold = 2000

def raster = HM.readHMRaster(bigDem)
def index = 0
for (feature in HM.readVector(polygons)) {
    index++
    // the id of the polygon names the outputs; its number if it has no such attribute
    def id = feature.getAttribute(idField) ?: index
    def polygon = feature.defaultGeometry
    println "Working on the polygon " + id

    def dem = outFolder + id + "_dem.tif"
    def pit = outFolder + id + "_pit.tif"
    def flow = outFolder + id + "_flow.tif"
    def drain = outFolder + id + "_drain.tif"
    def tca = outFolder + id + "_tca.tif"
    def net = outFolder + id + "_net.tif"

    if (!(dem as File).exists()) {
        // the cells outside the polygon become novalue
        def piece = raster.extractOnPolygon(null, polygon)
        piece.writeToFile(dem)
        piece.close()
    }
    if (!(pit as File).exists()) {
        def pitfiller = new Pitfiller()
        pitfiller.inElev = dem
        pitfiller.outPit = pit
        pitfiller.process()
    }
    if (!(flow as File).exists()) {
        def flowdirections = new FlowDirections()
        flowdirections.inPit = pit
        flowdirections.pMinElev = 0
        flowdirections.outFlow = flow
        flowdirections.process()
    }
    if (!(drain as File).exists()) {
        def draindir = new DrainDir()
        draindir.inPit = pit
        draindir.inFlow = flow
        draindir.pLambda = 1.0
        draindir.doLad = true
        draindir.outFlow = drain
        draindir.outTca = tca
        draindir.process()
    }
    if (!(net as File).exists()) {
        def extractnetwork = new ExtractNetwork()
        extractnetwork.inTca = tca
        extractnetwork.inFlow = drain
        extractnetwork.pThres = threshold
        extractnetwork.outNet = net
        extractnetwork.process()
        HM.makeQgisStyleForRaster("net", net, 0)
    }
}
raster.close()
