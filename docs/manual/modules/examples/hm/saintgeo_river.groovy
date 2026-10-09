import org.hortonmachine.modules.DrainDir
import org.hortonmachine.modules.ExtractNetwork
import org.hortonmachine.modules.FlowDirections
import org.hortonmachine.modules.Mapcalc
import org.hortonmachine.modules.Markoutlets
import org.hortonmachine.modules.NetworkAttributesBuilder
import org.hortonmachine.modules.Pitfiller

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// a lowland river, as SaintGeo is meant for: a meandering channel 3 m deep and 30 m wide,
// sloping by 0.2% towards the south, in a floodplain 500 m wide that falls towards it by 0.1%,
// between valley sides of 5%
var mapcalc = new Mapcalc()
// the elevation model only gives the grid: its values are not used
mapcalc.inRaster1 = folder + "dtm_flanginec.tif"
mapcalc.pFunction = """
images { river = write; }
cx = (xmin() + xmax()) / 2 + 150 * sin(2 * M_PI * (y() - ymin()) / 3000);
d = abs(x() - cx);
shape = d < 15 ? -3 : (d < 25 ? -3 + 0.3 * (d - 15) : (d < 250 ? 0.001 * (d - 25) : 0.225 + 0.05 * (d - 250)));
river = 200 + 0.002 * (y() - ymin()) + shape;
"""
mapcalc.outRaster = folder + "river.tif"
mapcalc.process()

// the network of the river, as for any elevation model
var pitfiller = new Pitfiller()
pitfiller.inElev = folder + "river.tif"
pitfiller.outPit = folder + "river_pit.tif"
pitfiller.process()

var flowDirections = new FlowDirections()
flowDirections.inPit = folder + "river_pit.tif"
flowDirections.outFlow = folder + "river_flow.tif"
flowDirections.process()

var drainDir = new DrainDir()
drainDir.inPit = folder + "river_pit.tif"
drainDir.inFlow = folder + "river_flow.tif"
drainDir.outFlow = folder + "river_drain.tif"
drainDir.outTca = folder + "river_tca.tif"
drainDir.process()

var markoutlets = new Markoutlets()
markoutlets.inFlow = folder + "river_drain.tif"
markoutlets.outFlow = folder + "river_drain_marked.tif"
markoutlets.process()

var extractNetwork = new ExtractNetwork()
extractNetwork.inTca = folder + "river_tca.tif"
extractNetwork.inFlow = folder + "river_drain_marked.tif"
extractNetwork.pThres = 5000
extractNetwork.outNet = folder + "river_net.tif"
extractNetwork.process()

var attributes = new NetworkAttributesBuilder()
attributes.inNet = folder + "river_net.tif"
attributes.inFlow = folder + "river_drain_marked.tif"
attributes.inTca = folder + "river_tca.tif"
attributes.doHack = true
attributes.outNet = folder + "river_net.shp"
attributes.process()
