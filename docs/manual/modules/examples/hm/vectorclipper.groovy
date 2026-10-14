import org.hortonmachine.modules.VectorClipper

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var clipper = new VectorClipper()
// the contour lines of ContourExtractor
clipper.inMap = folder + "contours.shp"
// the basin of ExtractBasin
clipper.inClipper = folder + "basin.shp"
clipper.outMap = folder + "contours_basin.shp"
clipper.process()
