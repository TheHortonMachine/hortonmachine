import org.hortonmachine.modules.LineSmootherMcMaster

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var smoother = new LineSmootherMcMaster()
// the contour lines of ContourExtractor, with a vertex for each cell crossed
smoother.inVector = folder + "contours.shp"
// a vertex every 2 m, the average of 41 vertices: a window of about 80 m
smoother.pDensify = 2.0
smoother.pLookahead = 41
smoother.pSlide = 1.0
// and then fewer vertices again
smoother.pSimplify = 0.5
smoother.outVector = folder + "contours_mcmaster.shp"
smoother.process()
