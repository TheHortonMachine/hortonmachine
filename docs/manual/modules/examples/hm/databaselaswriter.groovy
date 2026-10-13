import org.hortonmachine.gears.io.las.databases.DatabaseLasWriter

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the four pieces of LasSplitter in a spatialite database, in cells of 1 m
// with two levels of summaries over them
var writer = new DatabaseLasWriter()
writer.inFolder = folder + "las_pieces"
writer.inDatabasePath = folder + "las_pieces.sqlite"
writer.pDbType = "SPATIALITE"
writer.pCellsize = 1.0
writer.pLevels = 2
writer.pFactor = 5
writer.process()
