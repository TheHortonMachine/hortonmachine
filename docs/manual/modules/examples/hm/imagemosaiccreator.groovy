import org.hortonmachine.gears.utils.files.FileUtilities
import org.hortonmachine.modules.ImageMosaicCreator

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// a folder with the two elevation models cut by RasterResizer for Mosaic12
var tiles = folder + "dtm_tiles/"
new File(tiles).mkdirs()
FileUtilities.copyFile(folder + "dtm_basin.tif", tiles + "dtm_basin.tif")
FileUtilities.copyFile(folder + "dtm_outlet_basin.tif", tiles + "dtm_outlet_basin.tif")

// creates dtm_tiles.shp, the index of the footprints, and dtm_tiles.properties in it
var creator = new ImageMosaicCreator()
creator.inFolder = tiles
creator.process()
