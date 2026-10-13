import org.hortonmachine.lesto.modules.utilities.LasSlicer

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// slices of 1 m every 5 m of height over the ground, as rasters of 0.5 m
var slicer = new LasSlicer()
slicer.inLas = folder + "uni_bz_plot777.las"
slicer.inDtm = folder + "dtm_777.tif"
slicer.pInterval = 5.0
slicer.pThickness = 1.0
slicer.pGroundThreshold = 0.5
slicer.pMode = "raster"
slicer.pResolution = 0.5
slicer.outFolder = folder + "las_slices"
slicer.process()
