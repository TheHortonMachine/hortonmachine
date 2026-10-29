// Mosaic all the rasters (tiles) of a folder into a single raster
// keywords: mosaic, merge, tiles, join, combine, folder, rasters, patch, single raster
import org.hortonmachine.gears.modules.r.mosaic.OmsMosaic
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter

def folder = "/path/to/tiles/"
def extension = ".tif"
def outPath = "/path/to/mosaic.tif"

def files = []
(folder as File).eachFile { file ->
    if (file.name.endsWith(extension)) {
        files << file
    }
}
println "Mosaicking ${files.size()} rasters"

def mosaic = new OmsMosaic()
mosaic.inFiles = files
mosaic.process()
OmsRasterWriter.writeRaster(outPath, mosaic.outRaster)
