import org.hortonmachine.modules.CannyEdgeDetector
import org.hortonmachine.modules.Gradient
import org.hortonmachine.modules.Mapcalc
import org.hortonmachine.modules.RasterGenerator

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// an empty grid of 10 m cells, 1.5 x 1.2 km
var generator = new RasterGenerator()
generator.pWest = 1660000.0
generator.pEast = 1661500.0
generator.pSouth = 5100000.0
generator.pNorth = 5101200.0
generator.pXres = 10.0
generator.pYres = 10.0
generator.pCode = "EPSG:3003"
generator.outRaster = folder + "terraces_grid.tif"
generator.process()

// a slope rising towards north with four terraces: gentle treads and steep risers of 40, 40, 15
// and 6 m, wavy in plan, with a few centimeters of noise
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = folder + "terraces_grid.tif"
mapcalc.pFunction = """
images { terraces = write; }
xx = x() - xmin();
yy = y() - ymin();
// the risers follow a wavy line
yw = yy - 30 * sin(xx / 180);
// the treads rise 5 m every 100 m
z = 500 + 0.05 * yy;
start = 200;
foreach (height in [40, 40, 15, 6]) {
    // each riser is 40 m wide, one every 250 m
    t = min(max((yw - start) / 40, 0), 1);
    z += height * t * t * (3 - 2 * t);
    start += 250;
}
h = sin(xx * 12.9898 + yy * 78.233) * 43758.5453;
terraces = z + 0.05 * (h - floor(h));
"""
mapcalc.outRaster = folder + "terraces.tif"
mapcalc.process()

// the gradient: low on the treads, high on the risers
var gradient = new Gradient()
gradient.inElev = folder + "terraces.tif"
gradient.pMode = "Horn"
gradient.outSlope = folder + "terraces_gradient.tif"
gradient.process()

// the breaks of slope with the default thresholds
var canny = new CannyEdgeDetector()
canny.inMap = folder + "terraces_gradient.tif"
canny.outMap = folder + "terraces_breaks.tif"
canny.process()

// and with lower thresholds, that find also the weaker breaks
canny = new CannyEdgeDetector()
canny.inMap = folder + "terraces_gradient.tif"
canny.pLowthres = 0.03
canny.pHighthres = 0.06
canny.outMap = folder + "terraces_breaks_low.tif"
canny.process()
