import org.hortonmachine.modules.Raster2Xyz
import org.hortonmachine.modules.Xyz2Raster

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation model as a text file of triplets: easting, northing and elevation
var raster2xyz = new Raster2Xyz()
raster2xyz.inRaster = folder + "dtm_flanginec.tif"
raster2xyz.outFile = folder + "dtm_flanginec.xyz"
raster2xyz.process()

// and back to a raster, with the resolution and the CRS of the points
var xyz2raster = new Xyz2Raster()
xyz2raster.inFile = folder + "dtm_flanginec.xyz"
xyz2raster.pRes = 10.0
xyz2raster.pCode = "EPSG:3003"
xyz2raster.outRaster = folder + "dtm_from_xyz.tif"
xyz2raster.process()
