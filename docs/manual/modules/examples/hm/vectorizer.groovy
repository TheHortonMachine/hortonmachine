import org.hortonmachine.modules.Vectorizer

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the polygon of the basin of Wateroutlet
var vectorizer = new Vectorizer()
vectorizer.inRaster = folder + "outlet_basin.tif"
vectorizer.outVector = folder + "outlet_basin.shp"
vectorizer.process()
