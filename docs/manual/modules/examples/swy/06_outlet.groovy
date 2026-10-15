import org.hortonmachine.modules.CutOut
import org.hortonmachine.modules.ExtractBasin
import org.hortonmachine.modules.Mapcalc
import org.hortonmachine.modules.RasterReader
import org.hortonmachine.modules.RasterSummary

// the folder with the gura folder of the sample data: change it to yours
var folder = "/data/"
var gura = folder + "gura/"
var monthly = gura + "monthly/"
// the maps of the basin of the outlet go in their own folder
var basin = gura + "basin/"
new File(basin).mkdirs()

// the closure of the watershed, on the network: here the cell where the Gura leaves the map, at its northern border
var outletEast = 277728.16
var outletNorth = 9941844.5
var cellArea = 15 * 15

var extractBasin = new ExtractBasin()
extractBasin.inFlow = gura + "drain.tif"
extractBasin.pEast = outletEast
extractBasin.pNorth = outletNorth
extractBasin.outBasin = basin + "basin.tif"
extractBasin.process()
var cells = extractBasin.outArea / cellArea

// a map cut to the basin, in the basin folder with the same name, and the sum of its cells, in mm summed over the cells
var basinSum = { String raster ->
    var cutOut = new CutOut()
    cutOut.inRaster = raster
    cutOut.inMask = basin + "basin.tif"
    cutOut.outRaster = basin + new File(raster).name
    cutOut.process()
    // the minimum, maximum, mean and sum of the cells with a value
    return RasterSummary.getMinMaxAvgSum(RasterReader.readRaster(cutOut.outRaster))[3]
}

// the precipitation of the year, the sum of the months
var months = 1..12
var mapcalc = new Mapcalc()
months.each { mapcalc."inRaster${it}" = monthly + "prcp_" + it + ".tif" }
mapcalc.pFunction = """
images { ${months.collect { "prcp_" + it + " = read;" }.join(" ")} p_year = write; }
p_year = ${months.collect { "prcp_" + it }.join(" + ")};
"""
mapcalc.outRaster = gura + "p_year.tif"
mapcalc.process()

var pSum = basinSum(gura + "p_year.tif")
// the quickflow of each month
var qfSums = months.collect { basinSum(monthly + "qf_" + it + ".tif") }
var qfSum = qfSums.sum()
// the actual evapotranspiration, the local recharge and the baseflow of the year
var aetSum = basinSum(gura + "aet.tif")
var lSum = basinSum(gura + "l.tif")
var bSum = basinSum(gura + "b.tif")

// from mm summed over the cells: the mean depth over the basin, the volume and the mean discharge
var secondsOfYear = 365 * 86400
var depth = { sum -> sum / cells }
var volume = { sum -> sum * 0.001 * cellArea }
var line = { label, sum, seconds ->
    String.format("%-28s %8.1f mm %10.2f million m3 %8.3f m3/s", label, depth(sum), volume(sum) / 1e6, volume(sum) / seconds)
}

println ""
println String.format("Basin of the outlet: %.0f cells, %.2f km2", cells, extractBasin.outArea / 1e6)
println ""
println "The year"
println line("precipitation P", pSum, secondsOfYear)
println line("quickflow QF", qfSum, secondsOfYear)
println line("actual evapotranspiration AET", aetSum, secondsOfYear)
println line("local recharge L", lSum, secondsOfYear)
println line("water yield QF + L = P - AET", qfSum + lSum, secondsOfYear)
println line("baseflow B", bSum, secondsOfYear)
println String.format("%-28s %8.3f mm", "check P - QF - AET - L", depth(pSum - qfSum - aetSum - lSum))
println ""
println "The quickflow of the months"
var daysOfMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
for (month in months) {
    println line("month " + month, qfSums[month - 1], daysOfMonth[month - 1] * 86400)
}
