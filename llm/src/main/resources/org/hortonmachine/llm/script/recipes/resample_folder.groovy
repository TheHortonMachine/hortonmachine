// Resample all the rasters of a folder to a new cell size, into another folder
// keywords: resample, resampling, resolution, cell size, pixel size, folder, batch, rasters, interpolation, rasterresolutionresampler
import org.hortonmachine.modules.*

def inFolder = "/path/to/rasters/"
def outFolder = "/path/to/resampled/"
def extension = ".tif"
def newResolution = 2.0

(outFolder as File).mkdirs()
(inFolder as File).eachFile { file ->
    if (file.name.endsWith(extension)) {
        def name = file.name.substring(0, file.name.lastIndexOf("."))
        def resampler = new RasterResolutionResampler()
        resampler.inGeodata = file.absolutePath
        resampler.pXres = newResolution
        resampler.pYres = newResolution
        // nearest neightbour keeps the values, as needed for classes; bilinear smooths them
        resampler.pInterpolation = "bilinear"
        resampler.outGeodata = outFolder + name + "_res" + newResolution + extension
        resampler.process()
        println "Resampled " + file.name
    }
}
