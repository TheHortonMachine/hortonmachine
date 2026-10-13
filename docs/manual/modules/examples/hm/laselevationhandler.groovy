import org.hortonmachine.lesto.modules.utilities.LasElevationHandler
import org.hortonmachine.modules.LasConverter

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the heights of the points over the ground: the elevation model subtracted from them
var handler = new LasElevationHandler()
handler.inFile = folder + "uni_bz_plot777.las"
handler.inDtm = folder + "dtm_777.tif"
handler.outFile = folder + "las_heights.las"
handler.process()

// the normalized points as a vector, to see them on a map
var converter = new LasConverter()
converter.inFile = folder + "las_heights.las"
converter.outFile = folder + "las_heights.shp"
converter.process()
