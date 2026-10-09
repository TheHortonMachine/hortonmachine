import org.hortonmachine.modules.IntensityClassifierDebrisFlowTN
import org.hortonmachine.modules.Mapcalc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the maps of a debris flow, as a model of debris flows would give them: here simply made up,
// on a fan of 500 m at the end of the main channel of the valley, opening to the south-west.
// The flow is deep and fast at the apex and stops at the edge of the fan, deposits most
// in its middle and erodes only near the apex
var maps = [
    debris_depth   : "1.5 * (1 - r / 500)",
    debris_velocity: "2 * (1 - r / 500)",
    debris_deposits: "1.6 * r / 500 * (1 - r / 500)",
    debris_erosion : "con(r < 100, 2.5 * (1 - r / 100), 0)"
]
for (name in maps.keySet()) {
    var mapcalc = new Mapcalc()
    // the elevation model only gives the grid: its values are not used
    mapcalc.inRaster1 = folder + "dtm_flanginec.tif"
    mapcalc.pFunction = """
images { ${name} = write; }
dx = x() - 1638705;
dy = y() - 5112805;
r = sqrt(dx * dx + dy * dy);
${name} = con(dx < 0 && dy < 0 && r < 500, ${maps[name]}, null);
"""
    mapcalc.outRaster = folder + name + ".tif"
    mapcalc.process()
}

// the intensity, with the default thresholds
var classifier = new IntensityClassifierDebrisFlowTN()
classifier.inWaterDepth = folder + "debris_depth.tif"
classifier.inVelocity = folder + "debris_velocity.tif"
classifier.inDepositsThickness = folder + "debris_deposits.tif"
classifier.inErosionDepth = folder + "debris_erosion.tif"
classifier.outIntensity = folder + "debris_intensity.tif"
classifier.process()
