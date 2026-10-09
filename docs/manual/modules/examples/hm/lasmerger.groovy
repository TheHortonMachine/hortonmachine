import org.hortonmachine.lesto.modules.filter.LasMerger

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// merge the pieces of LasSplitter back into a single file
var merger = new LasMerger()
merger.inFolder = folder + "las_pieces"
merger.outLas = folder + "las_merged.las"
merger.process()
