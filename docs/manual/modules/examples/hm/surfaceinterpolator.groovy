import org.hortonmachine.modules.SurfaceInterpolator

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the surface of the lidar points of LasConverter, on the grid of the plot elevation model
var interpolator = new SurfaceInterpolator()
interpolator.inVector = folder + "las_points.shp"
interpolator.fCat = "elev"
interpolator.inGrid = folder + "dtm_777.tif"
interpolator.pMode = "IDW"
interpolator.pBuffer = 2.0
interpolator.outRaster = folder + "dsm_777.tif"
interpolator.process()
