// Aggregate a raster to coarser cells by exact factors (2x2, 5x5 cells, ...), with the average or other statistics
// keywords: subsample, aggregate, coarser, downsample, factor, blocks, average, cells, rastersubsampler, resolution
import org.hortonmachine.modules.*

def inRaster = "/path/to/dem.tif"
def outPrefix = "/path/to/dem_"
// output cells of factor x factor input cells
def factors = [2, 5, 10]

factors.each { factor ->
    def subsampler = new RasterSubsampler()
    subsampler.inRaster = inRaster
    subsampler.pFactor = factor
    subsampler.pMode = "avg" // or min, max, sum, majority, nearest
    subsampler.outRaster = outPrefix + factor + "x" + factor + ".tif"
    subsampler.process()
    println "Written the ${factor}x${factor} raster"
}
