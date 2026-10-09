import org.hortonmachine.lesto.modules.filter.LasHeightDistribution
import org.hortonmachine.modules.GridsGenerator
import org.hortonmachine.modules.RasterResolutionResampler

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the categories are mapped on the cells of the elevation model: 2 m cells
// contain enough points to tell the layers apart
var resampler = new RasterResolutionResampler()
resampler.inGeodata = folder + "dtm_777.tif"
resampler.pXres = 2.0
resampler.pYres = 2.0
resampler.outGeodata = folder + "dtm_777_2m.tif"
resampler.process()

// the tiles to analyze: 3 x 3 squares of 20 m over the plot, with an id field
var grid = new GridsGenerator()
grid.inRaster = folder + "dtm_777.tif"
grid.pRows = 3
grid.pCols = 3
grid.pWidth = 20.0
grid.pHeight = 20.0
grid.outMap = folder + "forest_tiles.shp"
grid.process()

var distribution = new LasHeightDistribution()
distribution.inIndexFile = folder + "uni_bz_plot777.las"
distribution.inDem = folder + "dtm_777_2m.tif"
distribution.inVector = folder + "forest_tiles.shp"
distribution.fId = "id"
distribution.outCats = folder + "forest_layers.tif"
distribution.process()
