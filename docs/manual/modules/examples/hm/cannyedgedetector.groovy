import org.hortonmachine.modules.CannyEdgeDetector

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the breaks of slope: the edges of the gradient map of Gradient. The rock faces have much
// steeper changes than the rest of the area, so the thresholds are low, to find also the
// breaks of the open slopes, and a stronger smoothing leaves out the small irregularities
var canny = new CannyEdgeDetector()
canny.inMap = folder + "gradient.tif"
canny.pRadiusgauss = 3.0
canny.pLowthres = 0.05
canny.pHighthres = 0.12
canny.outMap = folder + "slope_breaks.tif"
canny.process()
