import org.hortonmachine.modules.Mapcalc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var mapcalc = new Mapcalc()
// the elevation model only gives the grid: its values are not used
mapcalc.inRaster1 = folder + "dtm_flanginec.tif"
mapcalc.pFunction = """
images { terrain = write; }
// a fractal terrain: 6 octaves of value noise, each half as long and 0.45 times as high as the
// previous one; the random heights of the corners of a lattice are interpolated smoothly
seed = 1;      // change it for another terrain
z = 1000;
amp = 500;
wave = 1500;
foreach (k in 0:5) {
    // each octave is rotated, so that the lattices are not aligned
    a = k * 0.7;
    u = (x() * cos(a) - y() * sin(a)) / wave;
    v = (x() * sin(a) + y() * cos(a)) / wave;
    i = floor(u);
    j = floor(v);
    fu = u - i;
    fv = v - j;
    su = fu * fu * (3 - 2 * fu);
    sv = fv * fv * (3 - 2 * fv);
    // pseudo random heights between 0 and 1 of the four corners of the lattice cell
    s = seed + k;
    h = sin(i * 12.9898 + j * 78.233 + s) * 43758.5453;
    n00 = h - floor(h);
    h = sin((i + 1) * 12.9898 + j * 78.233 + s) * 43758.5453;
    n10 = h - floor(h);
    h = sin(i * 12.9898 + (j + 1) * 78.233 + s) * 43758.5453;
    n01 = h - floor(h);
    h = sin((i + 1) * 12.9898 + (j + 1) * 78.233 + s) * 43758.5453;
    n11 = h - floor(h);
    n = n00 + (n10 - n00) * su + (n01 - n00) * sv + (n00 - n10 - n01 + n11) * su * sv;
    z += amp * n;
    amp *= 0.45;
    wave /= 2;
}
terrain = z;
"""
mapcalc.outRaster = folder + "terrain.tif"
mapcalc.process()
