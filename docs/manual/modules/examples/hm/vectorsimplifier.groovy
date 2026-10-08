import org.hortonmachine.modules.VectorSimplifier

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var simplifier = new VectorSimplifier()
// the contour lines of ContourExtractor
simplifier.inVector = folder + "contours.shp"
// drop the vertices closer than 25 m to the simplified lines
simplifier.pTolerance = 25.0
simplifier.outVector = folder + "contours_simple.shp"
simplifier.process()
