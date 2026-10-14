import org.hortonmachine.gears.utils.RegionMap
import org.hortonmachine.gears.utils.files.FileUtilities
import org.hortonmachine.modules.DrainDir
import org.hortonmachine.modules.ExtractNetwork
import org.hortonmachine.modules.FlowDirections
import org.hortonmachine.modules.Mapcalc
import org.hortonmachine.modules.Pitfiller
import org.hortonmachine.modules.RasterReader
import org.hortonmachine.modules.RasterWriter

// the folder with the gura folder of the sample data: change it to yours
var folder = "/data/"
var gura = folder + "gura/"
// the monthly rasters of all the steps go in one folder
var monthly = gura + "monthly/"
new File(monthly).mkdirs()

// the flowdirections and the network, with a channel where 1000 cells drain, as in InVEST
var pitfiller = new Pitfiller()
pitfiller.inElev = gura + "DEM_gura.tif"
pitfiller.outPit = gura + "pit.tif"
pitfiller.process()

var flowDirections = new FlowDirections()
flowDirections.inPit = gura + "pit.tif"
flowDirections.outFlow = gura + "flow.tif"
flowDirections.process()

var drainDir = new DrainDir()
drainDir.inPit = gura + "pit.tif"
drainDir.inFlow = gura + "flow.tif"
drainDir.outFlow = gura + "drain.tif"
drainDir.outTca = gura + "tca.tif"
drainDir.process()

var extractNetwork = new ExtractNetwork()
extractNetwork.inTca = gura + "tca.tif"
extractNetwork.inFlow = gura + "drain.tif"
extractNetwork.pThres = 1000
extractNetwork.outNet = gura + "net.tif"
extractNetwork.process()

// the curve number and the monthly crop coefficient of each land use, from the biophysical table:
// a Mapcalc script with one line per land use
var table = FileUtilities.readFileToLinesList(gura + "biophysical_table_gura_SWY.csv")
var header = table[0].split(",").toList()
var rows = table.drop(1).findAll { it.trim() }.collect { it.split(",") }
var value = { row, column -> row[header.indexOf(column)] }

var cnScript = new StringBuilder("""
images { land_use_gura = read; soil_group_gura = read; cn = write; }
lu = land_use_gura;
s = soil_group_gura;
cn = null;
""")
for (row in rows) {
    var a = value(row, "CN_A")
    var b = value(row, "CN_B")
    var c = value(row, "CN_C")
    var d = value(row, "CN_D")
    cnScript << """if (lu == ${value(row, 'lucode')}) {
    if (s == 1) { cn = $a; } else if (s == 2) { cn = $b; } else if (s == 3) { cn = $c; } else if (s == 4) { cn = $d; }
}
"""
}
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = gura + "land_use_gura.tif"
mapcalc.inRaster2 = gura + "soil_group_gura.tif"
mapcalc.pFunction = cnScript.toString()
mapcalc.outRaster = gura + "cn.tif"
mapcalc.process()

// the grid of the elevation model, on which the 1 km climate rasters are resampled
var region = RegionMap.fromGridGeometry(RasterReader.readRaster(gura + "DEM_gura.tif").getGridGeometry())
var events = FileUtilities.readFileToLinesList(gura + "rain_events_gura.csv").drop(1).findAll { it.trim() }
        .collectEntries { def split = it.split(","); [(split[0] as int): split[1]] }

for (month in 1..12) {
    for (climate in [["precipitation/precip_gura_", "prcp_"], ["et0/ET0_gura_", "et0_"]]) {
        var reader = new RasterReader()
        reader.file = gura + climate[0] + month + ".tif"
        reader.pNorth = region.getNorth()
        reader.pSouth = region.getSouth()
        reader.pWest = region.getWest()
        reader.pEast = region.getEast()
        reader.pRows = region.getRows()
        reader.pCols = region.getCols()
        reader.process()
        RasterWriter.writeRaster(monthly + climate[1] + month + ".tif", reader.outRaster)
    }

    var kcScript = new StringBuilder("""
images { land_use_gura = read; kc = write; }
lu = land_use_gura;
kc = null;
""")
    for (row in rows) {
        kcScript << "if (lu == ${value(row, 'lucode')}) { kc = ${value(row, 'Kc_' + month)}; }\n"
    }
    mapcalc = new Mapcalc()
    mapcalc.inRaster1 = gura + "land_use_gura.tif"
    mapcalc.pFunction = kcScript.toString()
    mapcalc.outRaster = monthly + "kc_" + month + ".tif"
    mapcalc.process()

    // the number of rain events of the month, the same everywhere
    mapcalc = new Mapcalc()
    mapcalc.inRaster1 = gura + "DEM_gura.tif"
    mapcalc.pFunction = """
images { DEM_gura = read; events = write; }
events = ${events[month]};
"""
    mapcalc.outRaster = monthly + "events_" + month + ".tif"
    mapcalc.process()
}
