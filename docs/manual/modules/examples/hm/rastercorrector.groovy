import org.hortonmachine.modules.RasterCorrector

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// two cells set by column and row, the first one north-west, then removed with the novalue
var corrector = new RasterCorrector()
corrector.inRaster = folder + "dtm_flanginec.tif"
corrector.pCorrections = "120,80,1450.0, 121,80,-9999"
corrector.outRaster = folder + "dtm_corrected.tif"
corrector.process()

// the same with the coordinates of a point read off the map, in the reference system of the raster
corrector = new RasterCorrector()
corrector.inRaster = folder + "dtm_flanginec.tif"
corrector.pCorrections = "1638345.0,5113635.0,1450.0"
corrector.doCoordinates = true
corrector.outRaster = folder + "dtm_corrected_xy.tif"
corrector.process()
