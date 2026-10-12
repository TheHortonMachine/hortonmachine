import org.hortonmachine.modules.VectorReprojector

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var reprojector = new VectorReprojector()
// the sub-basins of BasinShape, in Monte Mario / Italy zone 1 (EPSG:3003)
reprojector.inVector = folder + "subbasins.shp"
// to UTM 32N on WGS84
reprojector.pCode = "EPSG:32632"
reprojector.outVector = folder + "subbasins_utm.shp"
reprojector.process()
