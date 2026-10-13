import org.hortonmachine.modules.RasterDiff

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the canopy height model of the lidar plot: the surface of SurfaceInterpolator minus the terrain
var diff = new RasterDiff()
diff.inRaster1 = folder + "dsm_777.tif"
diff.inRaster2 = folder + "dtm_777.tif"
// the canopy can't be below the ground
diff.doNegatives = false
// the vegetation under 2 m is left out
diff.pThreshold = 2.0
diff.outRaster = folder + "chm_777.tif"
diff.process()
