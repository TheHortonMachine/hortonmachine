import org.hortonmachine.modules.VectorFilter
import org.hortonmachine.modules.VectorMerger

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the lowest cells, to merge with the highest ones of the VectorFilter example
var filter = new VectorFilter()
filter.inVector = folder + "grid_elev.shp"
filter.pCql = "elev_max < 1500"
filter.outVector = folder + "grid_low.shp"
filter.process()

var merger = new VectorMerger()
merger.inVector1 = folder + "grid_high.shp"
merger.inVector2 = folder + "grid_low.shp"
merger.outVector = folder + "grid_extremes.shp"
merger.process()
