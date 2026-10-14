import org.hortonmachine.modules.VectorIntersector

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var intersector = new VectorIntersector()
// the network of NetworkAttributesBuilder
intersector.inMap1 = folder + "net_attributes.shp"
// the cells of RasterCatToFeatureAttribute, with their elevation
intersector.inMap2 = folder + "grid_elev.shp"
intersector.outMap = folder + "net_cells.shp"
intersector.process()
