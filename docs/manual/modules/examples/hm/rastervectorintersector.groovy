import org.hortonmachine.modules.RasterVectorIntersector

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation inside the high cells of the VectorFilter example
var intersector = new RasterVectorIntersector()
intersector.inRaster = folder + "dtm_flanginec.tif"
intersector.inVector = folder + "grid_high.shp"
intersector.outRaster = folder + "dtm_high_cells.tif"
intersector.process()

// and the elevation outside of them
intersector = new RasterVectorIntersector()
intersector.inRaster = folder + "dtm_flanginec.tif"
intersector.inVector = folder + "grid_high.shp"
intersector.doInverse = true
intersector.outRaster = folder + "dtm_not_high_cells.tif"
intersector.process()
