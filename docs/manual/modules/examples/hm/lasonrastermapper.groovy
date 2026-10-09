import org.hortonmachine.lesto.modules.raster.LasOnRasterMapper

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the surface of the highest point of each cell of the plot elevation model
var mapper = new LasOnRasterMapper()
mapper.inLas = folder + "uni_bz_plot777.las"
mapper.inDtm = folder + "dtm_777.tif"
mapper.doMax = true
mapper.outRaster = folder + "dsm_max.tif"
mapper.process()
