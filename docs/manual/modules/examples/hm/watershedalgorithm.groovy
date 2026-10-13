import org.hortonmachine.lesto.modules.vegetation.WatershedAlgorithm
import org.hortonmachine.modules.KernelDensity
import org.hortonmachine.modules.Mapcalc

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the canopy height model of Las2RasterInterpolator without the ground and the
// low vegetation, under 2 m, that would make basins of their own
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = folder + "chm_idw.tif"
mapcalc.pFunction = """
images {
    chm_idw = read;
    chm_trees = write;
}
chm_trees = chm_idw < 2 ? null : chm_idw;
"""
mapcalc.outRaster = folder + "chm_trees.tif"
mapcalc.process()

// smoothed with a gaussian kernel of 2 cells, so that the small bumps of the
// canopy do not make crowns of their own: only the order of the values counts
var kernel = new KernelDensity()
kernel.inMap = folder + "chm_trees.tif"
kernel.pKernel = 4
kernel.pRadius = 2
kernel.outDensity = folder + "chm_smooth.tif"
kernel.process()

// the crowns of the trees: the canopy flooded upside down, from its tops
var watershed = new WatershedAlgorithm()
watershed.inRaster = folder + "chm_smooth.tif"
watershed.doInvert = true
watershed.doBorders = false
watershed.outRaster = folder + "crowns.tif"
watershed.process()
