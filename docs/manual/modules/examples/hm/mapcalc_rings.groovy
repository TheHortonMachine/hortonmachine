import org.hortonmachine.modules.Mapcalc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var mapcalc = new Mapcalc()
// the elevation model only gives the grid: its values are not used
mapcalc.inRaster1 = folder + "dtm_flanginec.tif"
mapcalc.pFunction = """
images { rings = write; }
// concentric rings around the center of the map, every 300 m
cx = (xmin() + xmax()) / 2;
cy = (ymin() + ymax()) / 2;
d = sqrt((x() - cx) ^ 2 + (y() - cy) ^ 2);
rings = sin(2 * M_PI * d / 300);
"""
mapcalc.outRaster = folder + "rings.tif"
mapcalc.process()
