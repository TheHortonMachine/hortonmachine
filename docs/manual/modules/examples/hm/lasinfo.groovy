import org.hortonmachine.lesto.modules.utilities.LasInfo

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// print the header, the reference system and the first points of the survey
var info = new LasInfo()
info.inLas = folder + "uni_bz_plot777.las"
info.process()
