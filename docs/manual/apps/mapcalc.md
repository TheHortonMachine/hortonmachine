# Map Calculator

The Map Calculator computes new rasters from existing ones with scripts written in Jiffle, a small language for raster algebra that is now part of the Eclipse ImageN project. A script is run for every cell of the result: it can combine the values of several maps, read the neighbouring cells, and use variables, conditions and loops.

## Launching

Start the application with the `hm-mapcalc` launcher (see [Launching an application](../installation.md#launching-an-application)).

## The main window

:::{figure} ../images/apps/mapcalc/main.png
:alt: The Map Calculator
:width: 100%
:align: center

The Map Calculator with a landform classification script.
:::

Available maps
: the rasters the script can use. Add them with the **...** button. Each map is known in the script by its file name without extension, like `dtm_flanginec` for `dtm_flanginec.asc`. Double-click on a map to insert its name in the script.

Function Area
: the script, with syntax highlighting.

Syntax Help
: buttons that insert the constructs of the language, grouped by topic: control flow (`if`, `con`, `foreach`, `while`), general syntax, logical and arithmetic operators, numeric and statistical functions, and the functions describing the processing area (like `xres()` or `xmin()`). Hover over a button to see what it does.

Mapcalc History
: the scripts run so far, to recall one of them.

Output Path
: the raster file to create.

Heap [MB] and Debug
: the memory and the log level of the process running the script, as in the [Spatial Toolbox](spatialtoolbox.md#execution-options).

Press **run** to execute the script. Like the modules of the Spatial Toolbox, it runs in a separate process, with its progress shown in a log window.

:::{figure} ../images/apps/mapcalc/log.png
:alt: The log of a run
:width: 70%
:align: center

The log of the landform classification.
:::

## Writing scripts

The script assigns the value of each cell of the output map to the variable `result`. The Map Calculator declares the maps for Jiffle by itself, so the script contains just the calculation. The simplest scripts are one-liners, like the depth of the depressions filled by the Pitfiller module:

```text
result = pit_flanginec - dtm_flanginec;
```

The main elements of the language:

- **variables** need no declaration: `r = 5;`;
- **conditions**, with `if (...) { ... } else { ... }` blocks, or inline with `con(condition, valueIfTrue, valueIfFalse)`;
- **loops**, with `foreach (i in -2:2) { ... }` and `while (...) { ... }`;
- **no-data**: cells without a value are `null`; test them with `isnull(value)`, and write `result = null;` to leave an output cell empty;
- **functions** like `sqrt`, `abs`, `atan`, `radToDeg`, `min`, `max`, and the position and resolution of the current cell: `x()`, `y()`, `xres()`, `yres()`, and the bounds of the map, `xmin()`, `xmax()`, `ymin()`, `ymax()`.

### Neighbouring cells

`map[dx, dy]` reads the value of a map at an offset from the current cell. In the Map Calculator the offsets are expressed in **map units**, not in cells, since the maps are georeferenced: the cell on the right is `map[xres(), 0]`, the one above `map[0, yres()]`. Reading outside of the map stops the script with an error, so scripts reading the neighbours have to skip the cells along the borders, as in the example below.

## Example: landform classification

This script classifies the landforms of a DTM, combining two terrain indexes:

- the **Topographic Position Index** (TPI): the elevation of a cell minus the mean elevation around it, positive on ridges and negative in valleys;
- the **slope**, calculated with the Horn method on the 3×3 cells around each cell, to separate flat areas from steady slopes where the TPI is close to zero.

The result has six classes: 1 valley, 2 lower slope, 3 flat, 4 mid slope, 5 upper slope and 6 ridge. The parameters at the top set the size of the window used for the TPI and the thresholds of the classes.

```text
// Landform classification of a DTM, combining the
// Topographic Position Index (TPI) with the slope.
// Result classes:
//   1 valley, 2 lower slope, 3 flat, 4 mid slope, 5 upper slope, 6 ridge

r = 5;           // radius of the TPI window, in cells (11x11 cells)
tpiThres = 2.0;  // TPI threshold, in meters
flatSlope = 8;   // maximum slope of flat areas, in degrees

// skip the border cells, where the window would fall outside of the map
inside = x() > xmin() + (r + 1) * xres() && x() < xmax() - (r + 1) * xres()
      && y() > ymin() + (r + 1) * yres() && y() < ymax() - (r + 1) * yres();

if (!inside || isnull(dtm_flanginec)) {
    result = null;
} else {
    // TPI: elevation of the cell minus the mean elevation of the window
    sum = 0;
    n = 0;
    foreach (dy in -r:r) {
        foreach (dx in -r:r) {
            v = dtm_flanginec[dx * xres(), dy * yres()];
            if (!isnull(v)) {
                sum += v;
                n++;
            }
        }
    }
    tpi = dtm_flanginec - sum / n;

    // slope with the Horn method on the 3x3 neighbourhood
    // (neighbour offsets are in map units: one cell is xres() by yres())
    ex = xres();
    ey = yres();
    gx = ((dtm_flanginec[ex, -ey] + 2 * dtm_flanginec[ex, 0] + dtm_flanginec[ex, ey])
        - (dtm_flanginec[-ex, -ey] + 2 * dtm_flanginec[-ex, 0] + dtm_flanginec[-ex, ey])) / (8 * xres());
    gy = ((dtm_flanginec[-ex, ey] + 2 * dtm_flanginec[0, ey] + dtm_flanginec[ex, ey])
        - (dtm_flanginec[-ex, -ey] + 2 * dtm_flanginec[0, -ey] + dtm_flanginec[ex, -ey])) / (8 * yres());
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
```

To use it on another DTM, replace `dtm_flanginec` with the name of that map. A larger window (`r`) captures larger landforms, like main valleys and ridges, a smaller one the details of the terrain.

:::{figure} ../images/apps/mapcalc/landforms.png
:alt: The landform classes of the Flanginec DTM
:width: 70%
:align: center

The landforms of the Flanginec DTM: valleys and lower slopes in blue, flat areas in white, mid and upper slopes in orange, ridges in red. The border cells, skipped by the script, have no value.
:::
