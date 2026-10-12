import org.hortonmachine.modules.RasterValueRounder

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation to the decimeter
var rounder = new RasterValueRounder()
rounder.inRaster = folder + "dtm_flanginec.tif"
rounder.pPattern = "0.0"
rounder.outRaster = folder + "dtm_rounded.tif"
rounder.process()
