// Assign a projection to the rasters of a folder that have none (prj files) and convert them to GeoTIFF
// keywords: prj, projection, crs, epsg, assign, missing, folder, convert, geotiff, asc, ascii grid, rasters, batch
import org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter

def inFolder = "/path/to/rasters/"
def outFolder = "/path/to/geotiffs/"
def extension = ".asc"
def epsg = "EPSG:32632"

// a .prj file with the projection next to each file of the folder
OmsFileIterator.addPrj(inFolder, epsg)

(outFolder as File).mkdirs()
(inFolder as File).eachFile { file ->
    if (file.name.endsWith(extension)) {
        def name = file.name.substring(0, file.name.lastIndexOf("."))
        def raster = OmsRasterReader.readRaster(file.absolutePath)
        OmsRasterWriter.writeRaster(outFolder + name + ".tif", raster)
        println "Converted " + file.name
    }
}
