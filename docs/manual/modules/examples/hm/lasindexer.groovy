import org.hortonmachine.gears.io.las.index.LasIndexer

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// index the four pieces of LasSplitter in cells of 5 m, with an overview of their points
var indexer = new LasIndexer()
indexer.inFolder = folder + "las_pieces"
indexer.pCellsize = 5.0
indexer.doOverview = true
indexer.process()
