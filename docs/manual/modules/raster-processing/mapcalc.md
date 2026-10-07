# Mapcalc

Mapcalc computes a new raster from other rasters with a script in the Jiffle language: the value of each output cell is computed from the values of the input maps in the same cell, or in the cells around it, with arithmetic, conditions, loops and functions. It is the module behind the [Map Calculator](../../apps/mapcalc.md) application, and the language described below is the same in both.

Jiffle, its language and its engine, was created by [Michael Bedward](https://github.com/mbedward), and is developed today as part of [Eclipse ImageN](https://github.com/eclipse-imagen/imagen).

Unlike the application, the module needs the full Jiffle script, with the images block that declares the maps: the inputs with `read`, the output with `write`. The maps are known by their file name without extension. The module is hidden in the Spatial Toolbox, where the Map Calculator replaces it, and is meant for scripts.

## The Jiffle language

A Jiffle script is run once for each cell of the output map: every time, the names of the input maps give the values of that cell, and the value assigned to the output map becomes the value of the cell. The examples below are run on maps called `dem` and `slope`, with the output `result`.

### Structure of a script

A script has up to three blocks, followed by the statements:

```text
options { outside = 0; }                    // optional: options of the script
images { dem = read; result = write; }      // the input and output maps
init { count = 0; }                         // optional: variables kept from cell to cell

result = dem * 2;                           // the statements, run for each cell
```

- the **images** block declares the maps: each input with `read`, the output with `write`. In the Map Calculator the block is written by the application, and the script contains just the statements, with the output called `result`;
- the **init** block declares variables that are initialized once, and keep their value from one cell to the next, as counters. The cells are processed row by row, from the south-west corner of the map;
- the **options** block sets the value of the cells outside of the map, see [neighbouring cells](#neighbouring-cells).

Statements end with `;`. Comments are written as in Java: `// to the end of the line` and `/* between the marks */`.

### Values and no-data

All the values are numbers. `true` and `false` are 1 and 0, and the comparisons give 1 or 0 too.

The no-data cells of the input maps are `null`, which can be tested with `isnull()`. A `null` in a calculation gives `null`, and a `null` assigned to the output gives a no-data cell:

```text
result = isnull(dem) ? 0 : dem;      // no-data becomes 0
result = slope > 45 ? null : dem;    // the steep cells become no-data
result = dem + 10;                   // no-data stays no-data
```

The constants `M_PI`, `M_PI_2` (π/2), `M_PI_4` (π/4), `M_E` and `M_SQRT2` are available.

### Variables

Variables need no declaration, and exist from their first assignment:

```text
h = dem - 1000;
h *= 2;
result = h;
```

Variables are created anew for each cell; those of the init block keep their value from cell to cell.

### Operators

| Operators | Meaning | Example |
|---|---|---|
| `+` `-` `*` `/` | arithmetic | `dem / 1000` |
| `%` | remainder of the division | `dem % 100` |
| `^` | power | `slope ^ 2` |
| `==` `!=` `<` `<=` `>` `>=` | comparisons: 1 if true, 0 if false | `dem > 1500` |
| `&&` `\|\|` `^\|` `!` | logical and, or, exclusive or, not | `dem > 1500 && slope < 30` |
| `? :` | value by condition | `dem > 1500 ? 1 : 0` |
| `=` `+=` `-=` `*=` `/=` `%=` | assignment, also combined with an operation | `h += 10` |
| `++` `--` | add or subtract 1 | `n++` |

### Conditions

The `if` statement runs a block when a condition is true, with optional `else if` and `else` blocks:

```text
if (dem > 2000) {
    result = 3;
} else if (dem > 1500) {
    result = 2;
} else {
    result = 1;
}
```

The `con` function chooses a value by a condition, in one expression:

| Call | Value |
|---|---|
| `con(x)` | 1 if x is true, else 0 |
| `con(x, a)` | a if x is true, else 0 |
| `con(x, a, b)` | a if x is true, else b |
| `con(x, a, b, c)` | a if x is positive, b if it is 0, c if it is negative |

```text
result = con(dem > 1500, 1, 0);
result = con(dem - 1500, 1, 0, -1);   // 1 above 1500 m, 0 at 1500 m, -1 below
```

### Loops

Four forms of loop are available: on the values of a list, on a range of integers, while a condition is true and until it becomes true.

```text
s = 0;
foreach (v in [1, 2, 3]) { s += v; }          // on a list: s is 6

s = 0;
foreach (i in -2:2) { s += i; }               // on the integers from -2 to 2

n = 0;
while (n < dem) { n += 100; }                 // n is dem rounded up to 100

n = 0;
until (n >= dem) { n += 100; }                // the same, with the opposite condition
```

`breakif (condition);` leaves the loop when the condition is true, and `break;` leaves it immediately:

```text
n = 0;
foreach (i in 1:10) {
    n += i;
    breakif (n > 10);                         // n is 15
}
```

### Lists

Lists are written between square brackets, and new values are appended with `<<`. The statistical functions work on lists, and ignore their `null` values:

```text
values = [dem, dem * 2, 100];
values << slope;
result = mean(values);
```

| Function | Value |
|---|---|
| `max(list)` `min(list)` | the largest and the smallest value |
| `sum(list)` | the sum of the values |
| `mean(list)` `median(list)` `mode(list)` | the mean, the median and the most frequent value |
| `range(list)` | the largest minus the smallest value |
| `sdev(list)` `variance(list)` | the standard deviation and the variance |

### Neighbouring cells

`map[dx, dy]` reads the value of a map at an offset from the current cell. The offsets are in **map units**, positive towards east (`dx`) and north (`dy`), so one cell is `xres()` by `yres()`:

```text
east  = dem[xres(), 0];
north = dem[0, yres()];
west  = dem[-xres(), 0];
south = dem[0, -yres()];
```

Reading outside of the map stops the script with an error, unless the `outside` option sets the value of the cells outside, as `null` or a number not negative:

```text
options { outside = null; }
images { dem = read; result = write; }

// the mean elevation of the 3x3 cells around each cell, without the no-data
s = 0;
n = 0;
foreach (dy in -1:1) {
    foreach (dx in -1:1) {
        v = dem[dx * xres(), dy * yres()];
        if (!isnull(v)) {
            s += v;
            n++;
        }
    }
}
result = s / n;
```

`map[$x, $y]` reads the cell that contains the position x, y, in the coordinates of the map, instead of an offset: `dem[$1638705, $5112805]` gives the same value, the elevation of that point, to all the cells.

### Position and extent

These functions describe the current cell and the area processed, in map units:

| Function | Value |
|---|---|
| `x()` `y()` | the coordinates of the south-west corner of the current cell |
| `xres()` `yres()` | the size of the cells |
| `xmin()` `xmax()` `ymin()` `ymax()` | the bounds of the map |
| `width()` `height()` | the width and height of the map |

```text
// only the cells in the northern half of the map
result = y() > (ymin() + ymax()) / 2 ? dem : null;
```

### Functions

| Function | Value |
|---|---|
| `abs(x)` | absolute value |
| `sqrt(x)` | square root |
| `exp(x)` | e to the power x |
| `log(x)` `log(x, b)` | natural logarithm, and logarithm in base b |
| `floor(x)` `ceil(x)` | rounded down and up to an integer |
| `round(x)` `rint(x)` | rounded to the nearest integer |
| `round(x, n)` | rounded to the nearest multiple of the integer n: `round(1234, 100)` is 1200 |
| `sign(x)` | -1, 0 or 1, as the sign of x |
| `sin(x)` `cos(x)` `tan(x)` | trigonometric functions, of x in radians |
| `asin(x)` `acos(x)` `atan(x)` `atan2(y, x)` | inverse trigonometric functions, in radians |
| `degToRad(x)` `radToDeg(x)` | conversions between degrees and radians |
| `min(a, b)` `max(a, b)` | the smaller and the larger of two values |
| `isnull(x)` `isnan(x)` `isinf(x)` | 1 if x is null, not a number, infinite |
| `rand(x)` `randInt(x)` | a random number between 0 and x, decimal and integer |

To round to decimals, multiply and divide: `round(x * 100) / 100` keeps two decimals.

```text
// the slope in degrees, from the slope as tangent
result = radToDeg(atan(slope));
```

### Limitations

- The variables can't be declared with a type, as `int k = 3;`: they are all numbers.
- The value of the `outside` option can't be a negative number.
- The output map is the first map declared with `write`.

## Synthetic maps

Jiffle can also create maps from nothing but formulas: the position functions `x()` and `y()` give the coordinates of each cell, and the script computes a value from them. The input map only gives the grid of the output, its extent and cell size, and doesn't need to be declared in the images block.

In the [Map Calculator](../../apps/mapcalc.md), write the scripts without the images block, assign the output to `result` instead of the name used here, and load at least one map among the available maps: when the script uses none of them, the first one gives the grid. Synthetic maps are useful to test the modules on surfaces whose result is known, to create scenarios and to teach.

### Concentric rings

The classic example of Jiffle: the sine of the distance from the center of the map.

```{literalinclude} ../examples/hm/mapcalc_rings.groovy
:language: groovy
```

:::{figure} ../../images/modules/Mapcalc_rings.png
:alt: Concentric rings every 300 m around the center of the sample grid.

Concentric rings every 300 m around the center of the sample grid.
:::

### A hill

A Gaussian hill on a plain: the elevation falls with the distance from the top as a bell curve. On such a surface the slope, the aspect and the drainage directions are known exactly, which makes it a good test of the geomorphological modules.

```{literalinclude} ../examples/hm/mapcalc_hill.groovy
:language: groovy
```

:::{figure} ../../images/modules/Mapcalc_hill.png
:alt: A Gaussian hill 500 m high on a plain at 1000 m.

A Gaussian hill 500 m high on a plain at 1000 m.
:::

### A valley

A V shaped valley, with a bottom that meanders along a sine wave and slopes towards the south: the network, along the bottom, and the basin are known before running the hydrological modules.

```{literalinclude} ../examples/hm/mapcalc_valley.groovy
:language: groovy
```

:::{figure} ../../images/modules/Mapcalc_valley.png
:alt: A meandering valley with sides of 30% and a bottom sloping by 5% towards the south.

A meandering valley with sides of 30% and a bottom sloping by 5% towards the south.
:::

### A fractal terrain

Natural terrain looks the same at many scales: big hills carry smaller hills, which carry rough details. Fractal noise reproduces this by summing octaves of noise, each with half the size and a fraction of the height of the previous one. The noise of each octave is value noise: pseudo random heights on the corners of a lattice, interpolated smoothly. Jiffle has no random generator with a seed, so the random heights come from a hash of the lattice coordinates: the same seed always gives the same terrain, and another seed another one. The factor applied to the height of each octave, 0.45 here, sets the roughness: higher values give rougher terrain.

```{literalinclude} ../examples/hm/mapcalc_terrain.groovy
:language: groovy
```

:::{figure} ../../images/modules/Mapcalc_terrain.png
:alt: A fractal terrain of 6 octaves, from hills 1500 m wide to details of 50 m.

A fractal terrain of 6 octaves, from hills 1500 m wide to details of 50 m.
:::

The terrain can go through the usual hydrological chain, as a real elevation model:

```{literalinclude} ../examples/hm/mapcalc_terrain_network.groovy
:language: groovy
```

:::{figure} ../../images/modules/Mapcalc_terrain_net.png
:alt: The network of the fractal terrain, with a threshold of 200 cells. A random terrain has many closed depressions, unlike the real ones shaped by erosion: Pitfiller turns them into flat areas, which the drainage directions cross with straight parallel paths.

The network of the fractal terrain, with a threshold of 200 cells. A random terrain has many closed depressions, unlike the real ones shaped by erosion: Pitfiller turns them into flat areas, which the drainage directions cross with straight parallel paths.
:::

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/mapcalc.groovy
:language: groovy
```

:::{figure} ../../images/modules/Mapcalc_filled.png
:alt: The depth of the depressions of the sample area filled by Pitfiller, in meters: the depitted elevation minus the elevation, where it is above 0.

The depth of the depressions of the sample area filled by Pitfiller, in meters: the depitted elevation minus the elevation, where it is above 0.
:::

## Reference

```{include} ../generated/Mapcalc.md
```
