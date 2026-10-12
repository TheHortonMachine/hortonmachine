import org.hortonmachine.modules.VectorFilter

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var filter = new VectorFilter()
// the cells with the elevation of RasterCatToFeatureAttribute
filter.inVector = folder + "grid_elev.shp"
// the cells above 1900 m, also in the lower part
filter.pCql = "elev_avg > 1900 AND elev_min > 1700"
filter.outVector = folder + "grid_high.shp"
filter.process()
