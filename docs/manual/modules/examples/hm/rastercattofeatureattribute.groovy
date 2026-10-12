import org.hortonmachine.modules.RasterCatToFeatureAttribute

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var rat2feature = new RasterCatToFeatureAttribute()
rat2feature.inRaster = folder + "dtm_flanginec.tif"
// the cells of GridsGenerator
rat2feature.inVector = folder + "grid.shp"
// the polygons get the fields elev_min, elev_max, elev_avg and elev_sum
rat2feature.fNew = "elev"
rat2feature.outVector = folder + "grid_elev.shp"
rat2feature.process()
