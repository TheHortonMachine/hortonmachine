import org.hortonmachine.modules.IntensityClassifierFlood
import org.hortonmachine.modules.Mapcalc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the floods of 30, 100 and 200 years of return period on the river of the SaintGeo example,
// with a water surface parallel to the bottom, 5, 15 and 25 cm over the edge of the floodplain
var overbank = [30: 0.05, 100: 0.15, 200: 0.25]
for (tr in overbank.keySet()) {
    // the water depth, novalue where the cells are dry
    var depth = new Mapcalc()
    depth.inRaster1 = folder + "river.tif"
    depth.pFunction = """
images {
    river = read;
    depth = write;
}
h = 200 + 0.002 * (y() - ymin()) + ${overbank[tr]} - river;
depth = con(h > 0, h, null);
"""
    depth.outRaster = folder + "depth_tr" + tr + ".tif"
    depth.process()

    // the velocity of a uniform flow of that depth, with a Gauckler-Strickler coefficient
    // of 30 and the slope of 0.2% of the valley
    var velocity = new Mapcalc()
    velocity.inRaster1 = folder + "depth_tr" + tr + ".tif"
    velocity.pFunction = """
images {
    depth_tr${tr} = read;
    velocity = write;
}
velocity = 30 * depth_tr${tr} ^ (2.0 / 3.0) * sqrt(0.002);
"""
    velocity.outRaster = folder + "velocity_tr" + tr + ".tif"
    velocity.process()

    // the intensity, with the default thresholds: 0.5 and 1 m of depth, 0.5 and 1 m2/s of velocity by depth
    var classifier = new IntensityClassifierFlood()
    classifier.inWaterDepth = folder + "depth_tr" + tr + ".tif"
    classifier.inVelocity = folder + "velocity_tr" + tr + ".tif"
    classifier.outIntensity = folder + "intensity_tr" + tr + ".tif"
    classifier.process()
}
