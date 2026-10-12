import org.hortonmachine.modules.RasterCatToFeatureAttribute
import org.hortonmachine.modules.VectorTableJoiner

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// a table with the mean slope of each sub-basin, by netnum: the dbf of the
// sub-basins of BasinShape with the statistics of the slope of Slope
var rat2feature = new RasterCatToFeatureAttribute()
rat2feature.inRaster = folder + "slope.tif"
rat2feature.inVector = folder + "subbasins.shp"
rat2feature.fNew = "slope"
rat2feature.outVector = folder + "subbasins_slope.shp"
rat2feature.process()

var joiner = new VectorTableJoiner()
// the centroids of the sub-basins, of the DelaunayTriangulation example
joiner.inVector = folder + "subbasins_centroids.shp"
joiner.inTable = folder + "subbasins_slope.dbf"
joiner.fCommon = "netnum"
joiner.pFields = "slope_avg"
joiner.outVector = folder + "subbasins_centroids_slope.shp"
joiner.process()
