import org.hortonmachine.lesto.modules.utilities.LasOverviewCreator

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the extents of the four pieces of LasSplitter, with their names and points
var overview = new LasOverviewCreator()
overview.inLas = folder + "las_pieces"
overview.outOverview = folder + "las_pieces_overview.shp"
overview.process()
