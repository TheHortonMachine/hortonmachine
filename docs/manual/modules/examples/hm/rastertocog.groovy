import org.hortonmachine.modules.RasterToCog

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation model as a Cloud Optimized GeoTIFF, with the default settings:
// 512x512 tiles, deflate compression and averaged overviews
var cog = new RasterToCog()
cog.inRaster = folder + "dtm_flanginec.tif"
cog.outCog = folder + "dtm_flanginec_cog.tif"
cog.process()
