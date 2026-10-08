# Cb

Calculates the histogram of a raster: its values are split in intervals, and for each interval it gives the mean value and the number of cells. This is how [Peakflow](../hydro-geomorphology/peakflow.md) builds the width function and the distribution of the topographic index.

With a second map, the histogram is coupled: for the cells of each interval of the first map it also gives the mean and the moments of the second map, as the variance. It answers questions as how the slope changes with the elevation, or the soil moisture with the topographic index.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/cb.groovy
:language: groovy
```

:::{figure} ../../images/modules/Cb_elevation_histogram.png
:alt: The histogram of the sample elevation model: the number of cells in each elevation interval, most of them between 1600 and 1850 m.

The histogram of the sample elevation model: the number of cells in each elevation interval, most of them between 1600 and 1850 m.
:::

With a second map, the same rows also hold the mean of the second map in each elevation interval and its moments, for example how the slope changes with the elevation.

## Reference

```{include} ../generated/Cb.md
```
