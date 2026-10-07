package org.hortonmachine.mapcalc;

import java.util.LinkedHashMap;

/**
 * The example scripts of the manual, in the form used by the map calculator: without the images
 * block, which the application writes, and with the output called result.
 *
 * <p>
 * The scripts are those of docs/manual/apps/mapcalc.md and of the synthetic maps of
 * docs/manual/modules/raster-processing/mapcalc.md: keep them in sync.
 */
public class MapcalcExamples {

    private MapcalcExamples() {
    }

    /**
     * @return the examples, by name.
     */
    public static LinkedHashMap<String, String> getExamples() {
        LinkedHashMap<String, String> examples = new LinkedHashMap<>();
        examples.put("Depth of the filled depressions", """
                // the depth of the depressions filled by the Pitfiller module:
                // replace pit and dtm with the names of your maps
                result = pit - dtm;
                """);
        examples.put("Landform classification", """
                // Landform classification of a DTM, combining the
                // Topographic Position Index (TPI) with the slope.
                // Result classes:
                //   1 valley, 2 lower slope, 3 flat, 4 mid slope, 5 upper slope, 6 ridge
                // To use it on another DTM, replace dtm with the name of that map.

                r = 5;           // radius of the TPI window, in cells (11x11 cells)
                tpiThres = 2.0;  // TPI threshold, in meters
                flatSlope = 8;   // maximum slope of flat areas, in degrees

                // skip the border cells, where the window would fall outside of the map
                inside = x() > xmin() + (r + 1) * xres() && x() < xmax() - (r + 1) * xres()
                      && y() > ymin() + (r + 1) * yres() && y() < ymax() - (r + 1) * yres();

                if (!inside || isnull(dtm)) {
                    result = null;
                } else {
                    // TPI: elevation of the cell minus the mean elevation of the window
                    sum = 0;
                    n = 0;
                    foreach (dy in -r:r) {
                        foreach (dx in -r:r) {
                            v = dtm[dx * xres(), dy * yres()];
                            if (!isnull(v)) {
                                sum += v;
                                n++;
                            }
                        }
                    }
                    tpi = dtm - sum / n;

                    // slope with the Horn method on the 3x3 neighbourhood
                    // (neighbour offsets are in map units: one cell is xres() by yres())
                    ex = xres();
                    ey = yres();
                    gx = ((dtm[ex, -ey] + 2 * dtm[ex, 0] + dtm[ex, ey])
                        - (dtm[-ex, -ey] + 2 * dtm[-ex, 0] + dtm[-ex, ey])) / (8 * xres());
                    gy = ((dtm[-ex, ey] + 2 * dtm[0, ey] + dtm[ex, ey])
                        - (dtm[-ex, -ey] + 2 * dtm[0, -ey] + dtm[ex, -ey])) / (8 * yres());
                    slope = radToDeg(atan(sqrt(gx ^ 2 + gy ^ 2)));

                    if (isnull(slope)) {
                        result = null;
                    } else if (tpi <= -tpiThres) {
                        result = 1;
                    } else if (tpi <= -tpiThres / 2) {
                        result = 2;
                    } else if (tpi < tpiThres / 2) {
                        result = con(slope <= flatSlope, 3, 4);
                    } else if (tpi < tpiThres) {
                        result = 5;
                    } else {
                        result = 6;
                    }
                }
                """);
        examples.put("Synthetic map: concentric rings", """
                // concentric rings around the center of the map, every 300 m
                // (synthetic map: the first available map only gives the grid)
                cx = (xmin() + xmax()) / 2;
                cy = (ymin() + ymax()) / 2;
                d = sqrt((x() - cx) ^ 2 + (y() - cy) ^ 2);
                result = sin(2 * M_PI * d / 300);
                """);
        examples.put("Synthetic map: a hill", """
                // a gaussian hill 500 m high in the center of the map, on a plain at 1000 m
                // (synthetic map: the first available map only gives the grid)
                cx = (xmin() + xmax()) / 2;
                cy = (ymin() + ymax()) / 2;
                d = sqrt((x() - cx) ^ 2 + (y() - cy) ^ 2);
                height = 500;
                width = 600;
                result = 1000 + height * exp(-d ^ 2 / (2 * width ^ 2));
                """);
        examples.put("Synthetic map: a valley", """
                // a meandering V shaped valley, with sides of 30% and a bottom sloping by 5% towards the south:
                // the bottom swings 250 m from the center line, with a wave length of 1500 m
                // (synthetic map: the first available map only gives the grid)
                cx = (xmin() + xmax()) / 2 + 250 * sin(2 * M_PI * (y() - ymin()) / 1500);
                result = 1000 + 0.3 * abs(x() - cx) + 0.05 * (y() - ymin());
                """);
        examples.put("Synthetic map: a fractal terrain", """
                // a fractal terrain: 6 octaves of value noise, each half as long and 0.45 times as high as the
                // previous one; the random heights of the corners of a lattice are interpolated smoothly
                // (synthetic map: the first available map only gives the grid)
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
                result = z;
                """);
        return examples;
    }
}
