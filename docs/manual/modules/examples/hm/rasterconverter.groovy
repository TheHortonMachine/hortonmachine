import org.hortonmachine.modules.RasterConverter

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation rounded to the meter, as short integers: a quarter of the size of the double raster
var converter = new RasterConverter()
converter.inRaster = folder + "dtm_flanginec.tif"
converter.pOutType = "SHORT"
converter.outRaster = folder + "dtm_short.tif"
converter.process()

// the elevation belts of RangeLookup as bytes, the smallest type for classes
converter = new RasterConverter()
converter.inRaster = folder + "elevation_belts.tif"
converter.pOutType = "BYTE"
converter.outRaster = folder + "elevation_belts_byte.tif"
converter.process()
