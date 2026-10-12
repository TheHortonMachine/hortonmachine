import org.hortonmachine.modules.Mapcalc
import org.hortonmachine.modules.RasterMissingValuesFiller

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// holes of 60 x 60 m every 400 m in the elevation model
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = folder + "dtm_flanginec.tif"
mapcalc.pFunction = """
images { dtm_flanginec = read; dtm_holes = write; }
hole = (x() - xmin()) % 400 < 60 && (y() - ymin()) % 400 < 60;
dtm_holes = hole ? null : dtm_flanginec;
"""
mapcalc.outRaster = folder + "dtm_holes.tif"
mapcalc.process()

// filled with a thin plate spline through the rim of each hole; the mask keeps the novalue
// frame of the elevation model out, else the fill would extend the model over it
var filler = new RasterMissingValuesFiller()
filler.inRaster = folder + "dtm_holes.tif"
filler.inMask = folder + "dtm_flanginec.tif"
filler.pMaxDistance = 10
filler.doUseOnlyBorderValues = true
filler.pMode = "TPS"
filler.outRaster = folder + "dtm_filled.tif"
filler.process()
