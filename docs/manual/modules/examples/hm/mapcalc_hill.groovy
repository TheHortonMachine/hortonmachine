import org.hortonmachine.modules.Mapcalc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var mapcalc = new Mapcalc()
// the elevation model only gives the grid: its values are not used
mapcalc.inRaster1 = folder + "dtm_flanginec.tif"
mapcalc.pFunction = """
images { hill = write; }
// a gaussian hill 500 m high in the center of the map, on a plain at 1000 m
cx = (xmin() + xmax()) / 2;
cy = (ymin() + ymax()) / 2;
d = sqrt((x() - cx) ^ 2 + (y() - cy) ^ 2);
height = 500;
width = 600;
hill = 1000 + height * exp(-d ^ 2 / (2 * width ^ 2));
"""
mapcalc.outRaster = folder + "hill.tif"
mapcalc.process()
