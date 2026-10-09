import org.hortonmachine.modules.RasterExtruder

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the polygon of the dam of BobTheBuilder, raised by its field height, 20 m,
// over the terrain: a wall that follows the ground, not a flat crest
var extruder = new RasterExtruder()
extruder.inRaster = folder + "dtm_flanginec.tif"
extruder.inVector = folder + "dam.geojson"
extruder.pAddingField = "height"
extruder.doAdd = true
extruder.outRaster = folder + "dtm_wall.tif"
extruder.process()
