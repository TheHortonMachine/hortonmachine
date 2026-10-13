import org.hortonmachine.lesto.modules.utilities.LasMover

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// move the survey 100 m to the east and 0.35 m down, as to correct a datum shift
var mover = new LasMover()
mover.inFile = folder + "uni_bz_plot777.las"
mover.pDeltaX = 100.0
mover.pDeltaZ = -0.35
mover.outFile = folder + "las_moved.las"
mover.process()
