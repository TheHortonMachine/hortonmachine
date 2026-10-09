import org.hortonmachine.lesto.modules.filter.LasSplitter

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// split the survey in 2 x 2 pieces, uni_bz_plot777_1.las to uni_bz_plot777_4.las
new File(folder + "las_pieces").mkdirs()
var splitter = new LasSplitter()
splitter.inFile = folder + "uni_bz_plot777.las"
splitter.pRows = 2
splitter.pCols = 2
splitter.outFolder = folder + "las_pieces"
splitter.process()
