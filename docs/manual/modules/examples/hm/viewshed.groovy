import org.hortonmachine.modules.Viewshed

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// what can be seen from the outlet of ExtractBasin, 2 m above the ground
var viewshed = new Viewshed()
viewshed.inRaster = folder + "dtm_flanginec.tif"
viewshed.inViewPoints = folder + "outlet.shp"
viewshed.pHeight = 2.0
viewshed.outViewshed = folder + "viewshed.tif"
viewshed.process()
