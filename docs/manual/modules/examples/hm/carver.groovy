import org.hortonmachine.modules.Carver

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// burn the channels of the network of NetworkAttributesBuilder 3 m into the terrain
var carver = new Carver()
carver.inRaster = folder + "dtm_flanginec.tif"
carver.inCarverLines = folder + "net_attributes.shp"
carver.pDepthLines = 3.0
carver.outRaster = folder + "dtm_carved.tif"
carver.process()
