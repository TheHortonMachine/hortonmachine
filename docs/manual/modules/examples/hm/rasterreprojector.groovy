import org.hortonmachine.modules.RasterReprojector

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// from Monte Mario / Italy zone 1 to WGS 84 / UTM zone 32N
var reprojector = new RasterReprojector()
reprojector.inRaster = folder + "dtm_flanginec.tif"
reprojector.pCode = "EPSG:32632"
reprojector.outRaster = folder + "dtm_utm.tif"
reprojector.process()
