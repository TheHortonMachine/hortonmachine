import org.hortonmachine.modules.RasterTransformer

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation model turned by 30 degrees counterclockwise around its center
var transformer = new RasterTransformer()
transformer.inRaster = folder + "dtm_flanginec.tif"
transformer.pAngle = 30.0
// the elevation is continuous: the values are interpolated
transformer.pInterpolation = "bilinear"
transformer.outRaster = folder + "dtm_rotated.tif"
// the outline of the rotated raster
transformer.outBounds = folder + "dtm_rotated_bounds.shp"
transformer.process()
