import org.hortonmachine.modules.LineSmootherJaitools

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var smoother = new LineSmootherJaitools()
// the contour lines simplified by VectorSimplifier: on the original ones,
// the curves would round the steps of the cells
smoother.inVector = folder + "contours_simple.shp"
// the roundest curves
smoother.pAlpha = 0.0
smoother.outVector = folder + "contours_bezier.shp"
smoother.process()
