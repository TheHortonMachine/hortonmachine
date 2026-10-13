import org.hortonmachine.modules.Mapcalc
import org.hortonmachine.modules.RasterGenerator

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// a grid of 10 m cells on an area of 5 x 4 km without any data
var generator = new RasterGenerator()
generator.pWest = 1650000.0
generator.pEast = 1655000.0
generator.pSouth = 5100000.0
generator.pNorth = 5104000.0
generator.pXres = 10.0
generator.pYres = 10.0
generator.pCode = "EPSG:3003"
generator.outRaster = folder + "new_grid.tif"
generator.process()

// Mapcalc fills the grid with a synthetic terrain: 4 octaves of value noise, as in the Mapcalc page
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = folder + "new_grid.tif"
mapcalc.pFunction = """
images { synthetic_terrain = write; }
z = 800;
amp = 400;
wave = 1200;
foreach (k in 0:3) {
    u = x() / wave;
    v = y() / wave;
    i = floor(u);
    j = floor(v);
    su = (u - i) * (u - i) * (3 - 2 * (u - i));
    sv = (v - j) * (v - j) * (3 - 2 * (v - j));
    h = sin(i * 12.9898 + j * 78.233 + k) * 43758.5453;
    n00 = h - floor(h);
    h = sin((i + 1) * 12.9898 + j * 78.233 + k) * 43758.5453;
    n10 = h - floor(h);
    h = sin(i * 12.9898 + (j + 1) * 78.233 + k) * 43758.5453;
    n01 = h - floor(h);
    h = sin((i + 1) * 12.9898 + (j + 1) * 78.233 + k) * 43758.5453;
    n11 = h - floor(h);
    z += amp * (n00 + (n10 - n00) * su + (n01 - n00) * sv + (n00 - n10 - n01 + n11) * su * sv);
    amp *= 0.45;
    wave /= 2;
}
synthetic_terrain = z;
"""
mapcalc.outRaster = folder + "synthetic_terrain.tif"
mapcalc.process()
