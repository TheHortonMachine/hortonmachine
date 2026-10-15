# Cb

Calculates the histogram of a raster: its values are split in intervals, and for each interval it gives the mean value and the number of cells. This is how [Peakflow](../hydro-geomorphology/peakflow.md) builds the width function and the distribution of the topographic index.

With a second map, the histogram is coupled: for the cells of each interval of the first map it also gives the mean and the moments of the second map, as the variance. It answers questions as how the slope changes with the elevation, or the soil moisture with the topographic index.

## Details

```{include} ../generated/Cb_details.md
```

## Parameters

```{include} ../generated/Cb.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/cb.groovy
:language: groovy
```

:::{figure} ../../images/modules/Cb_elevation_histogram.png
:alt: The histogram of the sample elevation model: the number of cells in each elevation interval, most of them between 1600 and 1850 m.

The histogram of the sample elevation model: the number of cells in each elevation interval, most of them between 1600 and 1850 m.
:::

## The output and the moments

Each row of the output matrix is one interval of the first map, and its columns are:

| column | content |
|--------|---------|
| 0 | the mean value of the first map in the interval |
| 1 | the number of cells in the interval |
| 2 | the mean value of the second map in the interval: its first moment |
| 3 | the second moment of the second map, the variance |
| 4 | the third moment, and so on up to `pLast` |

The first moment is always in column 2, so with `pFirst` set to 1 it is not repeated. The moments from the second on are centered on the mean of the interval and divided by the number of cells:

- the **mean** is the typical value of the second map at that value of the first: its sequence over the rows is the relation between the two maps, as the curve of the slope against the elevation;
- the **variance** (second moment) tells how much the second map spreads around that mean; its square root, the standard deviation, has the units of the map. A small value says that the mean represents the interval well, a large one that cells with very different values are mixed in it;
- the **third moment** tells whether the spread is symmetric. Divided by the cube of the standard deviation it gives the skewness: near 0 the values are balanced around the mean, positive when a few cells are much higher than the others (a long tail of high values), negative when they are much lower.

The range of the first map is split in `pBins - 1` intervals of equal width, and the empty ones are left out: the 20 bins of the examples give 19 rows. The intervals with few cells, usually at the two ends of the range, give less reliable moments.

## Example with the elevation as both maps

The same elevation model can be given as both maps, to look at how the elevation is distributed inside each interval. The moments from 1 to 3 are asked:

```{literalinclude} ../examples/hm/cb_elevation.groovy
:language: groovy
```

:::{figure} ../../images/modules/Cb_elevation_moments.png
:alt: Above, the number of cells of each elevation interval; below, the skewness of the elevation inside each interval, negative where the number of cells grows going up and positive above 1800 m, where it decreases.

Above, the number of cells of each elevation interval; below, the skewness of the elevation inside each interval, red when positive and blue when negative.
:::

Here the moments do not relate two variables, they describe the elevation inside each interval:

- the **mean** is the same as the first column, since both maps are the same;
- the **standard deviation** is about 19.7 m in all the intervals. They are about 68 m wide, and values spread evenly over an interval of width w have a standard deviation of w / √12 ≈ 19.7 m: the cells fill each interval evenly, without steps or terraces at given elevations. A clearly smaller value would mean cells crowded at one elevation, as a flat valley floor or a lake;
- the **skewness** says where the cells sit inside the interval, so it is a detail of the histogram finer than the intervals. Negative, there are more cells in the upper part of the interval and the area grows going up, as at 960 m, where the cells jump from about 1000 to 3000 from one interval to the next. Positive, there are more cells in the lower part and the area shrinks going up, as in all the intervals above 1800 m, where the cells become fewer and fewer. Near 0 the area is about the same across the interval.

## Example with two maps

The second map is described inside each interval of the first one: here the slope of [Slope](../geomorphology/slope.md), as the tangent of the angle, in the same 20 intervals of the elevation:

```{literalinclude} ../examples/hm/cb_slope.groovy
:language: groovy
```

:::{figure} ../../images/modules/Cb_elevation_slope.png
:alt: Above, the mean slope of each elevation interval, between 0.5 and 0.7, in the band of one standard deviation, wider between 1750 and 1950 m; below, the skewness of the slope, between 3 and 4 in the same intervals.

Above, the mean slope of each elevation interval in the band of one standard deviation around it; below, the skewness of the slope.
:::

The mean alone says that the slope hardly depends on the elevation: it stays around 0.5 to 0.6, about 27 to 31 degrees, from the valley to the top. The other moments show what the mean hides: between 1750 and 1950 m the band of the standard deviation widens and the skewness jumps to 3 or 4, so most of those cells have ordinary slopes but a few are much steeper, as rock walls. At the top the skewness goes back to 0 and the slopes are again homogeneous.
