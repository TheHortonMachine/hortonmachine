import org.hortonmachine.modules.RasterNull

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// only the middle elevation belt of RangeLookup, from 1300 to 1700 m
var rasterNull = new RasterNull()
rasterNull.inRaster = folder + "elevation_belts.tif"
rasterNull.pValue = 2.0
rasterNull.doInverse = true
rasterNull.outRaster = folder + "belt_middle.tif"
rasterNull.process()
