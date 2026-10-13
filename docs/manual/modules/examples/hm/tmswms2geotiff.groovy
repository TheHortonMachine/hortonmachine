import org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the tiles of a web map over the basin of ExtractBasin, at zoom level 15 (cells of about 3 m)
var tms = new TmsWms2Geotiff()
// change it to the url of the service to use, with the placeholders of the tile indexes
tms.inServiceUrl = "https://tiles.example.org/ZZZ/XXX/YYY.png"
// the rows numbered from the north, as in most of the web maps
tms.pSchemaType = "Google"
tms.pSourceType = "TMS"
tms.pZoomlevel = 15
tms.inRoiPath = folder + "basin.shp"
tms.outRaster = folder + "basin_tiles.tif"
tms.process()
