import org.hortonmachine.lesto.modules.filter.LasThresholder
import org.hortonmachine.modules.LasConverter

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// keep only the points with an intensity of at least 150
var thresholder = new LasThresholder()
thresholder.inLas = folder + "uni_bz_plot777.las"
thresholder.pType = "intensity"
thresholder.pLower = 150.0
thresholder.outLas = folder + "las_bright.las"
thresholder.process()

// the kept points as a vector, to see them on a map
var converter = new LasConverter()
converter.inFile = folder + "las_bright.las"
converter.outFile = folder + "las_bright.shp"
converter.process()
