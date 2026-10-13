import org.hortonmachine.modules.GeomorphonMaximaFinder

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the tops of the trees as the peaks of the geomorphons of the surface of LasOnRasterMapper
var finder = new GeomorphonMaximaFinder()
finder.inDSM = folder + "dsm_max.tif"
finder.inDTM = folder + "dtm_777.tif"
finder.pRadius = 3.0
finder.pThreshold = 1.0
finder.pElevDiffThres = 5.0
finder.outMaxima = folder + "tops_geomorphon.shp"
finder.process()
