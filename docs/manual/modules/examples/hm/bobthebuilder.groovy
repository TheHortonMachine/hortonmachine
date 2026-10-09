import org.hortonmachine.modules.BobTheBuilder

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// a dam across a valley, drawn as a polygon wider than the valley,
// with points along its crest at 1310 m
var bob = new BobTheBuilder()
bob.inRaster = folder + "dtm_flanginec.tif"
bob.inArea = folder + "dam.geojson"
bob.inElevations = folder + "dam_crest.geojson"
bob.fElevation = "elev"
bob.outRaster = folder + "dtm_dam.tif"
bob.process()
