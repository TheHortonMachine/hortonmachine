import org.hortonmachine.modules.Mapcalc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var mapcalc = new Mapcalc()
// the elevation model only gives the grid: its values are not used
mapcalc.inRaster1 = folder + "dtm_flanginec.tif"
mapcalc.pFunction = """
images { valley = write; }
// a meandering V shaped valley, with sides of 30% and a bottom sloping by 5% towards the south:
// the bottom swings 250 m from the center line, with a wave length of 1500 m
cx = (xmin() + xmax()) / 2 + 250 * sin(2 * M_PI * (y() - ymin()) / 1500);
valley = 1000 + 0.3 * abs(x() - cx) + 0.05 * (y() - ymin());
"""
mapcalc.outRaster = folder + "valley.tif"
mapcalc.process()
